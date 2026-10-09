# 直播采集客户端启动方式

## 本机安装

正式安装建议使用生成的 `xinke-live-customer-setup-版本.exe`。安装程序会自动复制插件、创建桌面快捷方式和开机启动项。

如果暂时没有安装包，也可以继续使用下面的脚本方式。

在项目根目录打开 PowerShell，执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\install-live-customer-autostart.ps1
```

脚本会创建：

- 桌面快捷方式：`直播采集启动`
- 开机自动启动项：`直播采集启动`

第一次打开后，在专用 Chrome 窗口登录抖音。以后使用这台电脑时，开机即可自动打开采集页面。

## 日常使用

1. 开机后等待 Chrome 自动打开。
2. 确认抖音主播后台中的直播间名称正确。
3. 插件会自动开始采集，不需要每天手动点击开始。
4. 如果插件提示连续同步失败，先检查网络和服务器，再点击插件中的“恢复监控”。

## 注意事项

- 这台电脑只绑定一个直播间，不要在专用 Chrome 中切换其他直播账号。
- 日常浏览请使用普通 Chrome，不要关闭专用 Chrome 的登录状态。
- 插件更新后重新执行一次安装脚本即可更新快捷方式指向。
- 启动器每次打开时会检查线上版本；有新版且服务器提供更新包地址时，会先校验并替换插件，再打开 Chrome。
- 如果服务器将最低版本设得高于本机版本，但没有提供更新包或校验失败，启动器会停止打开采集页面并提示联系管理员。
- 发布新版时可在项目根目录执行 `powershell -ExecutionPolicy Bypass -File .\tools\package-live-customer-plugin.ps1`，将生成的 ZIP 放到可通过 HTTPS 访问的位置，并把地址和 `.sha256` 文件中的值配置到 `LIVE_CRM_V2_UPDATE_URL`、`LIVE_CRM_V2_UPDATE_SHA256`。
- 如需取消开机启动，删除启动文件夹中的 `直播采集启动.lnk` 即可；桌面快捷方式可以保留。
