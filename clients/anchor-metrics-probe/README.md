# 鑫客主播工作台

0.3.4增加WebView2运行库预检与缺少时的微软安装引导，中文提示组件缺失。分享文件夹附带微软Evergreen x64离线安装程序，对方现有0.3.3也可单独补装后继续使用。组件缓存和签名校验见docs/anchor-app-release-0.3.4.md。

发布脚本会重建Vue、生成资源哈希清单、向全新目录发布并验证外置HTML/JS/CSS后再制作安装包。运行库Bootstrapper需放在.codex-deploy/prerequisites/MicrosoftEdgeWebview2Setup.exe；分享用离线运行库位于同目录MicrosoftEdgeWebView2RuntimeInstallerX64.exe，均必须保持微软有效数字签名。

0.3.1：聚焦上报界面将本次区间十项明细默认展开并对照上次有效上报；包含历史补录/调整/作废、直播间主播分配、开机自启、托盘运行、单实例唤起和离线帮助。先登录网站账号；百应登录与网站登录分别保持。普通主播只可上报本人及已分配直播间，管理账号可管理名单和历史区间。采集凭据仅留在本机。网站令牌及待确认草稿使用 Windows 当前用户保护存储。

安装包默认提供桌面快捷方式和当前用户自启任务，App设置页可切换自启。`--startup`参数后台启动，仅初始化业务界面，用户打开后才初始化百应采集页；双击托盘/再次运行程序唤起同一实例。最小化进入托盘，关闭按钮退出。

构建前运行 `scripts/build_anchor_desktop_assets.py` 更新图标及离线说明。构建并签名后运行 `scripts/package_anchor_workbench.ps1` 整理桌面分享文件夹和ZIP。分享包不包含本机登录目录或发布私钥。

工作台界面源码位于 `xinke-ui/src/anchor-desktop/`。在 `xinke-ui` 下运行 `npx vite build --config vite.anchor.config.js --mode desktop` 生成随安装包发布的本地界面，不能只更新网站来更改已安装的本地界面。

首次打开时在内置浏览器登录百应。场次加载成功后，App 默认读取最新可打开的大屏场次，其他场次从“切换场次”选择。主界面显示采集结果。若列表需要翻页，选择“浏览更多场次”。App 在大屏加载前自动配置所需九项指标；有效响应缺项且请求未选齐时，校正配置并重新加载一次。若仍缺项，选择“补齐指标”查看百应页面。百应登录状态保存在 `%LOCALAPPDATA%\\XinkeAnchorMetricsProbe\\WebView2Profile`。

2026-11-24 已在 Chrome 验证：达人大屏从本地存储 `COMPASS_LIVE_SCEEN_CORE_INDICATORS_QIANCHUAN_TALENT_HIT` 读取指标数组。修改数组并重载后，官方页面按该数组生成带签名的 `core_data` 请求。App 在顶层 HTTPS 达人大屏文档创建时写入此配置，覆盖主 WebView 和官方弹窗；数值仍只读取接口响应 JSON。不会把埋点接口当成配置接口，也不会修改已有签名 URL。接口选齐仍不返回的值保持缺失，并记录日志，不会循环刷新或补零。

完整的指标快照要求九项核心指标及单独返回的成交金额。互动人数按累计观看人数和观看互动率估算，含异常交易金额仅供参考。正式上报仍由现有上报入口处理。

右上角“检查更新”从 `http://47.116.62.234/anchor-app/latest.json` 查询版本。清单用本机 RSA 私钥签名，客户端内置公钥校验清单，并校验安装包 SHA-256。用户确认后安装程序覆盖应用文件，保留 WebView2 登录目录。

以后发布新版时

1. 将 AnchorMetricsProbe.csproj 中的 Version 改为新版本，并同步调整 installer.iss 的默认版本。
2. 构建本地 Vue 界面后，在保留发布私钥的电脑运行 `./clients/anchor-metrics-probe/publish-update.ps1 -Notes '本次更新说明'`。
3. 脚本在桌面生成安装包，并在 `clients/build/anchor-app-release/` 生成对应的签名 `latest.json`。默认只构建和签名。
4. 管理员在网站“主播App软件更新”（`http://47.116.62.234/anchor-console/anchor-releases.html`）上传这两个文件，校验通过后选择“发布此版本”。网站采用分片上传，适配当前 Nginx 上传限制。网站不保存发布私钥。
5. 旧版客户端点击“检查更新”，下载安装包并安装；新版在“设置与更新”中操作。未完成的采集/上报/编辑会阻止安装。
6. 网站“撤回发布”恢复上一版清单，不会让已安装客户端自动降级；需要更高版本修复或手动恢复。

发布私钥位于 `%LOCALAPPDATA%\\XinkeAnchorRelease\\update-signing-key.pem`，需要保存在发布电脑。丢失后必须重新发布含新公钥的 App，旧客户端无法验证新签名。

App 业务 API 使用专用 HTTPS 连接并固定服务器证书 SHA-256；证书配置位于服务器 `/home/deploy/anchor-tls/`，服务端附加 TLS connector 读取该目录。该连接不改变现有网站 HTTP 入口。证书更新需要同时在 App 中发布新固定值，过期前须安排轮换；不要关闭证书固定校验。脚本 `scripts/server/prepare_anchor_tls.py` 生成部署材料，私有材料目录已加入 Git 忽略。
