# Native app-59 Surface Parity Map

Source of truth: Web/PWA app-59. Native target: `COAST-NATIVE-APP59-FULL-LOCAL-PARITY-26`.

This pass is intentionally local-only. It does not add a backend client, SSE, real model calls, real MCP, or `INTERNET` permission.

| PWA app-59 surface | Native 26 status | Native owner / note |
| --- | --- | --- |
| Gate | implemented | `feature/gate` local gate |
| MainShell | implemented | `feature/shell` |
| Sidebar | implemented | Main / Radio / Lighthouse / Daily / Memory / Wolf / Action Log / Appearance |
| Main Chat | implemented local | shared `ChatWindow` |
| Radio | implemented local | shared `ChatWindow`, `RoomType.Radio` |
| Lighthouse | implemented local | shared `ChatWindow`, `RoomType.Lighthouse` |
| Message actions | implemented local | assistant copy/like/favorite/regenerate/delete/footprint/variant; user copy/edit/delete/variant |
| 本轮家具气泡 | implemented local | exact `actionId` summaries bound to assistant messages |
| Dogtalk | retained local shell | existing four-field local surface |
| Model Box | implemented local | one Wolf entry + chat quick picker, mock/local list |
| Basic Settings | implemented local | 11 app-59 active settings in `LocalPreferencesStore` |
| Wolf Den / 小狼窝 | implemented local | six entries |
| 个人资料 | implemented local | nickname, export signature, bubble entry, Daily avatar-source note |
| 外观 | implemented local | light/dark/gold, user bubble, accent |
| 聊天记录导入导出 | implemented local | JSON / HTML export, JSON import through Android document APIs |
| 关于与诊断 | implemented local | version, theme, room, conversation/model/counts, INTERNET state, backend status |
| Serpent Action Log / 小蛇行动日志 | implemented local | status/action/conversation/exact action-id filtering |
| Daily / 海岸日报 | implemented local | three current entries |
| 碳硅圈 | implemented local | create/edit/delete/like/comment + Daily profile avatar/cover |
| 日记 | implemented local | create/edit/delete + date/weather/mood/tags |
| 宠物系统入口 | implemented local light shell | resting/active/sleepy + pet/sleep/story/box actions; future global-state note |
| Memory / 记忆 | implemented local | four app-59 entries |
| 记忆库 | implemented local | CRUD/search + title/life_core/content/usage/avoid/tags |
| 种子库 | implemented local | CRUD/search + active/dormant |
| 世界书 | implemented local | CRUD/search + enable/disable |
| 自定义指令 | implemented local | edit/save/clear; explicitly not sent to real model in this pass |
| Settings / Theme | implemented local | Wolf appearance + sidebar appearance entry |
| Toast / Snackbar | implemented | local completion/error/unwired notices |
| Overlay / Dialog / BottomSheet | implemented | editors, model picker, thought-soil sheet |
| 思维壤 / 手持种 | implemented local shell | current input, recent context, settings, local memory hits, furniture |

## Deliberately retired / excluded

The Native target does not restore Calendar, Today Coast, Summary, or Album. It also does not retain the old Serpent Desk, Myri portrait/bubble cards, desk note, duplicate turn desk, or hidden placeholder routes.

## Local ownership

- `feature/chat`: ChatWindow, messages, message actions, furniture bubble, thought soil.
- `feature/wolf`: profile, appearance, chat records, model box, basic settings, diagnostics.
- `feature/actionlog`: local action transparency and filters.
- `feature/daily`: moments, diary, pet light shell.
- `feature/memory`: memories, seeds, worldbook, custom instructions.
- `feature/shell`: navigation, top bar, drawer, feature routing.
- `core/model`: stable Native local domain shapes.
- `core/local`: persistence and feature stores.

`CoastShellViewModel` coordinates navigation and fake generation. Feature data ownership stays in the feature stores above.
