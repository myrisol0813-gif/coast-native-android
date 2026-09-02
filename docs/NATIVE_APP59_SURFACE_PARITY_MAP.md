# Native ↔ PWA app-59 surface parity map

Product mother: `myrisol0813-gif/elementera-coast@c4dd2ad6260bb9d8065a97ab57ddccfd78c697ef` (`coast-app-59`).

This Native pass starts from Native 25 (`f3d8c2519fd64827ad8075c6a7df891ce62910a4`). The abandoned PR #3 implementation is not reused. PWA source is read-only reference; no Web/PWA code or assets are copied into this repository.

## Surface map

| PWA app-59 surface | Native 26 local implementation |
| --- | --- |
| Gate | preserved Native 25 source-faithful Coast mark / password entrance |
| MainShell | preserved Compose shell |
| Sidebar | preserved Native 25 layout/status/search/conversation list; footer now Theme / Wolf Den / Serpent Action Log |
| Main / Radio / Lighthouse | one shared `ChatWindow` and `RoomType` |
| Message actions | assistant copy/like/favorite/regenerate/delete/footprint/1-1; user copy/edit/delete/1-1 |
| 本轮家具 | `FurnitureBubble`, only when explicit local action ids are bound to assistant message |
| Dogtalk | existing four-field local drawer unchanged |
| 思维壤 / 手持种 | local bottom sheet: current delivery, recent context, Basic Settings, local memory, furniture |
| Model Box | one Wolf Den model-box entry plus existing quick picker |
| Basic Settings | exact 11 active app-59 fields in `BasicSettings` / `WolfStore` |
| Wolf Den | 个人资料 / 外观 / 聊天记录 / 模型箱 / 基本设置 / 关于与诊断 |
| 个人资料 | local nickname/signature; avatar explanation points to Daily profile source |
| 外观 | light/dark/gold, user bubble, accent; persistent local state |
| 聊天记录 | current-window JSON/HTML export and JSON import |
| 关于与诊断 | APK version, theme, room, conversation, model, counts, no-INTERNET, backend unwired |
| Serpent Action Log | persistent redacted local action records with status/type/conversation/action-id focus |
| Daily | local Carbon-Silicon Circle / Diary / Pet owner |
| 碳硅圈 | local publish/edit/delete/like/comment, cover and Xiaohan Daily-profile avatar source |
| 日记 | local add/edit/delete/date/weather/mood/tags |
| 宠物系统 | resting/active/sleepy + pet/sleep/story/return-box local state |
| Memory | local Memory / Seeds / Worldbook / Custom Instructions owner |
| 记忆库 | local add/edit/delete/search/category/tags and Memory v2 fields |
| 种子库 | local add/edit/delete/search, active/dormant |
| 世界书 | local add/edit/delete/search/enable-disable |
| 自定义指令 | local edit/save/clear, explicitly not sent to backend |
| Theme | sidebar cycle plus Wolf Appearance selector |
| Toast/Snackbar | shared shell snackbar for local action results / placeholders |
| Dialog | edit message, local avatar, Daily/Memory editors |
| BottomSheet | thought-soil structure preview |

## app-59 source files actually read

- `elementera-mcp/deploy-pages/index.html`
- `elementera-mcp/deploy-pages/public/features/settings.js`
- `elementera-mcp/deploy-pages/public/features/tools.js`
- `elementera-mcp/deploy-pages/public/features/toolroom.js`
- `elementera-mcp/deploy-pages/public/features/chat-state.js`
- `elementera-mcp/deploy-pages/public/features/chat/chat-furniture.js`
- app-58 → app-59 changed-file map from commit `c4dd2ad...`

The app-59 source explicitly owns the Wolf six-entry structure, the 11 active Basic Settings fields, the Serpent Action Log naming, and per-assistant explicit furniture run metadata. Native mirrors those product structures without pretending to share the Web backend implementation.

## Local state ownership

```text
feature/chat       LocalChatStore + message UI + LocalFurnitureOrchestrator
feature/wolf       WolfStore + profile/appearance/records/model/settings/diagnostics
feature/actionlog  ActionLogStore + ActionLogScreen
feature/daily      DailyStore + Carbon Circle / Diary / Pet
feature/memory     MemoryStore + four Memory v2 surfaces
feature/shell      navigation, drawer, topbar and cross-feature coordination only
core/local         LocalPersistence + line-safe persistence codec
core/model         shared stable shell/chat models
```

`CoastShellViewModel` coordinates shell/navigation/generation. Feature data is owned by feature stores; `MainShell` only routes those owners.

## Furniture binding

Native follows app-59's explicit binding rule:

1. local fake-tool owner executes an action;
2. `ActionLogStore.record()` creates an explicit `actionId` with safe summaries;
3. the resulting `FurnitureRun` is attached directly to the target assistant `ChatMessage`;
4. UI renders no bubble when the list is empty;
5. “查看小蛇行动日志” opens Action Log focused by those exact ids.

No timestamp-window guessing is used.

## Local-only actions

Working locally: message copy/like/favorite/edit/delete/regenerate, model switch, Wolf settings/profile, JSON/HTML export, JSON import, Carbon Circle CRUD/comment/like, Diary CRUD, Pet state, Memory/Seed/Worldbook CRUD/search, Custom Instructions save/clear, local fake furniture actions and Action Log filters.

Placeholders remain explicit for real profile sync, real model catalog, real backend history/SSE, attachment/mic/call transport, and any global Pet service.

## Hard-retired / absent

No active route, hidden route, state field, UI label or test target is added for:

- Calendar / 今日一瞥 / 海岸日历
- Daily Summary / 一日总结
- Album / 相册
- old Serpent Desk pseudo-settings (Myri portrait/bubble/desk notes/construction junk)
- old run-control fields such as conversation/global seed/memory limits or auto-refresh controls
- API sandbox / 运行水闸

No `legacy`, `compat`, `bridge`, `temp`, or `misc` source directory is introduced.

## Network boundary

This pass does not add `android.permission.INTERNET`, a base URL, token, WebView, React Native, TypeScript, real SSE, MCP integration or o3 reply-card integration. `CoastGatewayClient` remains an unwired future interface.
