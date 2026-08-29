# coast-native-android

Private Android native client for Elementera Coast / CoastGPT.

**Status: MiniiChat-derived native shell PoC. Not the final CoastGPT UI and not connected to the Coast backend yet.**

The first native prototype was stopped after proving that native Android interaction and Coast backend integration were viable while its UI direction was not. First prototype reference commit: `7931202837dccb59b2f8d01d8c4866703266e62a`.

This PoC tests a narrower second approach: legally reuse selected MIT-licensed interaction ideas and Compose shell structure from `Minis233/miniichat`, remove its generic BYOK/provider product model, and verify that the remaining native shell can feel like a normal chat application before the PWA reference pack defines the final Elementera Coast visual language.

## Product identity

- App name: `CoastGPT`
- applicationId: `com.elementeracoast.app`
- Login title: `Elementera Coast`
- Future backend: `https://app.elementeracoast.com`

## What this PoC contains

- placeholder local login screen
- native chat message list
- right-aligned, width-limited user bubbles
- naturally flowing assistant text
- multiline bottom input bar
- send/stop button sharing the same location
- local fake streaming with one generation Job and explicit cancel owner
- fake model list with searchable bottom-sheet picker
- rough deep-sea / old-gold placeholder theme
- a compile-only `CoastGatewayClient` future contract with no implementation

## What this PoC intentionally does not contain

- no real network requests
- no Android `INTERNET` permission
- no provider API keys
- no OpenAI/OpenRouter/DeepSeek/Groq/Mistral/Ollama presets
- no provider settings or custom provider headers/body params
- no assistants or prompt-variable system
- no media attachments
- no universal `/chat/completions` transport
- no release signing configuration
- no keystore
- no release or packaged binary in the repository

## MiniiChat attribution

Selected shell interaction patterns and selected Compose UI structures are derived from MiniiChat by Minis233 under the MIT License. See `THIRD_PARTY_NOTICES.md` and `third_party/MiniiChat-LICENSE.txt`.

Derived/reworked areas in this PoC:

- `feature/chat/ChatScreen.kt` — message-flow structure and user/assistant presentation concept
- `feature/chat/InputBar.kt` — multiline composer and send/stop-in-one-place interaction
- `feature/chat/ModelPicker.kt` — search, selected state, and bottom-sheet interaction
- `feature/shell/CoastShellViewModel.kt` — single generation Job / cancel owner / transient streaming-state pattern

MiniiChat's provider/API-key/assistant/media/network/signing implementation was not brought into this repository.

No GPL/AGPL implementation from RikkaHub, GPT Mobile, or other copyleft projects is included.

## Build

Requires JDK 17, Android SDK 34, and Gradle 8.9.

```bash
gradle :app:assembleDebug
```

CI compiles the debug APK only to verify the shell and does not upload or release the APK.

## Next step

If this shell proves structurally sound, prepare `COAST-NATIVE-REFERENCE-PACK-00` before treating the UI as product design. The reference pack remains the visual source of truth for screenshots, spacing, colors, three themes, icons, and Coast backend contract notes.

Do not treat this PoC's placeholder theme as the final Elementera Coast design. Do not reconnect generic provider logic.
