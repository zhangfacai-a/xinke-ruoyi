param([string]$Version='0.3.4')
$ErrorActionPreference='Stop'
$repository=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$desktop=[Environment]::GetFolderPath('Desktop')
$folder=Join-Path $desktop "鑫客主播工作台-$Version-分享包"
$installer=Join-Path $desktop "xinke-anchor-metrics-agent-setup-$Version.exe"
if(-not(Test-Path -LiteralPath $installer)){throw '请先构建安装包'}
New-Item -ItemType Directory -Path $folder -Force | Out-Null
Copy-Item -LiteralPath $installer -Destination (Join-Path $folder "鑫客主播工作台安装包-$Version.exe") -Force
Copy-Item -LiteralPath (Join-Path $repository 'clients/anchor-metrics-probe/help/使用说明.html') -Destination (Join-Path $folder '使用说明.html') -Force
$runtimeInstaller=Join-Path $repository '.codex-deploy/prerequisites/MicrosoftEdgeWebView2RuntimeInstallerX64.exe'
$runtimeSignature=Get-AuthenticodeSignature -LiteralPath $runtimeInstaller
if($runtimeSignature.Status -ne 'Valid' -or $runtimeSignature.SignerCertificate.Subject -notmatch 'Microsoft Corporation'){throw '微软运行库签名验证失败，停止分发'}
Copy-Item -LiteralPath $runtimeInstaller -Destination $folder -Force
$quick=@"
鑫客主播工作台 $Version · Windows 10/11 64位

1. 解压本文件夹，双击“鑫客主播工作台安装包-$Version.exe”安装。缺少WebView2时会联网安装微软运行库。
2. 首次安装默认创建桌面图标、开启开机自启，可在安装时取消；App设置里也可关闭自启。
3. 打开App，分别登录自己的鑫客网站账号和百应账号。
4. 核对直播间及场次，选择实际上播主播。
5. 点击“采集并预览”，核对区间明细，再点击“确认上报”。

注意：
开机自启在任务栏托盘运行，不会自动上报；双击桌面图标唤起软件。
最小化进入托盘，关闭按钮退出。重复打开不会出现多个实例。
普通主播需要管理员先分配直播间。联系人或账号不会随安装包提供。
完整教程请双击“使用说明.html”，也可从App设置或开始菜单打开。
此分享包不含开发工具、网站密码、百应Cookie、本机登录缓存或用户上报数据。

如果提示缺少WebView2，或自动安装失败：
关闭App，双击同文件夹的 MicrosoftEdgeWebView2RuntimeInstallerX64.exe，完成后重新打开App。
这个微软官方离线安装程序不需要再次下载组件；App采集和上报仍需要联网。
微软官方下载页：
https://developer.microsoft.com/microsoft-edge/webview2/
"@
[IO.File]::WriteAllText((Join-Path $folder '先看这里-快速开始.txt'),$quick,[Text.UTF8Encoding]::new($true))
$notes=@"
版本 $Version · 2026-11-24

本版内容
・修复新电脑安装后工作台找不到文件：完整附带页面、脚本、样式和离线帮助，启动时检查资源完整性。
・保留首次安装初始化、自启选项的修复。
・安装前检测WebView2，缺少时安装微软官方运行库；分享包附带官方离线安装程序。
・采用聚焦上报布局，区间明细默认展开。
・本次数据和同一场次上次有效上报逐项对照，包含主播和时间区间。
・开机托盘自启开关、单实例唤起、桌面和开始菜单快捷方式。
・统一品牌图标、安装内置离线使用说明。
・通过百应官方登录中转打开场次，避免百应已登录但大屏登录过期的直达失败。

软件仍需要网络、可用的网站账号和百应账号。
管理员管理主播分配，普通主播按权限查看和上报。
"@
[IO.File]::WriteAllText((Join-Path $folder '本版更新说明.txt'),$notes,[Text.UTF8Encoding]::new($true))
$hashLines=Get-ChildItem -LiteralPath $folder -File | Where-Object Name -ne 'SHA256SUMS.txt' | Sort-Object Name | ForEach-Object { $hash=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant(); "$hash  $($_.Name)" }
[IO.File]::WriteAllLines((Join-Path $folder 'SHA256SUMS.txt'),[string[]]$hashLines,[Text.UTF8Encoding]::new($true))
$archive="$folder.zip"
Compress-Archive -LiteralPath $folder -DestinationPath $archive -Force
Get-ChildItem -LiteralPath $folder | Select-Object Name,Length
Write-Host "分享文件夹：$folder"
Write-Host "可直接发送：$archive"
