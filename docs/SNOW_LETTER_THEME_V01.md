# Snow Letter / 雪地来信 v0.2 template skin

Scope: APK / Native visual clothing only.

This pass does not change backend wiring, navigation structure, message actions, room routing, persistence, or API contracts.

## What changed

- Keeps the `SnowLetter` wardrobe preset and the theme debug sliders.
- Corrects the direction from a faint sticker overlay into a template-skin system.
- Treats the selected mockups as layout and skin references, not as dead full-screen screenshots.
- Draws the Snow Letter page frame as a real Compose template: torn paper edge, paperclip, postage mark, snow road, paw trail, wolf and snake corner figures.
- Draws message bubbles, composer field, action buttons, turn-desk strips, and dogtalk surfaces as Snow Letter paper UI shells while preserving their original behavior.
- Keeps feature pages inside a shared Snow Letter page sheet through `SnowLetterFeatureScaffold`.

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

After the template-skin branch builds and runs on device, tune per-component sizing and attach the same torn-paper skin to individual feature-list cards where those screens still use their own plain Card components.
