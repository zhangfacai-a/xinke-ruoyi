# 网站整理与发布操作

本轮整理前源码基线为 Git 提交 `1c5e120`。不要用旧局部 class 替换脚本发布本轮版本。

## 构建与私有配置

生产后端：`mvn -pl xinke-admin -am clean verify`。前端：`pnpm --dir xinke-ui build:prod`。

代码生成器仅开发环境需要时用 Maven `-Pdev-tools` 引入。主站发布包含客户、挽单、运费、礼品、主播和 App 版本管理；桌面 App 的本地界面仍独立构建。

真实配置放在被忽略的 `config/local/application-local.yml`；服务器使用 `/home/deploy/xinke-config/application-private.yml`，权限 0600，或通过 `XINKE_PRIVATE_CONFIG` 指定其他路径。系统环境变量继续生效。TOKEN_SECRET 必须提供，数据库/Redis/钉钉/COS 凭据不进入版本库。

本次在原值不变的情况下将线上内嵌配置外置。`prepare_website_runtime.py --current-jar <线上备份.jar>` 只在目标私有配置不存在时写入，不打印秘密，不覆盖已有配置。此步骤不是凭据轮换；如果历史提交曾公开包含真实凭据，需在独立维护窗口轮换并核对服务，不能因当前提交移除了秘密就认为历史已清除。

部署脚本依赖：`pip install -r scripts/server/requirements.txt`。测试工具依赖：`pip install -r scripts/server/test-requirements.txt`。

## 备份和隔离测试

1. 使用 `backup_database.py <本机备份.sql.gz>` 做全库一致性备份；脚本不在进程参数或日志中暴露密码，同时生成 SHA256 和 gzip 完整性信息。
2. 备份线上 JAR、网站资源、私有运行配置与用户上传目录；云端 COS 附件属于独立资产，数据库备份不包含云端图片文件。
3. `prepare_reorganization_test.py --database xinke_reorg_test_<数字> --backup <全库备份.sql.gz>` 创建全新隔离测试库，恢复备份中的菜单并复制必要测试基线。它拒绝覆盖同名测试库，不复制大体量客户事件数据，不执行生产提醒或同步。
4. 确认 Redis 数据库 15 专供测试且为空。测试应用使用 `config/local/reorganization-test.yml`，端口 18081，`live.scheduling.enabled=false`。
5. `java -jar xinke-admin/target/xinke-admin.jar --spring.config.additional-location=file:./config/local/reorganization-test.yml`。
6. `python scripts/server/test_website_reorganization.py` 验证菜单、共享组、岗位权限撤销、在线缓存、账号停用、普通账号边界和现有业务端点。

测试库和 Redis 的清理只针对有明确标记的测试数据；禁止将生产 Redis FLUSHDB/FLUSHALL 当作“刷新权限”。

## 迁移与发布

`reorganize_website.py` 默认只读预览，`--apply` 才执行迁移。它通过 `sys_release_migration` 记录版本：

- 新增账号本地停用标记，保护已有停用账号；不删除用户。
- 保留菜单 ID 和权限标识，重组菜单层级和主站入口；旧链接跳转保留。
- 相同店铺人员名单关联共享组，逐项断言 `live_subject_user_map` 完全一致；特殊店铺保留单独配置。
- 增加主播和正常登录用户的自动授权规则；已有人工角色保留。京东页面延续所有正常登录用户可用的要求。
- 数据库 DDL 在事务前执行；菜单与共享组变更在一个事务内提交。重复运行返回 alreadyApplied。

先在生产备份完成后执行迁移，再部署完整程序。现有受限服务器命令 `xinke-release` 会备份旧程序和网站、检查服务、部署并检查存活；不需要扩大 deploy 账号权限。当前服务器未配置蓝绿部署，仓库里的蓝绿脚本只作后续可选工具，本轮不宣称零停机。

用 `build_website_release.py --website-snapshot <上线前网站资源.tar.gz>` 生成完整源码构建产物；脚本只从旧资源保留签名 App 更新目录，不替换新版业务页面。`--skip-build` 仅用于紧接着已经验证过的完整构建。

发布使用 `deploy_with_key.py --release <生成目录> --stamp YYYYMMDD-HHMMSS`。服务器上已有部署命令所需的主播字段迁移是幂等的；本次菜单迁移由上述脚本明确执行，不混入旧初始化 SQL。

## 回退边界

程序和网站回退使用部署前备份；不要把整理前全库备份覆盖上线后新增订单。新增 `local_disabled` 列可保留，旧程序不引用它。菜单需要回退时，仅恢复本次迁移前导出的 `sys_menu/sys_role_menu` 配置并核对新增父目录；共享组只解除关联即可，原人员分配从未重写。移除自动角色应仅处理本轮受管角色，人工角色与业务责任保留。

线上验证失败时优先恢复程序与资源；权限/菜单的针对性回退必须先比较发布后是否有人继续编辑，避免覆盖他人的配置变更。历史备份恢复演练在隔离库执行，不能作为日常生产回退命令。

## 人员使用说明

“人员与业务配置 → 人员管理”查看业务岗位、分配数量及权限来源。停用前弹窗显示未完成挽单、待办跟进和业务分配数量，管理员安排交接后再停用；停用会终止在线会话，历史记录保留。钉钉同步不会自动恢复本地停用或删除的账号。

“店铺与人员组”维护共享人员。保存时可查看每家店新增和移除的岗位；移出组保留当前名单，转为单独配置。移除最后一项业务分配会撤销对应自动角色，历史人工授权则会明确显示为另一来源。

本轮不会替管理员决定实际人员离职、特殊角色去留或未完成订单改派；现存异常配置通过人员详情核查，历史角色先保留。
