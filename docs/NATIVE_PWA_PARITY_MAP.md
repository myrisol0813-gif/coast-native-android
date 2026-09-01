# Native ↔ PWA app-57 parity map

Reference: **Elementera Coast Web/PWA app-57**.

This document is the current structural/UI source of truth for the Native v1 shell after:

- `COAST-NATIVE-PARITY-CLEAN-24`
- `COAST-NATIVE-UI-PARITY-25`

The Android client remains Kotlin / Jetpack Compose and continues from the existing Native body. It is not a rewrite and it is not backend wiring.

## PWA app-57 files used as reference in UI parity 25

The supplied app-57 zip was unpacked only in the construction environment. It is not committed to this repository.

The Native UI pass inspected:

```text
index.html
public/app.js
public/features/chat/chat-render.js
public/features/chat/chat-actions.js
public/features/chat/chat-generation.js
public/features/chat/chat-stream.js
public/features/dogtalk.js
public/features/settings.js
public/styles/chat.css
public/styles/shell.css
public/styles/tokens.css
public/styles/features.css
docs/module-map.md
functions/auth.js
```

The supplied zip does not contain a separate `public/features/chat/chat-state.js`; the current split uses the files above instead.

No Web JavaScript/CSS was copied into Android. The PWA is a structure, interaction and visual-rhythm reference only.

## Gate identity source

The Gate mark is **not traced from a screenshot**.

Native continues to use `ui/brand/CoastBrandMark.kt`, a Compose Canvas/Path port of the actual inline hand-drawn Gate SVG in:

```text
Elementera Coast app-57 / functions/auth.js
```

That source contains the original two gold horn paths, three rotated ellipse loops, wolf silhouette/ears/cheeks/face paths and Coast colors. Entrance timing remains in `CoastBrandMarkAnimation.kt`.

This preserves the original mark geometry while avoiding a WebView or bundled PWA asset.

## Current Native chat file ownership

```text
feature/chat/
├─ ChatScreen.kt              # thin ChatWindow owner / clipboard / local dialogs
├─ ChatTimeline.kt            # LazyColumn, centering and auto-scroll
├─ MessageItem.kt             # role switch only
├─ AssistantMessage.kt        # avatar, assistant text, actions
├─ UserMessage.kt             # right bubble, user actions
├─ MessageActions.kt          # reusable action affordances
├─ GenerationFootprint.kt     # local model/source footprint
├─ VariantControl.kt          # 1/1 shell, future variant navigation
├─ AvatarPickerDialog.kt      # local-only Android Photo Picker entry
├─ EditMessageDialog.kt       # local user-message editor
├─ InputBar.kt                # shared Coast composer
└─ ModelPicker.kt             # local model picker
```

`ChatScreen.kt` does not own message drawing details, and the app still has exactly one shared `ChatWindow` for Main / Radio / Lighthouse.

## Room/window contract

Native mirrors PWA `conversation.room_type` with:

```text
main
radio
lighthouse
```

All three render through one `ChatWindow`.

Title normalization remains:

- Main: no prefix
- Radio: exactly one `【电波】`
- Lighthouse: exactly one `【灯塔】`

New conversation inherits `activeRoomType`.

Rename removes accidental special-room prefixes before applying the target room's own prefix.

### Delete-current fallback

If conversations remain after deletion:

1. prefer another conversation of the same room type;
2. otherwise prefer Main;
3. otherwise use the first remaining conversation.

Deleting a non-active conversation does not change the active id.

### Delete the global last conversation

The delete is allowed to complete.

After deleting the final conversation and removing its old thread:

- create a **fresh** Main fallback;
- use a new conversation id;
- title it `新聊天 1`;
- give it an empty message list;
- do not restore the deleted thread/greeting;
- show `已清空最后一个窗口`.

This fixes the previous UX where deleting a last `新聊天 1` could immediately create a visually identical replacement with content and therefore look undeletable.

## Local message UI model

`core/model/ChatMessage` is Native local UI state, not an API schema. It can carry:

- `modelId`
- `generationSource`
- `liked`
- `favorite`
- `errorDetail`
- `variantIndex`
- `variantCount`
- optional display time label

Future transport types remain separately under `core/contract/` (`CoastMessage`, `AssistantVariant`, etc.).

## Message actions in UI parity 25

Assistant message row now exposes:

- copy
- like
- regenerate
- favorite
- delete
- variant shell
- generation footprint

User message row now exposes:

- copy
- edit
- delete
- variant shell

### Local-only behaviors that really work

- Copy writes the selected text to the Android clipboard.
- Like toggles only the selected assistant message in the active local thread.
- Favorite toggles only the selected assistant message in the active local thread.
- Edit changes only the selected user message in the active local thread.
- Delete removes only the selected message from the active local thread.
- Regenerate clears the selected assistant response and runs the existing local fake streaming into that same message id.
- Assistant avatar can be changed with Android Photo Picker for the local Native session and reset to default.

None of these actions calls Coast history/profile/chat APIs.

### Still placeholders

- variant navigation remains `1/1` until real history variants are wired;
- generation footprint is local model/source metadata rather than server usage truth;
- avatar profile sync is explicitly deferred;
- attachment, mic and empty-input call behavior still explains that it is not wired.

## Chat visual parity

The Native chat spacing now follows the supplied PWA chat proportions rather than default Compose demo spacing:

- 52dp top bar rhythm;
- centered timeline with a 760dp upper width bound;
- 24dp mobile horizontal message padding;
- 16sp assistant/user text with looser assistant line-height;
- 34dp clickable assistant avatar;
- user bubble capped at roughly 86% width with 20dp radius;
- compact 34dp message actions;
- 28dp variant circles;
- small right-aligned generation footprint;
- subtle Dogtalk-to-composer separation;
- shared three-part Coast composer remains add / input+mic / call-send-stop.

Light, dark and gold themes continue to render through Material color roles rather than hard-coded white-only chat colors.

## Top bar and drawer

Top bar preserves the PWA `ChatGPT + model` semantic. Radio/Lighthouse add a small room identity line instead of becoming separate pages.

Drawer remains the application house map:

- Main
- Radio
- Lighthouse
- Memory
- Daily
- conversation windows
- Theme
- Wolf Den
- Serpent Desk

Calendar / Today Coast / Summary / Album are not restored.

## Daily and Memory light shells

Daily v1 remains:

1. 碳硅圈
2. 日记
3. 宠物系统

Memory v1 remains:

1. 记忆库
2. 种子库
3. 世界书
4. 自定义指令

They remain local placeholder landings in this UI pass.

## Dogtalk invariant

The existing four logical fields remain unchanged:

```text
body
true_core
weather
read_mode
```

Dogtalk is still a shared local card for Main / Radio / Lighthouse and is not persisted to the backend.

## Backend boundary remains closed

`core/network/CoastGatewayClient.kt` is still an interface-only future boundary.

UI parity 25 does **not** add:

- a real base URL;
- credentials/tokens/passwords;
- an HTTP implementation;
- real authentication;
- real conversations/history/profile/chat calls;
- real SSE;
- Android `INTERNET` permission;
- DataStore/Room just for the UI actions.

A later `COAST-NATIVE-BACKEND-WIRING` pass owns transport.

## Repository boundaries

This pass changes only `coast-native-android`.

It does not modify or commit:

- the Web/PWA repository;
- the supplied app-57 zip;
- o3 reply-card;
- MCP `tools/list`;
- server/D1 code;
- TypeScript / React Native / WebView code.
