# 直播辅助工具

## 主播工作台

- `anchor-metrics-probe/`：现用 Windows 主播 App，登录百应、采集指标、预览与上传区间数据、分配主播及检查更新。
- `tests/desktop-api/`：桌面协议和更新流程测试。
- 安装打包：`anchor-metrics-probe/publish.ps1`；分享包整理：`scripts/package_anchor_workbench.ps1`。
- 使用说明：`docs/anchor-workbench-user-guide.md` 和 App 内置帮助。

## 直播客户采集插件

- `extensions/douyin-live-customer-v2/`：独立的直播评论/客户采集插件，继续使用。
- `scripts/package-live-customer-plugin.ps1`：插件打包入口。

历史主播插件和启动器已停止分发，详见 `LEGACY.md`。`clients/releases/` 是忽略的本地产物目录，不是源码或部署入口。
