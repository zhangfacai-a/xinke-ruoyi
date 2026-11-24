# 挽单与转推 BI 订单明细

`bi_live_retention_order_fact` 每行对应 `live_retention_order.id` 的一条原订单。店铺、负责人、转推店铺名称是最近一次订单同步时的名称；ID 是稳定关联键。订单号按字符串读取，避免长数字被 BI 工具舍入。

首次上线先执行 `add-bi-live-retention-order-fact.sql`，完成建表与全量回填，再部署后端。后端默认每天北京时间 00:00 和 12:00 同步新增、修改和删除的订单，并在每次同步后核对有效订单数及三项金额。可通过 `live.retention.bi-sync-cron` 配置时间。全局 `live.scheduling.enabled=false` 时定时任务不运行。BI 数据最多可能比业务表晚约 12 小时。

BI 查询应始终加 `is_deleted=0`。`business_date` 是原订单创建日期，与挽单中心看板的日期筛选一致；完成时间和首次跟进时间是独立时间维度。`completion_seconds` 仅在已完成且完成时间不早于创建时间时有值，未完成或无有效时间时为 `NULL`。金额单位为元，挽单销售额、转推金额、运费金额是三个独立指标；同一订单可同时挽单和转推，不能把三项相加当作订单 GMV。

示例：

```sql
SELECT retention_id,business_date,shop_id,shop_name,order_no,
       owner_id,owner_name,progress,retention_result,retention_amount,
       referral_success,referral_shop_name,referral_order_no,referral_amount,
       freight_collected,freight_amount,first_followed_at,completed_at
FROM bi_live_retention_order_fact
WHERE is_deleted=0 AND business_date BETWEEN '2026-11-01' AND '2026-11-30'
ORDER BY business_date,retention_id;
```

本表不包含电话地址、跟进文本、审计历史和凭证路径。这些字段可能包含客户个人信息，而且把一对多的历史或凭证展开会重复计算订单和金额。只给 BI 账号授予这张表的 `SELECT` 权限；跨系统同步时使用该表的 `retention_id` 去重，以 `synced_at` 监测延迟，同时处理 `is_deleted=1` 的删除记录。名称维度只在订单发生变动时更新，人员或店铺单独改名不会立即刷新历史行。
