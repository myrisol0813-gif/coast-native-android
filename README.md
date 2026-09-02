# coast-native-android

Private Android native client for Elementera Coast / CoastGPT.

**Status: Native v1 local review shell aligned through current PWA app-60 surfaces and phone-review feedback. Real backend wiring is intentionally deferred.**

Current source of truth: `docs/NATIVE_APP60_SURFACE_PARITY_MAP.md`.

## Product identity

- App: `CoastGPT` / `com.elementeracoast.app`
- Gate: `Elementera Coast` · `沿海岸保存回声`
- Kotlin / Jetpack Compose only
- no WebView, React Native, TypeScript or committed Web/PWA assets

## Native body

- preserved Native 25 Gate, tuned drawer/topbar, `RoomType` and one shared `ChatWindow`
- Main / Radio / Lighthouse threads start empty; no fixture assistant message appears before user input
- chat top-right More opens the local `登岛信`
- island letter uses current-model `To ...` text and is stored independently per conversation + model; delivery remains unwired
- assistant actions: copy / like / favorite / local regenerate / delete / footprint
- user edit creates paired local user + assistant variants; fake generation targets the exact assistant variant
- local thought-soil / furniture transparency remains explicit
- Wolf Den: 个人资料 / 外观 / 聊天记录 / 模型箱 / 基本设置 / 关于与诊断
- redundant Profile avatar-location reminder removed
- Model Box shows the current model separately and puts all unselected models inside one grouped expandable catalog
- catalog groups mirror current PWA structure: o / GPT-4 / GPT-5 / other OpenAI Chat / Free Test / image
- Refresh is visible but explicitly offline until backend OpenRouter catalog wiring exists; no fake network result
- sidebar visible identity is again `Serpent Desk / 小蛇书桌`; its only current interior is the existing Action Log
- Daily: 碳硅圈 / 日记 / blank Pet interior
- Memory: 记忆库 / 种子库 / 世界书 / 自定义指令 in one 2×2 surface with real local filters
- Memory filter chips use a restrained accent tint / outline instead of muddy shadow-filled controls
- local JSON / HTML chat export and JSON import
- light / dark / gold plus local bubble/accent appearance

## State ownership

```text
feature/chat       LocalChatStore + message UI + LocalFurnitureOrchestrator
feature/wolf       WolfStore + ModelCatalog + ModelBoxScreen
feature/letters    IslandLetterContent + IslandLetterStore + IslandLetterScreen
feature/actionlog  ActionLogStore + ActionLogScreen
feature/daily      DailyStore + Daily surfaces
feature/memory     MemoryStore + Memory surfaces
feature/shell      navigation, drawer, topbars and cross-feature coordination
core/local         SharedPreferences / in-memory persistence boundary
core/model         shared stable shell/chat models
```

The physical-device product remains local-only. `CoastShellViewModel` coordinates shell/navigation/local generation; feature data remains owned by feature stores.

## Current PWA product mother

This review branch was rebuilt cleanly from Native 25 (`f3d8c2519fd64827ad8075c6a7df891ce62910a4`). Current read-only PWA reference: `elementera-coast@0b9ca38d50738d0c367637ccd06579a4f5feacfe` (`coast-app-60`).

The abandoned earlier Native 26 attempt is not a compatibility source and is not bridged into this code.

## Hard boundaries

- no `android.permission.INTERNET`
- no real login/API/SSE/OpenRouter request/model call
- no base URL/token/password/provider secret
- no real Daily/Memory/MCP sync
- no o3 reply-card changes
- no Calendar / Today Coast / Summary / Album
- no `FeatureDestination.Desk`; visible Serpent Desk is only the active Action Log shell
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
