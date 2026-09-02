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
| Chat topbar model | no chevron; larger model label with bounded/ellipsis display |
| Assistant message actions | copy / like / favorite / local regenerate / delete / footprint |
| User message actions | normal state: edit + copy; edited messages gain real local variants and current-variant delete/navigation |
| User variants | edit appends a variant; counter is total/current (`2/1`, `2/2`); single variant shows no `1/1`; delete removes only current variant |
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
| Daily home | PWA-like page chrome and large quiet cards for 碳硅圈 / 日记 / 宠物系统 |
| 碳硅圈 | changeable cover; Xiaohan/Myri local avatar sources; local publish/edit/delete/like/comment; PWA-like compose surface |
| Myri avatar | one local source in `DailyStore`; chat assistant avatar and Carbon Circle profile row share it |
| 日记 | PWA-like empty/list/compose surfaces; local add/edit/delete/date/weather/mood/tags |
| 宠物系统 | entrance retained; interior intentionally blank until its dedicated UI round; no hidden pet-state model remains active |
| Memory | PWA-like 2×2 tabs: 记忆库 / 种子库 / 世界书 / 自定义指令 |
| 记忆检索 | search plus 日期 / 模型 / 窗口 / 标签 dropdown filters; all four dimensions operate on local source metadata |
| 记忆库 | local add/edit/delete/search, Memory v2 content fields plus source model/window/date facets |
| 种子库 | local add/edit/delete/search, active/dormant plus source/tags facets |
| 世界书 / 海岸词典 | separate PWA-like page with “试一句” local hit test, add/edit/delete/enable-disable |
| 自定义指令 | PWA-like single active document, local edit/save/clear, explicitly not sent to backend |
| Theme | sidebar cycle plus Wolf Appearance selector |
| Typography | platform sans; slightly heavier body weights and softer foreground colors, without bundled font assets |
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
- `elementera-mcp/deploy-pages/public/features/daily.js`
- `elementera-mcp/deploy-pages/public/features/memory.js`
- `elementera-mcp/deploy-pages/public/features/memory/memory-constants.js`
- `elementera-mcp/deploy-pages/public/features/memory/memory-library-view.js`
- user-supplied PWA app-59 phone screenshots for Chat, Memory, Daily, Carbon Circle, Carbon compose, Diary and Diary compose
- app-58 → app-59 changed-file map from commit `c4dd2ad...`

The app-59 source explicitly owns the Wolf six-entry structure, the 11 active Basic Settings fields, the Serpent Action Log naming, per-assistant explicit furniture run metadata, Memory retrieval dimensions and Daily profile surfaces. Native mirrors those product structures without pretending to share the Web backend implementation.

## Local state ownership

```text
feature/chat       LocalChatStore + message UI + LocalFurnitureOrchestrator
feature/wolf       WolfStore + profile/appearance/records/model/settings/diagnostics
feature/actionlog  ActionLogStore + ActionLogScreen
feature/daily      DailyStore + Carbon Circle / Diary / blank Pet surface
feature/memory     MemoryStore + Memory PWA surface components
feature/shell      navigation, drawer, topbar and cross-feature coordination only
core/local         LocalPersistence + line-safe persistence codec
core/model         shared stable shell/chat models
```

`CoastShellViewModel` coordinates shell/navigation/generation and user-message variant mutation. Feature data is owned by feature stores; `MainShell` only routes those owners. Memory and Daily own their own feature-page chrome so their PWA-like dynamic titles/actions are not duplicated by the global shell topbar.

## User-message variant rule

Native local edit behavior follows the reviewed PWA interaction:

1. a one-version user message shows only edit + copy;
2. edit preserves the old text and appends a new variant;
3. the newly edited variant becomes current, so two versions show `2/2`;
4. navigating back to the first variant shows `2/1`;
5. delete in variant mode removes only the current variant and never adjacent messages;
6. when one variant remains, the counter and variant-delete chrome disappear instead of displaying `1/1`.

This is local UI state, not a claim about a future backend variant schema.

## Memory retrieval rule

The PWA filter-kind menu is mirrored as:

- 日期 → 全部日期 / available local source dates / 暂无日期
- 模型 → 全部模型 / available local source models / 暂无模型
- 窗口 → 全部窗口 / available local source windows / 暂无窗口
- 标签 → 全部标签 / canonical and local tags

Canonical PWA tag labels retained in the Native local surface: `关系`, `历史锚点`, `偏好`, `人物档案`, `海岸世界观`, `工程技术`.

Existing older local rows are read directly with default source metadata when those appended fields are absent; the active writer only emits the current structure. There is no second legacy writer or compatibility route.

## Daily profile rule

Carbon Circle owns the local Daily profile image sources:

- `profileAvatarUri` for Xiaohan
- `myriAvatarUri` for Myri
- `coverUri` for the Carbon Circle cover

The chat assistant avatar reads/writes that same `myriAvatarUri`; it does not maintain a hidden second avatar preference. Android `OpenDocument`/persistable URI access is used without storage permission or upload.

## Furniture binding

Native follows app-59's explicit binding rule:

1. local fake-tool owner executes an action;
2. `ActionLogStore.record()` creates an explicit `actionId` with safe summaries;
3. the resulting `FurnitureRun` is attached directly to the target assistant `ChatMessage`;
4. UI renders no bubble when the list is empty;
5. “查看小蛇行动日志” opens Action Log focused by those exact ids.

No timestamp-window guessing is used.

## Local-only actions

Working locally: message copy/like/favorite/edit/delete/regenerate, user variant switching/current-variant deletion, model switch, Wolf settings/profile, JSON/HTML export, JSON import, Carbon Circle cover/avatar/publish/edit/delete/comment/like, Diary CRUD, Memory/Seed/Worldbook CRUD/search/filter, Worldbook hit test, Custom Instructions save/clear, local fake furniture actions and Action Log filters.

Placeholders remain explicit for real profile sync, real model catalog, real backend history/SSE, attachment/mic/call transport, and any future Pet service. The Pet interior is intentionally empty rather than presenting a temporary local behavior as product design.

## Hard-retired / absent

No active route, hidden route, state field, UI label or test target is added for:

- Calendar / 今日一瞥 / 海岸日历
- Daily Summary / 一日总结
- Album / 相册
- old Serpent Desk pseudo-settings (Myri portrait/bubble/desk notes/construction junk)
- old run-control fields such as conversation/global seed/memory limits or auto-refresh controls
- old temporary pet-state controls from the first Native 26 draft
- API sandbox / 运行水闸

No `legacy`, `compat`, `bridge`, `temp`, or `misc` source directory is introduced.

## Network boundary

This pass does not add `android.permission.INTERNET`, a base URL, token, WebView, React Native, TypeScript, real SSE, MCP integration or o3 reply-card integration. `CoastGatewayClient` remains an unwired future interface.
