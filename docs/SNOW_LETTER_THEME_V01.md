# Snow Letter / 雪地来信 v0.1

Scope: APK / Native visual clothing only.

This pass does not change backend wiring, navigation structure, message actions, room routing, persistence, or API contracts.

## What changed

- Adds the `SnowLetter` wardrobe preset with a pale paper / snow / blue-gold palette.
- Adds `SnowLetterVisualSettings` and a `LocalSnowLetterVisuals` composition local for visual debugging.
- Adds a real drawable-backed `SnowLetterChatScaffold` for the main chat background, bottom snow road, wolf sticker, and snake sticker.
- Adds `SnowLetterFeatureScaffold` so Native feature pages can share light Snow Letter wallpaper and corner stickers without changing their routes or behavior.
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

## Real image assets

This pass imports curated, APK-friendly WebP resources into `app/src/main/res/drawable-nodpi/`:

- `snow_letter_chat_bg.webp`
- `snow_letter_bottom_strip.webp`
- `snow_letter_paper_card.webp`
- `snow_letter_snake_paper.webp`
- `snow_letter_wolf_write.webp`

No new images were generated for this pass; the imported files are compressed app resources derived from the selected Snow Letter asset set.

## Intentional constraints

- Real chat messages, input bars, buttons, drawers, and routes remain Compose UI.
- Full UI mockups are design references only, not page backgrounds.
- Component skins are owned by `SnowLetterSkins.kt`; feature files only choose which local component wears which role.
- Drawable assets are owned by the theme layer and are referenced through Android resources, not embedded as Kotlin strings or ad-hoc base64 constants.
- Code-drawn snow texture remains only as a light overlay and fallback, not the main animal/sticker artwork.

## Next visual pass

After the drawable-backed branch builds and runs on device, the next pass can tune asset opacity, swap in larger image resources where useful, and continue replacing small icon shells with more specific Snow Letter stamps.
