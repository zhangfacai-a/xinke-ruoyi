# 鑫客直播采集客户端安装包

## 构建环境

需要在构建电脑安装：

1. .NET 8 SDK（不是只安装 Runtime）。
2. Inno Setup 6。

## 生成安装包

在项目根目录打开 PowerShell，执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\build-live-customer-installer.ps1
```

生成文件位于：

```text
releases\xinke-live-customer-setup-2.4.0.exe
```

## 员工电脑安装

1. 确认电脑已安装 Google Chrome。
2. 双击安装包。
3. 保持默认安装目录并完成安装。
4. 第一次打开专用 Chrome 时登录抖音。
5. 以后开机后会自动打开采集窗口。

## 发布新版

1. 修改插件 `manifest.json` 的 `version`。
2. 修改 `installer.iss` 的 `MyAppVersion`。
3. 重新执行构建脚本。
4. 将新的安装包交给管理员或放到服务器下载地址。

插件目录、启动器和专用 Chrome 配置相互独立。卸载安装程序不会删除专用 Chrome 的本地缓存和日志，便于保留待同步数据。
