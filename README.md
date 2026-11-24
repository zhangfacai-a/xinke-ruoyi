# 鑫客直播运营系统

鑫客直播运营系统用于管理直播客户、主播数据上报和直播礼品等业务。项目采用 Spring Boot 与 Vue 3 前后端分离架构。

## 主要功能

- 直播客户中心：接收插件上报的观众和评论，完成客户领取、跟进、资料维护与导出。
- 主播数据上报：接收插件采集的直播经营指标，按主播交接区间计算业绩，并支持补录和调整。
- 直播礼品：维护礼品记录、个人设置、快捷模板、库存与成本汇总。
- 系统管理：管理用户、角色、菜单、部门、字典、日志和系统配置。

## 项目结构

```text
xinke-admin        后端启动入口与系统控制器
xinke-framework    安全框架、拦截器和通用配置
xinke-system       用户、角色、菜单、字典等基础服务
xinke-erp          直播运营后端模块（保留旧模块名以兼容现有代码）
xinke-generator    代码生成器
xinke-common       通用工具和基础组件
xinke-ui           Vue 3 前端
clients            插件、桌面启动器和发布包
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

## 旧模块清理

传统 ERP、财务、旧 BI 和 DataHub 代码已经下线。部署新版本时，在业务数据库执行一次 `config/database/remove-unused-business-menus.sql`，可移除对应菜单、角色授权和旧 ERP ETL 任务配置。该脚本不会删除业务表或历史业务数据，执行后重启后端并重新登录。

通用 Quartz 任务模块已移除。直播提醒、钉钉同步和客户转交仍由直播模块内的 Spring 定时任务执行。

## 许可

本项目基于 MIT 协议开源。
