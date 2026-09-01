# coast-native-android

Private Android native client for Elementera Coast / CoastGPT.

**Status: Native v1 local shell aligned to Elementera Coast Web/PWA app-57. Real backend wiring is intentionally deferred.**

Current structural source of truth: `docs/NATIVE_PWA_PARITY_MAP.md`.
Older Native planning documents are historical references where they conflict with the app-57 parity map.

## Product identity

- App name: `CoastGPT`
- applicationId: `com.elementeracoast.app`
- Gate title: `Elementera Coast`
- Gate tagline: `沿海岸保存回声`
- Kotlin / Jetpack Compose native client; no WebView, React Native or TypeScript layer

## Native v1 body now present

- Gate → MainShell
- light / dark / gold Coast themes
- Coast drawer with status strip, room entrances, conversation list, Memory / Daily, Theme / Wolf Den / Serpent Desk
- PWA-aligned `RoomType`: `main` / `radio` / `lighthouse`
- one shared `ChatWindow` for Main / Radio / Lighthouse
- one shared timeline, composer, model picker and Dogtalk card
- `【电波】` / `【灯塔】` title-prefix normalization for create and rename
- delete-current fallback: same room type → Main → create local Main fallback
- Daily local light shell: 碳硅圈 / 日记 / 宠物系统
- Memory local light shell: 记忆库 / 种子库 / 世界书 / 自定义指令
- Kotlin typed contract skeletons for conversations/messages/variants/profile/Daily/Memory/Dogtalk
- local mock threads and local fake streaming retained for UI verification

## Retired Native residue removed

The current app surface no longer carries the pre-app57 Calendar / Today Coast / Daily Summary / Album structure.
`ChatScope` has also been retired in favor of the PWA `room_type` concept.

See `docs/NATIVE_PWA_PARITY_MAP.md` for the exact mapping and fallback rules.

## Deliberately not wired yet

- Gate still enters the local shell; it does not perform real authentication
- no Android `INTERNET` permission
- no real base URL committed into the Native client
- no session/cookie implementation
- no real profile, conversation, history or chat request
- no real SSE
- no Radio/Lighthouse server behavior
- no Daily or Memory persistence
- no Dogtalk persistence/backend call
- no Mailbox/MCP wiring
- no provider keys, tokens, passwords or signing secrets

`core/network/CoastGatewayClient.kt` is an interface-only future boundary. A later `COAST-NATIVE-BACKEND-WIRING` pass should own real transport decisions.

## Dogtalk invariant

Dogtalk keeps the current four-field local contract:

```text
body
true_core
weather
read_mode
```

No legacy Dogtalk fields are restored by this pass.

## MiniiChat attribution

The earlier PoC legally reused selected interaction ideas from MiniiChat by Minis233 under the MIT License. Remaining derived interaction areas are documented in `THIRD_PARTY_NOTICES.md` and `third_party/MiniiChat-LICENSE.txt`.

No GPL/AGPL implementation from RikkaHub, GPT Mobile, or other copyleft projects is included.

## Build

Requires JDK 17, Android SDK 34, and Gradle 8.9.

```bash
gradle :app:testDebugUnitTest --no-daemon --stacktrace
gradle :app:assembleDebug --no-daemon --stacktrace
```

GitHub Actions runs both checks and stages the resulting debug build as the `CoastGPT-native-debug` artifact.
