# 鑫客直播运营前端

基于 Vue 3、Element Plus 和 Vite，提供直播客户中心、主播数据上报、直播礼品及系统管理页面。

使用 Node.js 18+ 和 pnpm，在本目录运行：

```bash
pnpm install
pnpm dev
```

默认开发地址为 `http://localhost:80`，后端代理默认连接 `http://localhost:8080`，可通过 `VITE_BACKEND_URL` 调整。

生产构建：

```bash
pnpm build:prod
```

构建结果位于 `dist/`。测试环境构建使用 `pnpm build:stage`。

项目依赖以 `pnpm-lock.yaml` 为准，系统运行与模块说明见根目录 README。
