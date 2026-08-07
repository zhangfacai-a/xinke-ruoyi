# 影刀自动查单与触达接口

## 1. 使用前配置

本地接口地址：`http://localhost:8080/open-api/rpa/outreach`

所有影刀请求都要带请求头：

```text
Content-Type: application/json
X-RPA-Key: 本机配置的接口密钥
```

`X-RPA-Key` 是影刀和若依后端之间的共享接口密码，只用于识别合法的影刀请求，不是抖音密码。不要把 `X-RPA-Key`、`leaseToken` 或抖音登录信息打印到影刀日志。每台运行影刀的电脑设置一个固定且唯一的 `workerId`，例如 `yingdao-pc-01`，重启后不要改变。

开始前要在若依后台完成两项配置：

1. 创建店铺配置，填写店铺编码、抖音号标识、抖店名称、私信模板和每日上限。私信模板不能为空。
2. 将采集到的直播间绑定到店铺。一个直播间只能属于一个店铺。

当前数据库没有店铺配置，因此在配置完成前，领取接口会正常返回 `available: false`。

## 2. 追踪范围

“直播观众追单池”页面提供两层控制：

- 自动规则：可暂停自动追踪，并设置最近 1 至 365 个自然日。设置为 1 时，只处理今天出现的新用户。
- 用户例外：单个或批量设置为“强制追踪”“永不追踪”或“恢复自动规则”。

用户例外优先于最近天数，但不会覆盖安全状态：已下单、追单前已下单、无效或已经跟进中的用户不会再次进入首次触达队列。关闭全局自动追踪时，强制追踪也暂停。

## 3. 推荐执行流程

1. 调用领取接口，一次领取最多 10 人。接口保证同一批全部属于同一个店铺。
2. 保存响应中的 `batchNo`、`leaseToken` 和 `workerId`，不要自行修改。
3. 根据 `batch.douyinAccountCode` 切换到对应抖音账号和抖店登录环境。
4. 逐个打开任务的 `profileUrl`，获取用户抖音号。
5. 用抖音号在 `batch.douyinShopName` 对应的抖店查询订单。
6. 已下单则回传 `ORDERED` 和订单号；未下单则关注、私信，再回传 `CONTACTED`。
7. 每处理完一个用户立即回传一次结果，不要等 10 人全部处理完。
8. 批次执行期间按接口返回的 `heartbeatAfterSeconds` 发送心跳。
9. 批次正常处理完后直接领取下一批；只有流程被人工停止或浏览器无法恢复时才调用释放接口。

每批租约默认 30 分钟。租约到期后，未完成任务会自动回到队列，最多尝试 5 次。

## 4. 健康检查

```http
GET /open-api/rpa/outreach/health
```

成功响应：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "ready": true,
    "leaseMinutes": 30,
    "maxBatchSize": 10,
    "configuredShopCount": 1,
    "unmappedRoomCount": 0
  }
}
```

影刀启动时先调用一次。`code` 不是 `200` 时不要继续领取任务。

## 5. 领取一批任务

```http
POST /open-api/rpa/outreach/task/claim
```

```json
{
  "workerId": "yingdao-pc-01",
  "limit": 10
}
```

`preferredShopCode` 是可选字段。只有需要让某台电脑固定处理某个店铺时才传：

```json
{
  "workerId": "yingdao-pc-01",
  "limit": 10,
  "preferredShopCode": "SHOP-001"
}
```

有任务时：

```json
{
  "code": 200,
  "data": {
    "available": true,
    "leaseSeconds": 1800,
    "heartbeatAfterSeconds": 900,
    "batch": {
      "batchNo": "RPA-ABC123",
      "leaseToken": "租约令牌",
      "workerId": "yingdao-pc-01",
      "shopCode": "SHOP-001",
      "shopName": "店铺一",
      "douyinAccountCode": "DY-ACCOUNT-01",
      "douyinShopName": "抖店一",
      "messageTemplate": "私信模板"
    },
    "tasks": [
      {
        "taskNo": "任务编号",
        "profileUrl": "https://www.douyin.com/user/用户secUid",
        "nickname": "用户昵称",
        "hasComment": 1,
        "commentCount": 2,
        "lastCommentContent": "最近评论",
        "estimatedStaySeconds": 180,
        "liveRoomName": "直播间名称"
      }
    ]
  }
}
```

无任务时：

```json
{
  "code": 200,
  "data": {
    "available": false,
    "retryAfterSeconds": 60,
    "reason": "当前没有可领取任务，请检查店铺和直播间绑定",
    "tasks": []
  }
}
```

影刀按 `retryAfterSeconds` 等待后再领取，不要高频循环请求。

## 6. 批次心跳

```http
POST /open-api/rpa/outreach/batch/heartbeat
```

```json
{
  "batchNo": "RPA-ABC123",
  "leaseToken": "领取响应中的租约令牌",
  "workerId": "yingdao-pc-01"
}
```

建议在独立子流程中每 15 分钟执行一次。心跳失败后停止当前批次，不要继续操作用户，重新领取任务。

## 7. 逐个回传结果

```http
POST /open-api/rpa/outreach/task/result
```

`requestId` 要为每次业务结果生成一个 UUID。同一个结果如果因超时需要重发，必须继续使用原来的 `requestId`，不能生成新值。

已查询到订单：

```json
{
  "batchNo": "RPA-ABC123",
  "leaseToken": "领取响应中的租约令牌",
  "workerId": "yingdao-pc-01",
  "requestId": "11111111-2222-3333-4444-555555555555",
  "taskNo": "任务编号",
  "outcome": "ORDERED",
  "douyinNo": "用户抖音号",
  "orderNo": "抖店订单号"
}
```

未下单，已经执行关注或私信：

```json
{
  "batchNo": "RPA-ABC123",
  "leaseToken": "领取响应中的租约令牌",
  "workerId": "yingdao-pc-01",
  "requestId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "taskNo": "任务编号",
  "outcome": "CONTACTED",
  "douyinNo": "用户抖音号",
  "followed": true,
  "messaged": true,
  "messageContent": "实际发送的私信内容"
}
```

结果类型：

| outcome | 使用场景 | 必填信息 | 后续处理 |
| --- | --- | --- | --- |
| `ORDERED` | 查询到订单 | `douyinNo`、`orderNo` | 完成，不再触达 |
| `CONTACTED` | 未下单且已关注或私信 | `douyinNo`，`followed`/`messaged` 至少一个为 `true` | 完成并记录跟进 |
| `SKIPPED` | 私密账号、无抖音号、明确不应处理 | `resultCode` | 完成，不自动重试 |
| `FAILED` | 确定无法恢复的错误 | `resultCode` 或 `errorMessage` | 完成，不自动重试 |
| `RETRYABLE_ERROR` | 页面超时、网络错误、临时风控 | `resultCode` 或 `errorMessage` | 重新排队，最多 5 次 |

建议使用固定的 `resultCode`，例如 `PROFILE_PRIVATE`、`DOUYIN_NO_NOT_FOUND`、`PAGE_TIMEOUT`、`SHOP_LOGIN_EXPIRED`、`RISK_CONTROL`。

成功响应中的 `idempotent: true` 表示这个 `requestId` 已处理过，影刀应当视为成功，不要再次执行关注、私信或查单。

## 8. 主动释放未完成批次

```http
POST /open-api/rpa/outreach/batch/release
```

```json
{
  "batchNo": "RPA-ABC123",
  "leaseToken": "领取响应中的租约令牌",
  "workerId": "yingdao-pc-01"
}
```

仅在影刀被人工停止、账号掉线或浏览器无法恢复时调用。已逐条完成的任务不会被释放，剩余任务会回到队列。

## 9. 错误处理

- HTTP 请求成功后仍要判断响应体的 `code`。
- `code = 200` 才表示接口接受请求。
- `code = 46011` 表示 `X-RPA-Key` 错误或缺失。
- 租约不匹配或已过期时，停止本批次并重新领取。
- 网络超时但无法确认结果是否写入时，原请求内容和原 `requestId` 原样重发。
- 抖店登录失效时先释放批次，再通知人工登录，不能继续空跑。
