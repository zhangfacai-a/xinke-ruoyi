# 数据库脚本入口

不要依文件名把本目录全部执行一遍。新建业务、历史版本升级和本轮网站整理属于不同迁移。

本轮网站整理使用 `scripts/server/reorganize_website.py`，默认只读，`--apply` 才写入；执行前备份、在隔离库演练、核对当前版本。该脚本记录 `sys_release_migration`，不清空业务数据。

现用增量脚本包括客户/RPA、运费、挽单、主播事实与修正、桌面协议等业务迁移；根据已有表结构和发布说明选择所需版本，不逆序重放。店铺人员组结构由 `add-live-staff-groups-and-auto-access.sql` 建立；本轮网站迁移以该结构已存在为前置条件。

`history/` 是历史初始化、重置与清理 SQL，只用于查阅，可能删除记录或重置配置，禁止放入自动发布流程。不要在本轮执行 `reset-live-metrics-v3.sql`、`simplify-system-menu.sql` 等历史脚本。

运维流程和回退限制见 `docs/operations/website-reorganization.md`。
