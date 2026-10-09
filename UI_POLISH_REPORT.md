# AgroRoute UI polish and photo-upload audit

## Scope and existing implementation

The project uses Jetpack Compose Material 3, a shared Material/extended color scheme, system typography with Sinhala fallback, Coil image loading, Hilt ViewModels, Firebase Auth/Firestore, Room v7 and MapLibre/MapTiler. There is no need for an additional icon dependency or a duplicate listing editor.

Screen inventory: welcome/onboarding; login/register/Google role selection; Farmer and Supplier dashboards and shells; marketplace/search/filters; farmer listings and the shared create/edit listing form; farmer bid list and listing bid details; supplier bid history and place-bid form; place-order-request and farmer request list; both order lists/details; both profiles and shared edit-profile dialog; calendar; leaderboard; review form; notification inbox/settings and general settings/help/about/privacy; location picker. Loading, empty, error and status components already exist and now inherit the revised shared tokens/icons. No new route or business workflow was introduced.

The audit found: heavily green page/card/border colors with weak visual separation; amber/status tokens also mapped to green; strong shadows and oversized corner radii across farmer screens; different role navigation implementations with active-only labels; white location-picker surfaces in dark mode; low-contrast points text on profile and leaderboard cards; listing edit/close/bidding callbacks not rendered by the farmer listing card; raw original avatar uploads with no bounded decoding and no dedicated upload state/preview/retry.

## Design and components

Light: page #F6F7F2, white cards, primary #286443, text #202D25, secondary text #59665C, border #DAE1D8. Deep green actions have readable white text. Mint is reserved for subtle containers, with amber #F7E9CC / #735119 for pending/warning status.

Dark: page #141B18, card #1E2822, raised surface #2A372F, primary #A0CBAA, text #E8EEE7, secondary text #B5C1B6, card border #39483E. Muted green and charcoal replace saturated teal surfaces. Amber warning containers are #443822 / #E5C88F. Error tones remain distinct reds. Calculated token contrast ratios: light action 7.01:1, light body 13.33:1, light secondary 6.03:1; dark action 6.84:1, dark body 12.89:1, dark secondary 8.15:1. These checks cover the main solid color pairs, not every image/gradient overlay. White text remains on the dark brand hero gradients.

Refined theme colors (including all Material surface-container roles so switches do not inherit lavender defaults), extended semantic status tokens, shape/spacing tokens, shared text-field surfaces, profile avatar, photo pickers, location-picker surfaces, and shared bottom navigation. Both role bars now use the shared equal-width icon-and-label layout with persistent labels, 48dp minimum targets and a clear selected container. Shared AppCard consumers inherit the smaller radius and 16dp padding. Existing order/bid/dashboard/welcome cards use subtler shadows and restrained corners. System typography/Sinhala fallback and Coil cache remain.

Icons use the installed Compose Material Rounded family through the existing Lucide compatibility facade and direct imports. Camera overlay uses the same family. No new icon library or APK dependency was added.

## Listing edit verification and fixes

Farmer-owned listing cards now render Edit listing, Close (for open listings), and Allow bidding controls using their existing callbacks. Closing retains the existing confirmation dialog. The existing route opens CreateListingScreen(editListingId), preloads name, quantity/unit, price, description, image URL, location, category, expected date, quality grade and harvest status. Save changes calls UpdateListingUseCase with the same listing ID, owner and creation date; it does not create a duplicate. Existing navigation pops back after success. Existing validation, role/ownership checks, accepted-bid checks and order-history restrictions remain intact.

**Existing restriction preserved:** listings with order history cannot be edited. This task does not relax that rule to enable location changes. Editable listings can update their location; existing order snapshots are never written by the edit flow.

## Profile upload failure: evidence and exact fix

Confirmed code defect: with the profile Cloudinary preset blank, ProfilePhotoStorage passes the original URI to Firebase Storage; storage.rules requires avatars strictly smaller than 5 MB. Large camera originals can therefore be rejected. The original code has no size preparation, preview, upload-specific state or retry. This is a concrete failure path, but the user's particular reported exception cannot be conclusively attributed to it without reproducing that upload. The connected device's retained StorageException/ProfilePhoto error tags contained no matching exception during inspection. Storage billing/bucket configuration and deployed rules are not inferred from local source.

The fix keeps Firebase as the existing default provider and the same profile_images/{uid}/avatar path. It bounds bitmap decoding to at most 1024px per dimension with BitmapFactory sampling, applies EXIF rotation/reflection, compresses JPEG at quality 88 off the main thread, checks the result is below the existing 5 MB rule, and deletes the cache file after upload. If a profile Cloudinary preset is configured, the existing optional provider is retained. Storage URI MIME resolution supports generated file:// JPEGs. Listing-image upload/provider behavior remains intact apart from the additive file-extension MIME fallback.

Both profile pages show the selected local image immediately, a busy overlay/text, updated remote photo on success, and retained preview plus Retry photo upload on failure. Separate photoUploading state prevents repository profile emissions from prematurely ending upload progress. Cancellation propagates and original upload failures are logged under ProfilePhoto. Existing AuthRepository.updateProfile writes the download URL to Firestore editable fields and Room; it preserves role/points and persistence across restart. Photo selection uses Android PickVisualMedia with its platform fallback, without adding storage permissions. Profile editor content scrolls for keyboard/small-screen use.

## Business behavior preserved

Authentication and Farmer/Supplier roles; Firestore collections, field names and security rules; Room entities/schema/migrations; all repositories and listing/request/bid/order use cases; existing validation; stock/state transitions; order pickup/delivery snapshots; MapLibre/MapTiler; Android Geocoder, GPS, map tap/pin, navigation, manual address and location encoding; routes; existing listing image provider and Coil loading/cache. AuthViewModel changes are limited to the defective photo upload UX/error lifecycle and coordination with profile saving. No backend rules were loosened and no migrations were removed.

## Verification

- Firestore emulator: **97 tests passed**, 0 failed. Includes ownership, accepted-bid/request/order state and immutable pickup/delivery snapshot regressions.
- Room migration validator: **passed**; actual 6-to-7 SQL preserves rows in all seven tables, matches generated v7 schema/defaults/indexes, and leaves old order locations unchanged.
- Android debug build: **assembleDebug passed**. Existing unit tests: **157 passed**, 0 failures/errors/skipped. Final rebuild after all changes: **BUILD SUCCESSFUL**, 157 tests passed.
- Connected physical device: Final APK install with data preserved **passed**, cold launch **passed** (3283 ms). Inspected light leaderboard/profile and dark marketplace/navigation screenshots. Device checks are a visual smoke test, not full role or photo-upload verification.
- No production data was changed to exercise uploads or orders.

## Exact manual device checklist

1. Install the debug APK over the existing debug app, preserving app data. Open welcome/login/register; check keyboard and scrolling at normal and enlarged font sizes. Sign in as each role and check all navigation destinations and logout.
2. In each role, select Light, Dark and System settings. Check dashboard, listing/marketplace, bids/requests, orders/details, notifications, settings and profile: readable text, subtle borders, distinguishable cards/statuses, persistent bottom labels and no clipped controls at 320dp width and large fonts.
3. Farmer: create a listing with image, category, price, quantity and pinned location. Open its visible Edit listing action; check every existing field/image is prefilled. Change price/description/location; save; confirm the same listing ID/document is updated, no duplicate appears and back navigation works. Repeat with an existing image unchanged and a replacement image. Check existing validation errors and order-history/accepted-bid restrictions. Verify bidding toggle and Close confirmation/cancellation.
4. Supplier: search/filter marketplace, place a bid and a fixed-price request using the existing delivery pin flow. Farmer: accept/reject requests/bids. Check stock, totals, permissions and existing order status actions. Check pickup/delivery addresses/pins and navigation on each role's order details. Change an editable listing/profile location and confirm previously saved order snapshots remain unchanged.
5. Location picker in both themes: map tap/pin, address search, manual address, GPS permission allow/deny, blue current-location dot, Confirm action, saved coordinate restoration and keyboard scrolling. Check unavailable GPS/search error feedback.
6. Profile photo on each role: tap camera, cancel picker, select JPEG/PNG/WebP and a large rotated camera image (>5 MB). Confirm preview, busy overlay, single in-flight upload, success feedback and new photo. Restart/sign out and back in; confirm the URL persists in Firestore/Room and avatar reloads. Check EXIF orientation and cloud-backed picker documents. Confirm no storage-permission prompt.
7. Turn network off before selecting a photo; check error and retry controls without clearing the preview. Restore connectivity and retry. Test corrupt/unsupported image, revoked document access and server permission denial; inspect ProfilePhoto logs for the original cause. Verify previous stored profile URL remains on failed saves.
8. Empty/loading/error/disabled states: no listings/orders, slow network, failed refresh and double taps on saving/uploading. Check scrolling with keyboard and 1.5x/2x font scale. Check no UI freeze when preparing large photos.

## Local limits

Live profile upload, deployed Firebase Storage rules/bucket availability, persisted restart behavior and full role workflows need an authenticated device session and a selected image. Bitmap/EXIF preparation and picker behavior need Android runtime checks. Firestore emulator tests do not exercise Firebase Storage or Cloudinary. Successful source compilation is not evidence of complete visual or end-to-end device verification.

## Changed files

- `app/src/main/java/com/agroroute/core/components/AgroTextField.kt`
- `app/src/main/java/com/agroroute/core/components/AnimatedBottomBar.kt`
- `app/src/main/java/com/agroroute/core/components/LocationPickerDialog.kt`
- `app/src/main/java/com/agroroute/core/components/PhotoPicker.kt`
- `app/src/main/java/com/agroroute/core/components/ProfilePhoneContact.kt`
- `app/src/main/java/com/agroroute/core/components/UserProfileAvatar.kt`
- `app/src/main/java/com/agroroute/core/remote/CloudinaryPhotoStorage.kt`
- `app/src/main/java/com/agroroute/core/remote/ProfilePhotoStorage.kt`
- `app/src/main/java/com/agroroute/core/remote/StorageService.kt`
- `app/src/main/java/com/agroroute/core/theme/Color.kt`
- `app/src/main/java/com/agroroute/core/theme/ExtendedColors.kt`
- `app/src/main/java/com/agroroute/core/theme/Theme.kt`
- `app/src/main/java/com/agroroute/features/auth/AuthViewModel.kt`
- `app/src/main/java/com/agroroute/features/auth/LoginScreen.kt`
- `app/src/main/java/com/agroroute/features/auth/RegisterScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/bids/ViewBidsScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/calendar/HarvestCalendarScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/home/FarmerHomeScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/listing/MyListingsScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderDetailScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrdersScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/profile/FarmerProfileScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/shell/FarmerBottomBar.kt`
- `app/src/main/java/com/agroroute/features/farmer/shell/FarmerTopBar.kt`
- `app/src/main/java/com/agroroute/features/leaderboard/LeaderboardScreen.kt`
- `app/src/main/java/com/agroroute/features/reviews/SubmitReviewScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/home/SupplierHomeScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderDetailScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/profile/SupplierProfileScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/shell/SupplierBottomBar.kt`
- `app/src/main/java/com/agroroute/features/supplier/shell/SupplierTopBar.kt`
- `app/src/main/java/com/agroroute/features/welcome/WelcomeScreen.kt`
- `app/src/main/java/com/github/thelacspace/icons/Lucide.kt`
- `UI_POLISH_REPORT.md`

## Device visual evidence

- `delivery/ui-polish/device-launch.png` (light leaderboard before its additional contrast fix)
- `delivery/ui-polish/profile-light.png` (profile settings and shared navigation)
- `delivery/ui-polish/marketplace-dark.png` (dark marketplace cards, search, filters and shared navigation)
