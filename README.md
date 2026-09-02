# coast-native-android

Private Android native client for Elementera Coast / CoastGPT.

**Status: Native v1 full local surface shell aligned to PWA app-59. Real backend wiring is intentionally deferred.**

Current source of truth: `docs/NATIVE_APP59_SURFACE_PARITY_MAP.md`.

## Product identity

- App: `CoastGPT` / `com.elementeracoast.app`
- Gate: `Elementera Coast` · `沿海岸保存回声`
- Kotlin / Jetpack Compose only
- no WebView, React Native, TypeScript or committed Web/PWA assets

## Native body

- preserved Native 25 Gate, tuned drawer/topbar, RoomType and one shared ChatWindow
- Main / Radio / Lighthouse still share timeline, message actions, Dogtalk and composer
- assistant actions: copy / like / favorite / local regenerate / delete / footprint / 1-1 variant
- user actions: copy / edit / delete / 1-1 variant
- local thought-soil / hand-seed sheet
- per-assistant explicit `FurnitureRun` bubble bound to local action ids
- Wolf Den: 个人资料 / 外观 / 聊天记录 / 模型箱 / 基本设置 / 关于与诊断
- Serpent Action Log: redacted persistent local action transparency
- Daily: 碳硅圈 / 日记 / 宠物系统 with local stores
- Memory: 记忆库 / 种子库 / 世界书 / 自定义指令 with local stores
- local JSON / HTML chat export and JSON import into the current window
- light / dark / gold plus local user-bubble and accent appearance

## State ownership

Feature state is not dumped into `MainShell` or one all-purpose ViewModel:

```text
feature/chat       LocalChatStore + LocalFurnitureOrchestrator
feature/wolf       WolfStore
feature/actionlog  ActionLogStore
feature/daily      DailyStore
feature/memory     MemoryStore
feature/shell      navigation and cross-feature coordination
core/local         SharedPreferences / in-memory persistence boundary
```

The physical-device product remains local-only. SharedPreferences stores Wolf/Daily/Memory/ActionLog state; conversations remain the Native chat owner and can be explicitly imported/exported.

## App-59 product mother

This pass was rebuilt cleanly from Native 25 (`f3d8c2519fd64827ad8075c6a7df891ce62910a4`) using the read-only PWA app-59 source at `elementera-coast@c4dd2ad6260bb9d8065a97ab57ddccfd78c697ef`.

The abandoned earlier Native 26 attempt is not a compatibility source and is not bridged into this code.

## Hard boundaries

- no `android.permission.INTERNET`
- no real login/API/SSE/model call
- no base URL/token/password/provider secret
- no real Daily/Memory/MCP sync
- no o3 reply-card changes
- no Calendar / Today Coast / Summary / Album
- no old Serpent Desk pseudo-settings
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
