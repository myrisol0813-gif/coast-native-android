# CoastGPT · Native Android

Elementera Coast 的 Android 原生主聊天窗口（debug prototype）。

> Native APP 是新的主聊天窗口，不是新的大脑。后端仍然是 `https://app.elementeracoast.com`。

## 当前阶段

`COAST-NATIVE-CHAT-00`

- Kotlin + Jetpack Compose
- applicationId: `com.elementeracoast.app`
- APP 显示名: `CoastGPT`
- 登录页标题: `Elementera Coast`
- `CoastGatewayClient` 作为 APP → Coast 后端的唯一网络门框
- 复用现有 `/login` cookie session；不会保存海岸密码明文
- 真实读取 `/api/session`、`/api/models`、`/api/chat/profile`
- 真实创建/读取主聊天 conversation + history
- `/api/chat` SSE 流式回复骨架与停止生成
- 三种主题：深海旧金（默认）/ 潮汐纸白 / 夜航金
- 竖屏优先，输入区带 `imePadding()` / navigation bar inset

## Gateway 边界

APP 不内置 OpenRouter/OpenAI/Cloudflare/GitHub/Notion/MCP 密钥，也不直接请求模型供应商。

```text
CoastGPT Android
  ↓
CoastGatewayClient
  ↓
https://app.elementeracoast.com
  ↓
Elementera Coast backend
```

现有后端的 mutating API 需要 same-origin 保护，因此原生客户端会给海岸自身 POST/PUT 请求显式发送：

```http
Origin: https://app.elementeracoast.com
```

这不是绕过认证；owner session 仍由后端 `/login` 下发的 `__Host-coast_session` cookie 验证。

## 构建

CI 使用 JDK 17 + Gradle 8.9：

```bash
gradle --no-daemon test :app:assembleDebug
```

GitHub Actions 成功后会产生私有 artifact：`CoastGPT-debug`，内容为 `app-debug.apk`。

## 安全

仓库禁止提交：

- `local.properties`
- `*.jks` / `*.keystore`
- `*.apk` / `*.aab`
- 任意 API key / token / secret

本轮不做 release 签名。

## TODO

- 真机验证 OPPO Reno14 / Android 16 / ColorOS 16。
- 将 launcher icon 的临时原生 vector 替换为 **PWA 当前 icon-512.png 的原始二进制资产**；登录页已经先复刻“六瓣黑结/金色/小狗”的方向。
- 后续再做多聊天窗口管理、完整 generation detail、落袋/思维壤等能力；本轮不搬 PWA 全功能。
