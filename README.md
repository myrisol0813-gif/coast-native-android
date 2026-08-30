# coast-native-android

Private Android native client for Elementera Coast / CoastGPT.

**Status: Native v1 visual + scope body. Still a local/static shell; Coast backend wiring is intentionally not connected in this pass.**

Current build direction follows `docs/NATIVE_V1_BUILD_PLAN.md` plus the visual/scope addendum captured in `docs/NATIVE_V1_VISUAL_SCOPE_ADDENDUM.md`.

## Product identity

- App name: `CoastGPT`
- applicationId: `com.elementeracoast.app`
- Gate title: `Elementera Coast`
- Gate tagline: `沿海岸保存回声`
- Future backend boundary: `https://app.elementeracoast.com`

## Native v1 body now present

- Boot → Gate → MainShell
- original Coast Gate mark geometry migrated from the PWA inline SVG in `elementera-coast/functions/auth.js`
- Gate composition based on the current PWA: Coast mark, title/tagline, large rounded password shell, arrow action and `海岸信箱`
- light / dark / gold themes
- mobile drawer with Coast status cards, rooms, feature entries, scoped conversation list, Theme / Wolf Den / Serpent Desk
- one shared `ChatWindow` for Main / Radio / Lighthouse
- one shared `CoastTimeline`
- one shared `CoastComposer`
- one shared `ConversationList`
- one shared `ModelQuickPicker`
- one shared Dogtalk row
- `【电波】` and `【灯塔】` title prefixes for newly created scoped windows
- Daily / Memory / Calendar / Letters / Wolf / Desk feature landings using the current PWA card rhythm as visual reference
- local fake streaming remains only for UI/interaction verification

## Intentionally still placeholder / deferred

- Gate still enters the local shell; it does not POST the password to `/login` yet
- no Android `INTERNET` permission in this visual-only pass
- no session cookie implementation
- no `/api/chat/profile` GET/PUT yet
- no `/api/chat/conversations` / history / SSE wiring yet
- model selection changes local state only; real `profile.model_box` persistence is a later backend pass
- Radio/Lighthouse old APIs are not used; scope is represented by normal native conversation state + title prefix
- Dogtalk persistence is deferred
- conversation rename/delete is deferred
- Daily/Calendar/Memory/Letters/Wolf/Desk editors and deep data are deferred
- image, mic, empty-input call and other unfinished actions show an explanatory Snackbar rather than pretending to work

## Coast mark source

The Gate mark was **not traced or redrawn from screenshots**. `CoastBrandMark.kt` ports the actual SVG geometry embedded by the PWA in:

`elementera-coast/functions/auth.js`

The source includes the three rotated ellipses, black stroke, paper under-stroke, gold horns, cream wolf, gold face paths and original Coast color values. PWA high-resolution icon assets were also verified to exist under `elementera-mcp/deploy-pages/public/icons/`, but the Gate identity uses the original SVG geometry rather than a screenshot-derived imitation.

## MiniiChat attribution

The earlier PoC legally reused selected interaction ideas from MiniiChat by Minis233 under the MIT License. The remaining derived interaction areas are documented in `THIRD_PARTY_NOTICES.md` and `third_party/MiniiChat-LICENSE.txt`.

No GPL/AGPL implementation from RikkaHub, GPT Mobile, or other copyleft projects is included.

## Safety / repository boundaries

- no provider API keys
- no OpenAI/OpenRouter/etc provider endpoints
- no release signing configuration
- no keystore
- no committed APK/AAB
- CI builds a debug APK only to verify compilation and never uploads/releases it
- this repository does not modify the Elementera Coast PWA or backend

## Build

Requires JDK 17, Android SDK 34, and Gradle 8.9.

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

The CI also guards the three-scope shared ChatWindow contract and refuses committed signing/binary material.
