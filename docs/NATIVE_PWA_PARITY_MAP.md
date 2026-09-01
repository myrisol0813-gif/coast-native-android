# Native ↔ PWA app-57 parity map

Reference: **Elementera Coast Web/PWA app-57**.

This document is the structural source of truth for `COAST-NATIVE-PARITY-CLEAN-24`.
When older Native planning documents still mention Calendar, Daily Summary, Album, Letters, `ChatScope`, or a separate room body, this map wins for the current Native v1 shell.

## 1. What this pass is

This is an in-place cleanup of the existing Kotlin / Jetpack Compose Native repository. It is **not** a rewrite.

The pass keeps the working Native body and removes structures that predate PWA app-57:

- keep Gate and Coast visual identity;
- keep MainShell and the existing chat interaction body;
- keep one shared ChatWindow / timeline / composer / model picker;
- keep local mock conversations and mock streaming;
- keep Dogtalk's four-field local contract;
- replace legacy `ChatScope` naming with PWA-aligned `RoomType` / `room_type`;
- remove retired Calendar / Today Coast / Summary / Album surfaces;
- split oversized shell/model files only where the responsibility boundary is already clear.

No Web/PWA source is copied into the Android app. The app-57 zip is reference material only and is not committed here.

## 2. Relevant Native tree after cleanup

```text
app/src/main/kotlin/com/elementeracoast/app/
├─ MainActivity.kt
├─ core/
│  ├─ contract/
│  │  └─ CoastContracts.kt
│  ├─ model/
│  │  ├─ RoomType.kt
│  │  ├─ FeatureDestination.kt
│  │  ├─ ChatModels.kt
│  │  ├─ ConversationModels.kt
│  │  └─ ShellState.kt
│  └─ network/
│     └─ CoastGatewayClient.kt
├─ feature/
│  ├─ gate/
│  │  └─ GateScreen.kt
│  ├─ chat/
│  │  ├─ ChatScreen.kt
│  │  ├─ InputBar.kt
│  │  └─ ModelPicker.kt
│  ├─ dogtalk/
│  │  ├─ DogtalkCard.kt
│  │  ├─ DogtalkReadMode.kt
│  │  ├─ DogtalkScope.kt
│  │  └─ DogtalkUiState.kt
│  ├─ shell/
│  │  ├─ MainShell.kt
│  │  ├─ CoastTopBar.kt
│  │  ├─ CoastDrawer.kt
│  │  ├─ FeatureLandingScreen.kt
│  │  ├─ ConversationActions.kt
│  │  └─ CoastShellViewModel.kt
│  ├─ daily/
│  │  └─ DailyLanding.kt
│  └─ memory/
│     └─ MemoryLanding.kt
└─ ui/
   ├─ brand/
   ├─ icons/
   └─ theme/
```

`core/model/Models.kt` is intentionally gone; the old all-in-one file had begun to mix room, feature, shell and message responsibilities.

## 3. What is preserved

The following existing Native work remains the foundation:

- `MainActivity` and Compose application shell;
- `GateScreen`, password field and Coast visual tokens;
- `MainShell` as the application body rather than a single chat screen;
- existing shared `ChatWindow` message flow;
- `InputBar` / composer interaction;
- `ModelPicker`;
- conversation rename/delete UI;
- local mock thread storage and local mock streaming;
- light / dark / gold themes;
- Wolf Den and Serpent Desk shell entrances;
- Dogtalk card and its four-field contract.

The cleanup changes ownership and naming where necessary, not the product into a new app.

## 4. PWA app-57 structural mapping

| PWA app-57 concept | Native v1 mapping |
| --- | --- |
| `conversation.room_type` | `RoomType` |
| `main` | `RoomType.Main` |
| `radio` | `RoomType.Radio` |
| `lighthouse` | `RoomType.Lighthouse` |
| Main / Radio / Lighthouse chat body | one shared `ChatWindow` |
| chat sidebar room entry | `CoastDrawer` room entries |
| current conversation | `CoastShellState.activeConversationId` |
| current room type | `CoastShellState.activeRoomType` |
| Daily / Moments | `DailyLanding` → 碳硅圈 |
| Daily / Diaries | `DailyLanding` → 日记 |
| Pet future area | `DailyLanding` → 宠物系统 placeholder |
| Memory v2 entrances | `MemoryLanding` → 记忆库 / 种子库 / 世界书 / 自定义指令 |
| model box / current model | local `models` / `currentModel`; typed `ModelProfile` reserved for wiring |
| Dogtalk | existing local card + typed `DogtalkSubmission` skeleton |

## 5. Room/window rules

Native v1 has exactly three room types:

```text
main
radio
lighthouse
```

All three render through the same `ChatWindow`.

Conversation title rules:

- Main: ordinary title, no room prefix.
- Radio: title is normalized to exactly one `【电波】` prefix.
- Lighthouse: title is normalized to exactly one `【灯塔】` prefix.

New-window behavior:

- creating a new window inherits `activeRoomType`;
- if a room entry is opened but has no local mock window, one local window is created for that room type;
- no special room gets a separate page or duplicate timeline/composer implementation.

Rename behavior:

- strips any accidental special-room prefix first;
- reapplies the target conversation's own required prefix;
- Main stays unprefixed.

Delete-current fallback:

1. prefer another conversation with the same `roomType`;
2. otherwise prefer an existing Main conversation;
3. if Main does not exist, create a local Main fallback and activate it.

Deleting a non-active conversation does not change the active room/window.

## 6. Retired structures removed from the Native surface

The current app shell no longer exposes or models:

- `FeatureDestination.Calendar`;
- Calendar / 今日一瞥 drawer entry;
- Calendar icon dependency in the drawer;
- 海岸日历;
- 日历、事件与便签;
- 月视图;
- 新建事件 placeholder;
- 一日总结;
- 相册;
- the old Daily Calendar/Summary/Album card set;
- legacy `ChatScope` / `activeScope` naming.

These are not renamed or hidden elsewhere in Daily.

## 7. Daily v1 after app-57 alignment

The Native Daily landing deliberately stays small:

1. **碳硅圈** — 海岸内部朋友圈
2. **日记** — 留下今天的纸页
3. **宠物系统** — 还在准备休憩箱

They remain local light-shell entries in this pass. No real Daily list/write endpoint is called.

## 8. Memory v1 light shell

The Native Memory landing presents the current four entrances:

1. 记忆库
2. 种子库
3. 世界书
4. 自定义指令

This pass does not load or mutate real memory data.

## 9. Kotlin typed contract skeleton

`core/contract/CoastContracts.kt` reserves transport-neutral types for the next backend wiring pass:

- `CoastConversation`
- `CoastMessage`
- `UserVariant`
- `AssistantVariant`
- `ModelProfile`
- `DailyMoment`
- `DailyDiary`
- `MemoryEntry`
- `DogtalkSubmission`

The contract file intentionally has no serializer annotations and does not pretend a final wire mapping already exists where it has not been confirmed.

`CoastGatewayClient` is also an **interface only**. It reserves names for login/profile/conversations/history/chat/models/moments/diaries/memory search without a base URL or implementation.

## 10. Dogtalk invariant

The existing four logical wire fields remain:

```text
body
true_core
weather
read_mode
```

The current Native Dogtalk remains local-only. This pass does not add persistence, an API call, or legacy Dogtalk fields.

## 11. Deliberately deferred

This pass does not implement:

- real Gate authentication;
- a real base URL or HTTP client;
- Android `INTERNET` permission;
- profile/model-box persistence;
- real conversations/history sync;
- real SSE chat;
- real Radio or Lighthouse backend behavior;
- Daily read/write;
- Memory read/write/search;
- Mailbox backend wiring;
- MCP integration;
- TypeScript / Node / React Native / WebView.

A future `COAST-NATIVE-BACKEND-WIRING` pass can implement network ownership after contracts are confirmed.

## 12. Repository boundaries

This pass changes only `coast-native-android`.

It does not modify:

- the Web/PWA main repository;
- the app-57 reference zip;
- o3 reply-card;
- MCP `tools/list`;
- backend/server code;
- secrets, credentials, passwords or tokens.

## 13. Visual direction

The app-57 screenshots are reference for hierarchy and atmosphere, not a Web asset dump.

Native should retain:

- Elementera Coast identity before generic Material defaults;
- strong page hierarchy and generous breathing room;
- large quiet cards for Daily / Memory / Desk surfaces;
- the side drawer as a real house map, not only a conversation list;
- one coherent chat body shared by all room types;
- Gate as an entrance, not an incidental login form.

Fine-grained pixel work can continue later without reopening retired product structure.
