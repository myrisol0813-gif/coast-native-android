# Snow Letter / 雪地来信 v0.1

Scope: APK / Native visual clothing only.

This pass does not change backend wiring, navigation structure, message actions, room routing, persistence, or API contracts.

## What changed

- Adds the `SnowLetter` wardrobe preset with a pale paper / snow / blue-gold palette.
- Adds `SnowLetterVisualSettings` and a `LocalSnowLetterVisuals` composition local for visual debugging.
- Adds a code-rendered `SnowLetterChatScaffold` for the main chat background.
- Adds `SnowLetterSkins` as the shared component clothing layer for Snow Letter surfaces.
- Adds Snow Letter debug sliders inside the theme wardrobe:
  - main chat background
  - paw prints and small animals
  - paper texture
- Wraps the main shell with Snow Letter visual settings.
- Lets the chat screen draw Snow Letter decorations only when the `SnowLetter` preset is active.
- Lets chat components keep their original behavior while wearing Snow Letter skins:
  - assistant message paper
  - user message paper
  - message action buttons
  - composer field and composer buttons
  - turn-desk strip and cards
  - dogtalk card and dogtalk fields

## Intentional constraints

- Real chat messages, input bars, buttons, drawers, and routes remain Compose UI.
- Full UI mockups are design references only, not page backgrounds.
- Component skins are owned by `SnowLetterSkins.kt`; feature files only choose which local component wears which role.
- PNG/WebP illustration assets are not required for this v0.1 pass; this version still uses lightweight Compose drawing so it can be reviewed safely before importing the curated sticker pack.
- Generated sticker assets can be imported later into `res/drawable-nodpi/snow_letter/` after visual selection.

## Next visual pass

After the component-skin branch builds and runs on device, the next pass can replace or augment the code-drawn wolf, snake, bottom snow road, and paper texture with the curated WebP sticker assets.