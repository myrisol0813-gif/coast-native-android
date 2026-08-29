# coast-native-android

Private Android native client for Elementera Coast / CoastGPT.

**Status: reset before second prototype.**

The first native prototype proved that a native Android client can connect to the Coast backend and feel smoother than the PWA, but the UI direction was discarded. API integration was useful; the UI as a whole should not be patched further.

First prototype reference commit: `7931202837dccb59b2f8d01d8c4866703266e62a`.

A later UI rebase attempt was also discarded as part of this reset. The next attempt should begin only after preparing a PWA reference pack with screenshots, colors, spacing, assets, and API contract notes.

**Do not continue patching the first prototype. Do not begin the second Android UI before the reference pack exists.**

## Next step

Prepare `COAST-NATIVE-REFERENCE-PACK-00` first:

- PWA login screenshot
- PWA main chat screenshot
- PWA model selector screenshot
- screenshots of all three themes
- input bar screenshot
- icon / favicon / manifest icons
- current CSS color tokens
- current chat API contract
- current models API contract
- current login API contract
- UI elements that must closely match the PWA
- areas that may be adapted to native Android behavior

Think of the reference pack as the sample room for the next construction pass: this is the door, this is the wall color, this is the light, this is the input bar, this is the model footprint, this is the chat shape, these are the three themes, and these backend contracts are not to be touched.
