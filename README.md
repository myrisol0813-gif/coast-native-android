# coast-native-android

Private native Android client for Elementera Coast / CoastGPT.

## Current status

Native 30 is under Draft validation. The Android body is no longer a local prototype shell: authentication, conversations, history, chat streaming, chat profile, Daily data and thought soil use the real Coast backend.

The architectural rule is:

```text
PWA ─┐
     ├── Coast canonical backend
Native ┘
```

Shared Coast content has one canonical server owner. Native caches may retain the latest successful snapshot for continuity, but they are not a second authoritative database and there is no local↔remote merge engine.

## Product identity

- App: `CoastGPT`
- application id: `com.elementeracoast.app`
- Kotlin + Jetpack Compose
- JDK 17 / Android SDK 34 / Gradle 8.9
- production Coast origin is centralized in `BuildConfig.COAST_API_BASE_URL`
- no WebView, React Native, TypeScript or embedded PWA source

## Real backend wiring

Current canonical paths include:

- Coast password/session restore through the real backend cookie contract
- conversation list/create/rename/delete
- shared PWA ↔ Native chat history
- real `/api/chat` SSE generation
- canonical chat profile and model catalog
- per-conversation thought soil
- Daily moments, likes, comments and real Myri instant comments
- Daily diaries including date/tags edits
- Xiaohan Daily avatar and moment cover
- Myri avatar through the canonical chat-profile `assistant_avatar_dataurl`

The current shared-state acceptance pass also carries support for the canonical assistant-variant `desk_slip` history field. Once the matching Coast/PWA backend change is promoted, the current window's `本轮桌面` can survive switching away and back in both clients.

## Native state ownership

Server-owned feature repositories currently include:

```text
core/auth                  session owner
core/network               one Coast HTTP/SSE transport
core/remote                wire DTOs + remote snapshot cache
feature/shell              conversations + chat profile composition
feature/chat               canonical history/generation mapping + UI
feature/daily              canonical Daily repository + UI
feature/memory             canonical thought-soil repository
```

Device-local state remains appropriate for this body's own appearance/preferences, such as Native theme/accent/bubble presentation.

Some older feature surfaces are still local prototypes and are scheduled for later single-source checkpoints rather than being silently treated as synchronized data. In particular, do not assume every Memory/Worldbook/Custom Instructions, Action Log or Island Letter management surface has completed canonical migration until its owning repository is explicitly converted.

## Current Native 30 acceptance behavior

- Myri avatar is owned by the chat profile, not a duplicate Daily profile field.
- Xiaohan avatar and Carbon Circle cover remain Daily profile fields.
- posting a Xiaohan moment triggers the real instant Myri-comment flow after the moment is saved; manual retry remains available if that second step fails.
- furniture/tool result bubble is a timeline attachment above thought soil and the assistant reply.
- in-app Refresh re-reads canonical Coast state through existing repositories instead of restarting the Activity.
- launcher uses the Coast navy/gold horned GPT mark.

## Stable signing

Gradle and GitHub Actions already support a persistent signing key. No keystore or password belongs in source control.

When all four repository secrets exist, CI decodes the keystore only inside the runner and signs both debug acceptance builds and release builds with the stable key:

```text
COAST_ANDROID_KEYSTORE_B64
COAST_ANDROID_KEYSTORE_PASSWORD
COAST_ANDROID_KEY_ALIAS
COAST_ANDROID_KEY_PASSWORD
```

Until those secrets are configured, CI intentionally reports `stable_signing=false` and falls back to the runner debug key. Different runner keys may prevent overwrite-install even when `versionCode` increases.

## Build

```bash
gradle :app:testDebugUnitTest --no-daemon --stacktrace
gradle :app:assembleDebug --no-daemon --stacktrace
```

GitHub Actions additionally enforces structural/single-source guards, verifies the package identity, records signing mode, stages the APK and publishes SHA-256 metadata.

## Hard boundaries

- no provider/API/session secrets committed to the APK repository
- no logging of Cookie or Authorization values
- no second Daily HTTP client
- no local authoritative clone of canonical Daily data
- no local↔remote sync/merge engine
- no fake Myri reply or fake tool run to make an unwired feature look complete
- no `legacy` / `compat` / `bridge` / `temp` / `misc` source layer
- no theme/wardrobe work mixed into backend wiring checkpoints
- no merge of Draft Native checkpoints without explicit device acceptance / approval

## Attribution

Selected earlier PoC interaction patterns from MiniiChat remain attributed under MIT in `THIRD_PARTY_NOTICES.md` and `third_party/MiniiChat-LICENSE.txt`. No GPL/AGPL implementation is included.
