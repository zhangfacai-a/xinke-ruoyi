# 鑫客直播运营系统

鑫客直播运营系统用于管理直播客户、主播数据上报和直播礼品等业务。项目采用 Spring Boot 与 Vue 3 前后端分离架构。

## 主要功能

- 直播客户中心：接收插件上报的观众和评论，完成客户领取、跟进、资料维护与导出。
- 主播数据上报：接收主播 App 采集的直播经营指标，按主播交接区间计算业绩，并支持补录和调整。
- 直播礼品：维护礼品记录、个人设置、快捷模板、库存与成本汇总。
- 系统管理：管理用户、角色、菜单、部门、字典、日志和系统配置。

## 项目结构

```text
xinke-admin        后端启动入口与系统控制器
xinke-framework    安全框架、拦截器和通用配置
xinke-system       用户、角色、菜单、字典等基础服务
xinke-erp          直播运营后端模块（保留旧模块名以兼容现有代码）
xinke-generator    开发可选代码生成器（生产包不引入）
xinke-common       通用工具和基础组件
xinke-ui           Vue 3 前端
clients            主播 App、直播客户插件与客户端测试
scripts            服务端启动与构建脚本
config             本机配置模板和数据库维护脚本
```

## 技术环境

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Node.js 18+
- pnpm

## 本地运行

后端：

```powershell
powershell -File scripts/server/manage.ps1 build
powershell -File scripts/server/manage.ps1 run
```

本机数据库、Redis 和其他私有配置从 `config/local/application-local.yml` 读取。首次使用可复制同目录的 `application-local.example.yml`，实际配置文件已被 Git 忽略。

前端：

```bash
pnpm --dir xinke-ui install
pnpm --dir xinke-ui dev
```

生产构建：

```bash
pnpm --dir xinke-ui build:prod
```

后端清理和测试也统一由同一个脚本执行：

```powershell
powershell -File scripts/server/manage.ps1 clean
powershell -File scripts/server/manage.ps1 test
```

Linux 服务器使用 `sh scripts/server/manage.sh`，参数与 PowerShell 版本一致。

## 网站整理与发布

网站按客户运营、主播运营、礼品管理、运费管理、人员与业务配置和系统维护分区。运费、京东客户、App 版本发布统一接入主站；旧地址保留跳转。

人员分配通过共享人员组和独立店铺/直播间配置管理，自动角色由有效岗位统一同步；用户详情可查看权限来源。钉钉同步保留网站管理的停用状态。

完整发布、隔离测试、数据库备份与回退说明见 [网站整理操作](docs/operations/website-reorganization.md)。数据库脚本入口见 [迁移说明](config/database/README.md)，禁止整目录批量执行 SQL。传统 ERP、财务、旧 BI 和 DataHub 已退出网站，历史表保留；仍被现用业务依赖的店铺、礼品及 BI 事实表继续维护。

生产不引入代码生成器；开发确需使用时加 Maven `-Pdev-tools`。本地真实配置不进入 Git，服务器使用受限私有配置文件或环境变量，必须提供 `TOKEN_SECRET`。

## 许可

本项目基于 MIT 协议开源。
