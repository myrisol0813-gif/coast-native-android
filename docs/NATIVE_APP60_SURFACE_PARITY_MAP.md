# Native ↔ PWA app-60 surface parity map

Product mother: `myrisol0813-gif/elementera-coast@0b9ca38d50738d0c367637ccd06579a4f5feacfe` (`coast-app-60`).

This Native review branch still grows from Native 25 (`f3d8c2519fd64827ad8075c6a7df891ce62910a4`). The abandoned PR #3 implementation is not reused. PWA source is read-only reference; Web/PWA code and assets are not copied into this repository.

## Current surface map

| PWA / reviewed surface | Native local implementation |
| --- | --- |
| Gate | preserved Native 25 source-faithful Coast mark / password entrance |
| MainShell / Drawer | preserved tuned Native 25 shell and conversation layout |
| Main / Radio / Lighthouse | one shared `ChatWindow` / `RoomType`; local threads start empty |
| Chat model label | bounded / ellipsized, no trailing chevron |
| Chat More | opens `登岛信` for the current conversation and current model |
| 登岛信 | `feature/letters`; default first line is `To <current model>：`; local save/reset is scoped by conversation + model; delivery remains unwired |
| Assistant actions | copy / like / favorite / local regenerate / delete / footprint |
| User actions / variants | edit + copy; edit creates paired user/assistant variants and fake-regenerates the exact assistant branch |
| 本轮家具 | explicit local action ids bound to the assistant message; no timestamp guessing |
| Dogtalk | existing four-field local drawer unchanged |
| 思维壤 | current-delivery / recent-context / local-settings / memory / furniture preview |
| Wolf Den | 个人资料 / 外观 / 聊天记录 / 模型箱 / 基本设置 / 关于与诊断 |
| Wolf Profile | nickname, export display name, bubble-color entry; redundant avatar-location reminder removed |
| 模型箱 | selected current model stands alone; all unselected entries live inside one grouped catalog card and expand for details |
| Model groups | o 系列 / GPT-4 系列 / GPT-5 系列 / 其他 OpenAI Chat / Free Test / 图片模型 |
| Model refresh | visible local button; explicitly reports that OpenRouter refresh awaits backend wiring and never fakes network data |
| Chat archive | local JSON / HTML export + JSON import into current window |
| Basic Settings | exact 11 active local settings |
| 小蛇书桌 | visible name restored to `Serpent Desk / 小蛇书桌`; its only current interior is the existing Action Log owner |
| Action Log | persistent redacted local action records with type/status/conversation/action-id filters |
| Daily | 碳硅圈 / 日记 / 宠物系统; Pet interior intentionally blank |
| 碳硅圈 | local cover/avatar/profile, Xiaohan/Myri authors, publish/like/comment/delete; confirmed delete |
| 日记 | local add/edit/delete/date/weather/mood/tags; compact edit/delete icons |
| Memory | 记忆库 / 种子库 / 世界书 / 自定义指令 in one 2×2 surface |
| Memory filters | 日期 / 模型 / 窗口 / 标签 with real local filtering; chips use a clear accent-tinted/outlined pair rather than muddy shadow fills |
| 世界书 | remains in the same Memory surface; `新增` opens `新增世界书`; local hit test / CRUD / enable-disable |
| Typography / depth | Android platform sans, heavier compact titles, smaller gray subtitles, restrained card depth |
| Network | no `INTERNET` permission, no base URL, no real OpenRouter/API/SSE call |

## app-60 source files read

- `elementera-mcp/deploy-pages/index.html`
- `elementera-mcp/deploy-pages/public/features/settings.js`
- `elementera-mcp/deploy-pages/public/features/models.js`
- `elementera-mcp/deploy-pages/public/features/models/models-view.js`
- `elementera-mcp/deploy-pages/public/features/models/models-constants.js`
- `elementera-mcp/deploy-pages/public/features/letters.js`
- `elementera-mcp/deploy-pages/public/content/island-letter.js`
- earlier active chat / daily / memory modules already used by the Native 26 local shell
- user-supplied phone screenshots used for Native visual review

The current PWA confirms `Serpent Desk / 小蛇书桌`, the standalone `登岛信`, current model catalog grouping, and model-specific `To ...` island-letter content. Native mirrors those product structures without pretending to share the Web backend.

## Feature ownership

```text
feature/chat       LocalChatStore + message UI + LocalFurnitureOrchestrator
feature/wolf       WolfStore + ModelCatalog + ModelBoxScreen + profile/appearance/records/settings/diagnostics
feature/letters    IslandLetterContent + IslandLetterStore + IslandLetterScreen
feature/actionlog  ActionLogStore + ActionLogScreen (visible through Serpent Desk)
feature/daily      DailyStore + Carbon Circle / Diary / blank Pet surface
feature/memory     MemoryStore + Memory / Seed / Worldbook / Custom Instructions surfaces
feature/shell      navigation, drawer, topbars and cross-feature coordination only
core/local         local persistence boundary
core/model         shared stable shell/chat models
```

`CoastShellViewModel` coordinates navigation and local chat generation. It does not own Wolf, Letter, Daily, Memory or Action Log feature data.

## Model-box rule

1. The current model is rendered alone above the catalog.
2. It is excluded from the unselected catalog.
3. Unselected entries are grouped by model family inside one large catalog surface.
4. Each model begins as a compact name bubble; tapping expands id, family and source information plus `设为当前`.
5. Empty groups display `暂无目录项`; Native does not manufacture placeholder models.
6. Refresh remains an explicit offline placeholder until the real backend can retrieve the OpenRouter directory, after which the same classifier can receive the real list.

## Island-letter rule

1. Top-right More in the chat shell opens `登岛信`.
2. The default body is derived from the current PWA app-60 island-letter source and begins with `To <current model>：`.
3. Local storage key is scoped by `conversationId + modelName`.
4. Switching model or conversation therefore opens that pair's own letter/default.
5. Save and reset are real local actions.
6. `递出登岛信` saves locally but clearly reports that the Native read/delivery endpoint is not connected; it never pretends a model received the letter.

## Retired / absent

There is no active or hidden route for Calendar / 今日一瞥 / 海岸日历, Daily Summary / 一日总结, Album / 相册, `FeatureDestination.Desk`, or the former Serpent Desk pseudo-settings clutter. `Serpent Desk / 小蛇书桌` is now only the visible shell for the active Action Log feature; old desk portrait/bubble/construction-junk UI is not restored.

No `legacy`, `compat`, `bridge`, `temp`, or `misc` source layer is introduced.

## Network boundary

This review pass does not add `android.permission.INTERNET`, real login/API/SSE/OpenRouter retrieval, model calls, Daily/Memory sync, MCP, o3 reply-card, WebView, React Native, TypeScript, base URL, token or provider secret. `CoastGatewayClient` remains an unwired future interface.
