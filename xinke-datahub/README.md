# DataHub 模块

DataHub 用于从 Excel 或 CSV 预览字段结构，经用户确认后创建独立的 MySQL 动态数据表，并通过不可变物理版本支持追加、覆盖、清空、编辑和回滚。

## 部署前置

- Java 17、MySQL 8.0+
- 主数据库已经存在 `sys_menu`、`sys_user`、`sys_role`、`sys_user_role`
- 应用数据库账号可以读写 `dh_*` 元数据表
- 动态表所在 Schema 已创建，应用账号具备 `CREATE`、`ALTER`、`DROP`、`SELECT`、`INSERT`、`UPDATE`、`DELETE` 权限
- 多实例部署必须使用同一个共享私有文件目录

## 数据库迁移

1. 备份主数据库。
2. 确认没有正在执行的 DataHub 导入任务。
3. 首次安装时，在主业务数据库执行 `sql/datahub_schema_20260726.sql`。
4. 基础脚本成功后，执行文件夹迁移 `sql/datahub_folder_20260727.sql`。
5. 文件夹脚本成功后，执行数据变更迁移 `sql/datahub_mutation_20260727.sql`。
6. 部署后端和前端资源并重启后端进程。
7. 在角色管理中分配“数据中心”菜单及按钮权限。

示例：

```bash
mysql --default-character-set=utf8mb4 -h DB_HOST -u DB_USER -p DB_NAME < sql/datahub_schema_20260726.sql
mysql --default-character-set=utf8mb4 -h DB_HOST -u DB_USER -p DB_NAME < sql/datahub_folder_20260727.sql
mysql --default-character-set=utf8mb4 -h DB_HOST -u DB_USER -p DB_NAME < sql/datahub_mutation_20260727.sql
```

不要重新执行 `sql/ry_20260417.sql`。两个增量迁移均可在中断后重跑；数据变更迁移创建 `uk_dh_version_job` 前输出的重复检查必须没有结果，且 SQL 客户端不得忽略任何 DDL 错误。上线前仍应记录迁移版本并备份数据库。

本次代码生成没有连接数据库，也没有执行迁移脚本。

## 配置

```yaml
datahub:
  # 仅限定动态 dh_data_* 表；dh_* 元数据仍在主业务数据库
  schema: xinke_datahub
  # 必须是私有目录，不能位于 xinke.profile 下
  storage-path: D:/xinke-private/datahub
  table-prefix: dh_data_
  max-file-size: 30MB
  max-rows: 50000
  max-dataset-rows: 200000
  max-columns: 200
  max-cell-length: 10000
  preview-rows: 20
  preview-expire-hours: 24
  insert-batch-size: 500
  recovery-stale-minutes: 30
  dispatch-batch-size: 20
  max-edit-mutations: 1000
  version-retention-days: 30
```

`datahub.storage-path` 为空时使用系统临时目录。代码会拒绝把它设置到 `xinke.profile` 或其子目录，因为 `/profile/**` 是可公开访问的静态资源路径。运行账号必须拥有该私有目录的读写和删除权限。

Spring multipart 的文件上限必须不小于 `datahub.max-file-size`。首次创建动态表后，不要修改 `datahub.schema` 或 `table-prefix`。

## 导入与失败处理

- 上传后先解析文件并返回 Sheet、字段翻译、类型推断和样例数据，不会立即建表。
- 确认后任务由数据库原子领取，再进入后台线程池；多次确认不会启动多个 Worker。
- 任意一行类型校验失败时整批不发布，最多记录并展示 1000 条错误。用户可以返回结构配置修改后重试。
- 数据先写入 staging 表，再重命名为不可变版本表，最后用事务发布元数据指针。
- 应用重启后，`QUEUED` 任务会重新派发；超过恢复阈值的 `VALIDATING`、`STAGING`、`COMMITTING` 任务会重新排队。
- 元数据提交结果不确定时不会删除已经重命名的目标表，避免破坏可能已经生效的版本。
- 追加、覆盖、清空和编辑都会生成新的不可变版本；回滚只切换到仍在保留期内的已有版本。
- 旧版本默认保留 30 天；清理采用 `PURGING` 租约状态，DDL 结果不确定时不会把版本恢复为可回滚状态。
- 预览过期后，终态任务的原始文件由定时任务清理；文件名、SHA-256 和任务统计仍保留。

恢复阈值应高于正常最大导入耗时。生产环境需要监控长期处于 `QUEUED`、`VALIDATING`、`STAGING`、`COMMITTING` 或 `MANUAL_REQUIRED` 的任务，并对照 `dh_import_job`、`dh_dataset_version` 和实际物理表处理异常残留。

## 当前限制

- 支持 `.xls`、`.xlsx`、`.csv`；CSV 支持 UTF-8、UTF-8 BOM 和 GB18030。
- 单次默认最多 30MB、50000 条原始数据记录（空行也计入）、200 列、单元格 10000 字符；追加后单个数据集默认最多 200000 行。
- CSV 按记录迭代，但编码识别、最终数据行和 Excel 工作簿仍会保留在内存，大文件需要预留足够 JVM 堆空间。
- 动态业务字段默认不建索引，复杂筛选会产生全表扫描。
- 同名展示名称和同名英文标识均全局拒绝；软删除后名称仍保留。
- 当前不提供删除数据集和按业务键 UPSERT；编辑使用行 ID 与行哈希进行并发校验。

## 数据变更 API

- `POST /datahub/dataset/{id}/import/preview`：预览 `APPEND` 或 `REPLACE` 文件。
- `PUT /datahub/dataset/{id}/import/{previewId}/sheet`：切换 Sheet 并重新预览。
- `POST /datahub/dataset/{id}/import/{previewId}/confirm`：确认字段映射并提交后台任务。
- `POST /datahub/dataset/{id}/edit`、`POST /datahub/dataset/{id}/clear`：编辑或清空数据。
- `GET /datahub/dataset/{id}/versions`、`POST /datahub/dataset/{id}/versions/{versionId}/rollback`：查看和回滚版本。
- `GET /datahub/import/{previewId}`、`GET /datahub/import/{previewId}/errors`：提交者或管理员轮询任务与错误。

## 后续追加与覆盖约束

- `APPEND` 必须先写 staging，再在一个 InnoDB 事务中执行 `INSERT ... SELECT`；通过 `_import_job_id` 支持失败恢复和追加撤销。
- `REPLACE` 必须创建新的不可变物理版本，校验完成后只切换 `current_version_id/current_schema_id`，旧版本保留到保留期结束。
- `ROLLBACK` 只把指针切回已有版本，不创建引用同一物理表的新版本行。
- 同一数据集的写任务必须通过 `active_job_id` 和数据库条件更新串行化，不能依赖单机 JVM 锁。
- `UPSERT` 上线前必须由用户明确确认业务键，并为业务键建立相应索引和重复数据策略。

## 应用回滚

应用版本回滚时先停止新导入，保留 `dh_*` 元数据、`dh_data_*` 物理表和未过期源文件，再恢复上一版后端和前端。完全卸载前必须备份，并从 `dh_dataset_version`、`dh_import_job` 精确取得表名后逐表处理；禁止使用通配方式批量删除 `dh_data_*`。
