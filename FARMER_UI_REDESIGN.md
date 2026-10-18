# Farmer presentation redesign

The supplied reference guides the warm cream surfaces, forest-green accents,
photo-led Home, rounded cards, compact headers, selected navigation capsules,
and intentional deep-green dark theme. Existing local farm artwork is reused.
Real listing photos keep their original source and image-loading behavior.

## Presentation changes

- Home: greeting in the shared header; compact farm hero; four existing quick
  actions arranged in one or two rows according to width/text scale; existing
  metrics and incoming requests; recent cards from the orders already loaded
  by Home. Calendar and leaderboards remain lower on the dashboard.
- Requests: existing supplier identity, quantity, price and accept confirmation;
  Home cards also display the request's existing status and timestamp.
- Listings: larger photo treatment, wrapping status badges, accessible action
  targets, quieter search/filter surfaces, and balanced quantity/category text.
  The existing New Listing action has a measured bottom slot so scrolling
  listing actions remain unobscured; its route and keyboard visibility are unchanged.
- Create Listing: photo-led compact header, consistent form-card styling and
  a publish bar that can grow with text. Camera/gallery, five-photo limit,
  location, expiry, validation, draft and publish behavior are unchanged.
- Orders: compact controls and cards; delivered-order summary moved below the
  list. Existing range/search/status filters, values, progress and details
  actions remain available.
- Profile: compact avatar/name/role summary, existing account and location
  controls, settings before decorative artwork, and a green logout surface.
- Chat, notifications and location: Farmer-scoped card/bubble/composer styling,
  category/read-state surfaces, map borders and selected-address surfaces.

The shared `AnimatedBottomBar` architecture and Farmer tab order remain:
**Home, Listings, Orders, Profile**. Its label tracking and horizontal padding
adapt to larger text without increasing its existing font-aware height.

## Logic freeze

No ViewModels, repositories, use cases, models, Room files, Firebase files,
security rules, navigation graph, or routes were edited. Existing ViewModel
member/action references were compared against the original source and match.
Acceptance, stock/expiry, pickup PIN, lifecycle, scoring, chat subscriptions,
notification delivery and saved coordinates retain their original code paths.
The only added order selection is presentation sorting/taking the three most
recent records from Home's already-loaded order list; it makes no new data calls.

Shared style changes are guarded by `LocalFarmerPresentation` where needed,
so Supplier-specific rendering keeps its existing treatment.

## Verification scope

The final debug build, lint and full 328-test unit suite passed (zero failures).
The full device run completed with 86 passing cases and four permission-dependent
local-notification cases skipped. Coverage includes 90 targeted device cases:
Farmer roots/subscreens, acceptance guards, navigation, photo input, chat,
notification actions, location confirmation, keyboard and map layout.

Navigation checks cover 320/384 dp, light/dark, 1.0x/1.6x text and synthetic
24/48 dp bottom insets. Location layout checks include compact, medium and
landscape viewports with synthetic gesture/three-button/keyboard insets.
No device-wide navigation settings are changed for these checks. A subsequent
layout pass also checks large-text notification filters, Profile control wrapping
and separation between listing actions and the New Listing button. All three
final Farmer flow tests passed after this layout pass; the eight navigation
parameter cases and nine Farmer presentation/filter cases also passed.
The four skipped cases require Android notification permission on the device;
no device-wide permission or navigation setting was changed.

Screenshots are collected under `delivery/farmer-premium/` for visual review.
Role flows use isolated demo data and chat/photo tests use injected fixtures.
Live production acceptance, account mutation and physical camera capture are
not exercised by these UI checks.
