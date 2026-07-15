# 采购运单匹配系统说明

## 部署顺序

1. 备份当前数据库。
2. 执行 `sql/purchase_waybill_match_20260712.sql`。
3. 重新打包后端：

```bash
mvn -pl xinke-admin -am -DskipTests package
```

4. 构建前端：

```bash
cd xinke-ui
npm run build:prod
```

5. 发布后重启后端和 nginx，并确认菜单权限已分配给采购角色。

## 页面入口

系统会创建“采购匹配”菜单，包含：

- 手工订单总表
- 采购汇总总表
- 数据异常
- 导入记录
- 供应商设置

## 导入规则

手工订单表上传前必须选择供应商，导入唯一键为 `supplier_id + order_no`。采购汇总表从 Excel 的“单据供应商”识别供应商或供应商别名，导入唯一键为 `document_supplier_id + purchase_order_remark + merchant_code`。

重复数据按行处理：完全相同标记重复，数据库空字段可被新文件补充，新旧非空不同会进入数据冲突表，不静默覆盖。单行失败不影响其他行。

## 匹配规则

采购汇总的 `document_supplier_id + purchase_order_remark` 匹配手工订单的 `supplier_id + order_no`，成功后写入 `matched_logistics_no`，并记录 `matched_order_id`、匹配状态、匹配时间和匹配日志。新增或修改手工订单物流单号、订单号、供应商后，会重新匹配受影响的采购汇总数据。

## 文件处理

系统不再保存原始 Excel 文件，避免长期占用服务器磁盘。导入时直接读取上传流，业务数据进入 `purchase_manual_order`、`purchase_summary`，逐行审计数据进入 `purchase_import_detail.raw_data_json`。导入批次仅保留原文件名、Sheet 名称和文件 Hash，数据库中不再保留服务器文件路径，也不提供原文件下载入口。

## 已知限制

- 当前仓库未发现用户提到的两个真实模板文件，代码按需求中的真实表头实现并支持表头预览校验。
- 本版重新匹配为同步分批执行，接口幂等；如果后续单次导入量非常大，可以在此基础上扩展独立任务队列表。
- 大数据异步导出预留在说明中，本版导出为按当前筛选条件同步生成 Excel。

## 验证结果

- 后端编译：`mvn -pl xinke-admin -am -DskipTests compile` 通过。
- 后端测试：`mvn -pl xinke-erp -am test` 通过，4 个测试。
- 前端构建：`npm run build:prod` 通过。
