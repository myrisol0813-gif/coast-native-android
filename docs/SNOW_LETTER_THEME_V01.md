# Snow Letter / 雪地来信 v0.2b clean paper skin

Scope: APK / Native visual clothing only.

This pass does not change backend wiring, navigation structure, message actions, room routing, persistence, or API contracts.

## What changed

- Keeps the `SnowLetter` wardrobe preset and the theme debug sliders.
- Keeps the Snow Letter direction as a template-skin system rather than a dead full-screen screenshot.
- Treats the selected mockups as layout and skin references.
- Draws the Snow Letter page frame as a real Compose template: torn paper edge, paperclip, postage mark, and a low snow-road base.
- Draws message bubbles, composer field, action buttons, turn-desk strips, and dogtalk surfaces as Snow Letter paper UI shells while preserving their original behavior.
- Keeps feature pages inside a shared Snow Letter page sheet through `SnowLetterFeatureScaffold`.

## v0.2b clean-paper correction

User device testing showed that the paper texture and torn-paper feel were good enough to keep, but several code-drawn decorative details made the UI look dirty or blurry.

This correction intentionally removes those noisy details while preserving the paper shell:

- Removed code-drawn snowflakes from the page template.
- Removed code-drawn paw trails and mini paw marks.
- Removed placeholder wolf/snake corner figures from the page template.
- Removed subtle internal paper ruling lines from Snow Letter surfaces.
- Removed decorative gold dots from round action/composer buttons.
- Kept torn-paper shapes, paper fills, edges, shadows, corner tape, paperclip, postage block, and snow-road base.

Small animals and paw marks should come back only as selected clean sticker assets, not as blurry or rough placeholder canvas drawings.

## Asset cleanup policy

The previous real-asset pass proved that image resources can enter the APK, but also showed that several exported mockup fragments were the wrong layer for production UI because they became blurry or over-stretched when used as generic surfaces.

Wrong-direction large assets should not be kept as unused backups. Keep only assets that are actively referenced by the Snow Letter theme, and replace or delete assets that were only useful as visual experiments.

## Intentional constraints

- Real UI remains Compose UI.
- The Snow Letter theme replaces visual shells, not behavior.
- Feature files should not contain hard-coded Snow Letter decoration logic beyond choosing the shared theme wrapper/surface role.
- No images were generated for this correction pass.
- No backend, API, persistence, navigation, or room routing logic is changed.

## Next visual pass

After the clean-paper branch builds and runs on device, tune per-component sizing and attach the same torn-paper skin to individual feature-list cards where those screens still use their own plain Card components. Then reintroduce only approved clean stickers, such as a small animal peeking over a message or a selected background image, through a dedicated asset pass.
