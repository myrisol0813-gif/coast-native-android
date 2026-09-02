# Native ↔ PWA surface parity map

Baseline product mother: PWA app-60 (`elementera-coast@0b9ca38d50738d0c367637ccd06579a4f5feacfe`).

Reviewed delta for Native `0.1.13-app62-desk-fix-27`: current PWA app-62 `settings.js`, where `settings:desk` opens `小蛇书桌 / Myri 的工作台` and the current tool shelf contains `小蛇行动日志`.

PWA source remains read-only reference. Web/PWA code and assets are not copied into this repository.

## Current surface map

| Surface | Native local implementation |
| --- | --- |
| Gate | existing source-faithful Coast mark / password entrance |
| MainShell / Drawer | tuned Native shell and conversation layout |
| Main / Radio / Lighthouse | one shared `ChatWindow` / `RoomType`; local threads start empty |
| Chat More | opens local `登岛信` for current conversation + model |
| Assistant actions | copy / like / favorite / local regenerate / delete / footprint |
| User actions / variants | edit + copy; edit branches paired user/assistant local variants |
| 本轮家具 | explicit action ids bound to assistant message; no timestamp guessing |
| Dogtalk | existing four-field local drawer |
| Wolf Den | 个人资料 / 外观 / 聊天记录 / 模型箱 / 基本设置 / 关于与诊断 |
| 模型箱当前模型 | one compact solid `surfaceVariant` bubble with thin outline; no full-width alpha fill |
| 模型目录 | unselected models grouped inside one catalog card by o / GPT-4 / GPT-5 / other OpenAI Chat / Free Test / image |
| Model refresh | visible offline action; real OpenRouter retrieval still deferred |
| Serpent Desk | visible desk home (`小蛇书桌 / Myri 的工作台`) with an extensible tool shelf |
| 小蛇行动日志 | current only desk tool; persistent redacted local records with filters |
| Furniture → log | opens the Action Log tool inside Serpent Desk using exact action ids |
| Daily | 碳硅圈 / 日记 / blank Pet surface |
| Memory | 记忆库 / 种子库 / 世界书 / 自定义指令 in one 2×2 surface |
| Network | no `INTERNET` permission, no base URL, no real API/OpenRouter/SSE call |

## Ownership

```text
feature/chat         chat thread/message/furniture UI
feature/wolf         Wolf settings + ModelCatalog + ModelBoxScreen
feature/letters      Island Letter local content/store/screen
feature/serpentdesk  visible desk home + tool-shelf navigation
feature/actionlog    Action Log data/store/tool screen
feature/daily        Carbon Circle / Diary / blank Pet
feature/memory       Memory / Seed / Worldbook / Custom Instructions
feature/shell        shell navigation and cross-feature coordination only
core/local           local persistence boundary
core/model           stable shared shell/chat models
```

Serpent Desk and Action Log do not duplicate state: the desk is the container; Action Log is a tool implementation. The existing `actionLogFocusIds` remain action-log-specific navigation data for furniture deep links.

## Model-box rules

1. Current model stands alone above the catalog as a compact bubble sized to content.
2. It is excluded from the unselected catalog.
3. Unselected models live inside one large grouped catalog surface.
4. Each model starts compact and expands in place for id/family/source and `设为当前`.
5. Empty groups display `暂无目录项`; Native does not manufacture extra placeholder models.
6. Refresh remains explicitly offline until backend wiring can retrieve the OpenRouter directory.

## Serpent Desk rules

1. Sidebar opens `Serpent Desk / 小蛇书桌`.
2. Desk home shows `小蛇书桌` with subtitle `Myri 的工作台`.
3. The tool list is modeled as `SerpentDeskItem(tool, title, subtitle)` so future tools can be added without rewriting Action Log.
4. Current only tool: `小蛇行动日志` — `工具调用成功 / 失败 · 房间 · 脱敏摘要`.
5. Opening the tool shows the existing Action Log UI and offers a local back path to the desk home.
6. Furniture deep links with focus ids enter the Action Log tool directly, still inside the Serpent Desk surface.

## Hard boundaries

There is no Calendar / Today Coast / Summary / Album restoration, no old desk pseudo-settings, no hidden legacy route, no WebView/React Native/TypeScript, no `legacy` / `compat` / `bridge` / `temp` / `misc` source layer, and no real network wiring.
