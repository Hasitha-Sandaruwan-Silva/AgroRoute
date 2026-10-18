# Official AgroRoute branding

The approved source is `app/src/main/res/drawable/logo.png`. Its file bytes,
leaf/road/gold-arrow identity, proportions and full-color palette are unchanged.
No generated or traced substitute is used. Existing Farmer UI work is retained.

## Assets

- Adaptive icon: original full-color mark on the existing forest `#123D32`
  background; every visible pixel stays within the 66dp circular safe area on
  the 108dp layer. Arrow and stem remain inside all three preview masks.
- Monochrome: the same alpha silhouette in white, for Android themed icons.
- Legacy: density-specific square/round assets from the same original.
- Native splash: the original logo on a transparent registered canvas, using
  the existing Android SplashScreen integration and light/dark backgrounds.
- Animation: exact-pixel source layers isolate the existing leaf, road markings
  and gold arrow. Only margins, uniform resizing and layer extraction are used.

Regenerate with `tools/branding/Generate-StartupBranding.ps1` (Python, Pillow,
numpy). `Preview-StartupBranding.ps1` also refreshes the mask contact sheet.
Generated review artifacts are in `delivery/official-branding/`.
`source.json` records the original hash and pixel-exact layer recomposition.

## Startup presentation

The existing 2400ms timeline and 200ms exit remain, approximately 2.6 seconds
when the current session resolution is ready. Native and Compose use the same
logo canvas and center at handoff. A restrained scale/opacity reveal leads to
an upward settle, a light sweep on the existing road pixels (400?1100ms), tiny
arrow movement settling at 1500ms, the name/subtitle (1200?1900ms), and a quiet
route accent (1800?2400ms). There is no spin, bounce, or blank delay.

The real app remains composed underneath throughout. The existing session-ready
gate, loading state, save/restore behavior, navigation and destination selection
are unchanged. Firebase, auth, Demo Mode, ViewModels, repositories, Room,
notifications and business features are untouched by this branding change.

Light and dark native/Compose surfaces match. The legacy native navigation-bar
color now uses the same startup background as the composition.

## Validation

Pending final build/device results. Mask previews cover circle, squircle and
rounded square on light/dark backgrounds. Device checks cover underlying content
lifetime, session wait, saved-state restoration, no replay, light/dark frames,
compact/medium 1.0x/1.6x text, and the actual device launcher drawable mask.

Sizing follows Android's official [adaptive icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
and [splash screen guidance](https://developer.android.com/develop/ui/views/launch/splash-screen).
