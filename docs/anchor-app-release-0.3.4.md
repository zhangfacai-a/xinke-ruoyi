# 主播工作台0.3.4：缺少WebView2运行库

对方截图明确为WebView2RuntimeNotFoundException：Couldn't find a compatible WebView2 Runtime installation to host WebViews。开发电脑有Edge WebView2运行库，但对方电脑没有。0.3.3资源文件修复与此为两个独立问题。

## 处理

- 分享包新增微软官方Evergreen x64离线运行库，可先安装组件再打开现有0.3.3，或使用0.3.4安装程序。
- App安装程序内置微软官方Bootstrapper。安装前检测微软文档指定的HKCU/HKLM注册信息；没有可用运行库时运行 /silent /install，安装后重新检查。缺少且安装失败则中止App安装并说明手动补装方法。
- 已有运行库时直接安装App。仅普通用户权限，沿用微软支持的per-user安装流程。
- App缺少运行库的错误用中文说明补装方法，提供微软官网下载入口，不再把所有启动失败都提示为资源缺失。
- 分享包独立运行库与安装器内嵌Bootstrapper均保留微软原文件，分发前验证Authenticode签名状态为Valid且签发主体为Microsoft Corporation。

## 来源

微软分发说明：https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/distribution

官方Bootstrapper：https://go.microsoft.com/fwlink/p/?LinkId=2124703

官方Evergreen x64独立安装程序：https://go.microsoft.com/fwlink/p/?LinkId=2124701

原始安装程序缓存于.codex-deploy/prerequisites，不提交至Git。分享包不包含用户会话或凭据。

## 验证范围

正常安装、自启选项、页面资源哈希延续实际隔离安装验证。缺少组件、补装成功、补装失败的安装流程使用仅测试编译版本的状态模拟，不卸载开发电脑的现有运行库。不能把模拟测试声称为对方电脑的实际安装结果。

2026-11-24结果：四项实际隔离安装回归、三项组件状态模拟全部通过。通过仅对子进程设置无效WebView2浏览器目录实际触发RuntimeNotFound，验证中文补装提示及官方入口显示；测试结束未修改用户系统运行库。

最终ZIP六个文件，7个App外置资源校验、全部分享文件SHA-256、清单签名及ZIP完整性通过。离线运行库294099168字节，Authenticode签名Valid，签发者Microsoft Corporation；完整ZIP大小329.29MiB。
