# coast-native-android

Private Android native client for Elementera Coast / CoastGPT.

**Status: Native v1 local review shell aligned through the app-60 body plus the reviewed app-62 Serpent Desk delta. Real backend wiring is intentionally deferred.**

Current source of truth: `docs/NATIVE_APP60_SURFACE_PARITY_MAP.md` (app-60 baseline with later reviewed deltas recorded in-place).

## Product identity

- App: `CoastGPT` / `com.elementeracoast.app`
- Gate: `Elementera Coast` · `沿海岸保存回声`
- Kotlin / Jetpack Compose only
- no WebView, React Native, TypeScript or committed Web/PWA assets

## Native body

- preserved tuned drawer/topbar, `RoomType` and one shared `ChatWindow`
- Main / Radio / Lighthouse threads start empty; no fixture assistant message appears before user input
- chat top-right More opens the local `登岛信`
- island letter uses current-model `To ...` text and is stored independently per conversation + model; delivery remains unwired
- assistant actions: copy / like / favorite / local regenerate / delete / footprint
- user edit creates paired local user + assistant variants; fake generation targets the exact assistant variant
- local thought-soil / furniture transparency remains explicit
- Wolf Den: 个人资料 / 外观 / 聊天记录 / 模型箱 / 基本设置 / 关于与诊断
- Model Box renders the current model as one compact solid bubble; all unselected models stay inside one grouped expandable catalog
- catalog groups: o / GPT-4 / GPT-5 / other OpenAI Chat / Free Test / image
- Refresh is visible but explicitly offline until backend OpenRouter catalog wiring exists; no fake network result
- sidebar identity is `Serpent Desk / 小蛇书桌`; opening it now lands on the desk home, whose current tool shelf contains `小蛇行动日志`
- furniture deep links still open the Action Log tool inside Serpent Desk, rather than bypassing the desk product structure
- Daily: 碳硅圈 / 日记 / blank Pet interior
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
feature/daily        DailyStore + Daily surfaces
feature/memory       MemoryStore + Memory surfaces
feature/shell        navigation, drawer, topbars and cross-feature coordination
core/local           SharedPreferences / in-memory persistence boundary
core/model           shared stable shell/chat models
```

`Serpent Desk` is the visible container; `Action Log` is a tool inside it. They do not duplicate state or UI. `CoastShellViewModel` still owns shell coordination and the focus ids used when furniture links directly into the log tool.

## PWA reference

The main Native body was reviewed against PWA app-60. The Serpent Desk home/tool-shelf behavior in version `0.1.13-app62-desk-fix-27` additionally follows the read-only current app-62 `settings.js`, where `settings:desk` opens `小蛇书桌 / Myri 的工作台` and currently exposes `小蛇行动日志` as one row.

Web/PWA source remains reference-only and is never copied into this repository.

## Hard boundaries

- no `android.permission.INTERNET`
- no real login/API/SSE/OpenRouter request/model call
- no base URL/token/password/provider secret
- no real Daily/Memory/MCP sync
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
