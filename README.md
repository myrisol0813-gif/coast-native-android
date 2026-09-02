# coast-native-android

Private Android native client for Elementera Coast / CoastGPT.

**Status: Native v1 local review shell aligned through the app-60 body plus reviewed app-62 deltas. Real backend wiring is intentionally deferred.**

Current source of truth: `docs/NATIVE_APP60_SURFACE_PARITY_MAP.md`.

## Product identity

- App: `CoastGPT` / `com.elementeracoast.app`
- Gate: `Elementera Coast` · `沿海岸保存回声`
- Kotlin / Jetpack Compose only
- no WebView, React Native, TypeScript or committed Web/PWA assets

## Native body

- tuned drawer/topbar, `RoomType` and one shared `ChatWindow`
- Main history is concrete; Radio / Lighthouse sidebar entries are transient empty room landings
- there are no prebuilt `radio-1` / `lighthouse-1` fixture conversations
- first send in Radio / Lighthouse creates a fresh persistent conversation before writing messages; explicit New still creates immediately
- created Radio / Lighthouse conversations appear in sidebar history and keep their local thread when revisited
- main chat user/assistant body text renders at normal font weight; headings/actions keep their own hierarchy
- chat top-right More opens local `登岛信`
- island letter uses current-model `To ...` text and is stored independently per conversation + model; delivery remains unwired
- assistant actions: copy / like / favorite / local regenerate / delete / footprint
- user edit creates paired local user + assistant variants; fake generation targets the exact assistant variant
- local thought-soil / furniture transparency remains explicit
- Wolf Den: 个人资料 / 外观 / 聊天记录 / 模型箱 / 基本设置 / 关于与诊断
- Appearance accent presets are parsed as ordinary opaque ARGB colors; the old packed-color `ULong` path is forbidden by CI
- Model Box renders the current model as one compact solid bubble; all unselected models stay inside one grouped expandable catalog
- catalog groups: o / GPT-4 / GPT-5 / other OpenAI Chat / Free Test / image
- Refresh is visible but explicitly offline until backend OpenRouter catalog wiring exists; no fake network result
- sidebar identity is `Serpent Desk / 小蛇书桌`; opening it lands on the desk home, whose current tool shelf contains `小蛇行动日志`
- furniture deep links open the Action Log tool inside Serpent Desk
- Daily: 碳硅圈 / 日记 / blank Pet interior
- Carbon Circle top profile selectors are one compact 小寒 / Myri avatar strip; the Myri avatar remains the same `DailyStore.myriAvatarUri` consumed by homepage chat
- Carbon Circle moment body uses normal weight; comments use bold author + normal content at larger content-area size; a light divider separates content from actions and time stays small
- Carbon Circle keeps like/comment/delete on the first action row and a separate small second-row `叫 Myri 来评论` chip
- the Myri-comment chip is intentionally local-only for now: it logs the request and states that real model commenting waits for backend wiring; it never fabricates a Myri comment
- Memory: 记忆库 / 种子库 / 世界书 / 自定义指令 in one 2×2 surface with real local filters
- local JSON / HTML chat export and JSON import
- light / dark / gold plus local bubble/accent appearance

## State ownership

```text
feature/chat         LocalChatStore + message UI + LocalFurnitureOrchestrator
feature/wolf         WolfStore + ModelCatalog + ModelBoxScreen
feature/letters      IslandLetterContent + IslandLetterStore + IslandLetterScreen
feature/serpentdesk  SerpentDeskScreen + visible tool shelf/navigation
feature/actionlog    ActionLogStore + ActionLogScreen tool implementation
feature/daily        DailyStore + MomentCard + MomentActions + Daily surfaces
feature/memory       MemoryStore + Memory surfaces
feature/shell        navigation + transient room landing/materialization coordination
core/local           SharedPreferences / in-memory persistence boundary
ui/theme             theme/color parsing + appearance composition
core/model           shared stable shell/chat models
```

A Radio/Lighthouse landing is not a hidden conversation or second thread store. The first real send creates one normal `LocalChatStore` conversation and all subsequent message operations use that exact thread.

## PWA reference

The main Native body was reviewed against PWA app-60. Later read-only app-62 review confirms the Serpent Desk home/tool-shelf structure and the current Daily model-comment action. Web/PWA source remains reference-only and is never copied into this repository.

## Hard boundaries

- no `android.permission.INTERNET`
- no real login/API/SSE/OpenRouter request/model call
- no base URL/token/password/provider secret
- no real Daily/Memory/MCP sync
- no fake Myri-authored Daily comment while model-comment backend wiring is absent
- no o3 reply-card changes
- no Calendar / Today Coast / Summary / Album
- no restored old desk pseudo-settings
- no hidden temporary Pet controls/state
- no `legacy` / `compat` / `bridge` / `temp` / `misc` source layer

`core/network/CoastGatewayClient.kt` remains an unwired future interface.

## Dogtalk invariant

```text
body
true_core
weather
read_mode
```

## Build

Requires JDK 17, Android SDK 34, Gradle 8.9.

```bash
gradle :app:testDebugUnitTest --no-daemon --stacktrace
gradle :app:assembleDebug --no-daemon --stacktrace
```

GitHub Actions runs both and stages the debug APK artifact.

## Attribution

Selected earlier PoC interaction patterns from MiniiChat remain attributed under MIT in `THIRD_PARTY_NOTICES.md` and `third_party/MiniiChat-LICENSE.txt`. No GPL/AGPL implementation is included.
