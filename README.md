# coast-native-android

Private Android native client for Elementera Coast / CoastGPT.

**Status: Native v1 local UI shell aligned to Elementera Coast Web/PWA app-57. Real backend wiring is intentionally deferred.**

Current source of truth: `docs/NATIVE_PWA_PARITY_MAP.md`.

## Product identity

- App name: `CoastGPT`
- applicationId: `com.elementeracoast.app`
- Gate title: `Elementera Coast`
- Gate tagline: `沿海岸保存回声`
- Kotlin / Jetpack Compose native client
- no WebView, React Native or TypeScript layer

## Native v1 body

- Gate → MainShell
- Gate brand geometry ported from the real inline hand-drawn SVG in the supplied app-57 `functions/auth.js`, not traced from screenshots
- light / dark / gold Coast themes
- drawer with room entrances, conversations, Memory / Daily, Theme / Wolf Den / Serpent Desk
- PWA-aligned `RoomType`: `main` / `radio` / `lighthouse`
- one shared `ChatWindow` for all three room types
- split chat UI: timeline, role renderer, assistant/user messages, actions, variants, footprint and local dialogs
- assistant actions: copy / like / regenerate / favorite / delete
- user actions: copy / edit / delete
- local `1/1` variant shell and local model/source generation footprint
- assistant avatar button with Android Photo Picker local-only replacement/reset
- PWA-like timeline spacing, 34dp assistant avatar, right-aligned user bubble and compact action row
- shared Dogtalk four-field row
- shared add / input+mic / call-send-stop composer
- Daily light shell: 碳硅圈 / 日记 / 宠物系统
- Memory light shell: 记忆库 / 种子库 / 世界书 / 自定义指令

## Local-only behavior

The current APK really performs these actions in the in-memory Native thread:

- copy to Android clipboard
- toggle assistant like/favorite
- edit one user message
- delete one selected message
- fake-stream regenerate one assistant message
- change/reset the local assistant avatar

None of these actions calls a server.

Deleting the global last conversation now removes its old thread and creates a fresh empty Main `新聊天 1` with a new id, so the delete no longer looks like the same window silently came back.

## Still deliberately not wired

- no Android `INTERNET` permission
- no real authentication/session
- no real base URL or HTTP implementation
- no profile/conversation/history/chat request
- no real SSE
- no Radio/Lighthouse server behavior
- no Daily/Memory backend persistence
- no Dogtalk backend persistence
- no avatar profile upload/sync
- no Mailbox/MCP wiring
- no provider keys, tokens, passwords or signing secrets

`core/network/CoastGatewayClient.kt` remains an interface-only future boundary. A later `COAST-NATIVE-BACKEND-WIRING` pass owns transport.

## Retired structures stay retired

The Native surface does not restore Calendar / 今日一瞥 / Daily Summary / Album. `ChatScope` remains retired in favor of `RoomType`.

## Dogtalk invariant

```text
body
true_core
weather
read_mode
```

## MiniiChat attribution

The earlier PoC legally reused selected interaction ideas from MiniiChat by Minis233 under the MIT License. Remaining derived interaction areas are documented in `THIRD_PARTY_NOTICES.md` and `third_party/MiniiChat-LICENSE.txt`.

No GPL/AGPL implementation from RikkaHub, GPT Mobile, or other copyleft projects is included.

## Build

Requires JDK 17, Android SDK 34, and Gradle 8.9.

```bash
gradle :app:testDebugUnitTest --no-daemon --stacktrace
gradle :app:assembleDebug --no-daemon --stacktrace
```

GitHub Actions runs the tests and debug assembly and stages the current debug APK artifact.
