# 鑫客直播数据助手

Windows 托盘程序负责从鑫客服务器领取已到下播时间的班次，并启动带采集插件的独立 Chrome。

- 每 60 秒检查一次任务，没有任务时只产生一次很小的请求。
- 下播后 3 分钟首次执行，失败后间隔 2 分钟重试，最多 3 次。
- 同一任务带 10 分钟租约，多台电脑不会同时领取。
- Chrome 使用独立用户目录，首次登录巨量百应后会保留登录状态。
- 插件只读取页面 DOM，不监听或拦截巨量百应接口。

构建安装包：

```powershell
powershell -ExecutionPolicy Bypass -File clients/scripts/build-live-metrics-agent.ps1
```

部署后端前必须先执行 `config/database/add-live-metrics-automation-task.sql`。
