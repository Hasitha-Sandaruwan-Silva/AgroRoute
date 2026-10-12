# Listing, stock and maps audit — 2026-10-12

## Map-flow matrix (inspection before implementation)
| Screen | Source | Editable | Storage | Display | GPS | Destination | Failures |
|---|---|---|---|---|---|---|---|
| Farmer/Supplier profile | profile location string | yes | Firestore + Room | shared picker | optional | selected profile point | missing/legacy address, denial, no fix, geocoder/style failure |
| Listing creation/edit | listing location, profile initial value | yes, history guarded | Firestore + Room | shared picker | optional | pickup | missing coordinates, historical edit guard |
| Request/Bid | submission deliveryLocation | before submit | Firestore + Room | shared picker | optional | delivery | missing coordinates, denial, no fix |
| Marketplace | listing location | no | bitmap LRU cache | MapLibre snapshot, semaphore 1 | no | pickup | missing coordinates/style/timeouts |
| Farmer order | immutable order delivery | no | order Firestore + Room | shared map | no | delivery | address-only legacy snapshot |
| Supplier order | immutable pickup before collection, delivery later | no | order Firestore + Room | shared map | optional | stage-specific snapshot | denial/no fix/missing legacy snapshot |
| Picker | restored string then selected pin | yes | caller saves address + [latitude, longitude] | one remembered MapView | optional | selected coordinate | search/no address/permission/GPS/map failure |

Existing authoritative expiry: nullable Long expiresAt, epoch milliseconds; null means no deadline. No expiry input currently exists. Existing bids are price-only, implicitly buying remaining stock. Quantity support must preserve null legacy semantics. Acceptance already leaves competing submissions pending. Existing request inbox observes stock reactively; bid inbox captures a stale listing map. Existing order maps already use snapshots, but a repository/Rules path still allows changing delivery and must be restricted to legacy missing-coordinate repair.


## 1–3. Expiry model, enforcement and authoritative time

`HarvestListing.expiresAt: Long?` is the existing authoritative Unix-millisecond deadline in Firestore and Room. Null/absent retains the legacy no-deadline meaning. No second expiry field or persisted EXPIRED status was added. Expiry is derived when `now >= expiresAt`.

Security Rules compare the original listing deadline against `request.time.toMillis()` at commit. New submissions, acceptance updates, stock reservation and order creation all require an unexpired listing. Invalid non-null deadline types fail closed. Publication rejects already-expired deadlines; subsequent ordinary updates cannot clear or extend the deadline. Expired listings cannot be deleted through the client Rules. Phone time is only a preliminary feedback/presentation check; moving the phone clock backward cannot authorize an expired write. Kotlin acceptance transactions re-read the authoritative listing and check expiry; Rules provide the trusted commit-time decision, including the open-at-10:59/submit-at-11:01 race. Existing Spark score and pickup timestamps still use server timestamps.

Creation offers optional availability in hours, stored in the existing expiresAt. Blank means no expiry. The fixed absolute deadline is established when publishing; the duration input uses local time, so a skewed device clock can affect the chosen deadline and display, but cannot bypass Rules after that deadline. Existing deadlines are preserved in edits. Presentation refreshes every second without remote expiry writes. Expired listings leave the active marketplace; Farmer history and pending submission explanations show Expired/Listing expired. Existing orders remain eligible for pickup, delivery and review. Expiry itself neither creates score events nor cancels orders.

Reference: [Firebase Rules Timestamp.toMillis](https://firebase.google.com/docs/reference/rules/rules.Timestamp_).

## 4–7. Requests, bids, actionability and atomic stock

Direct requests retain requestedQuantity and existing pre-harvest semantics. Submission and acceptance both check availability; acceptance checks current stock, READY harvest, purchase mode, owner, pending proposal, locations and duplicate order inside the transaction.

Previously Bid contained only offerPrice and acceptance purchased all remaining stock. New optional `Bid.requestedQuantity: Double?` extends that same model. New UI requires a finite positive quantity no larger than current stock. Existing null/absent price-only bids retain all-remaining-stock acceptance semantics. Rules retain compatibility with older clients using null/absent quantity. Room is version 8, with a non-destructive 7→8 migration adding one nullable REAL column; all earlier migrations remain registered.

Bid acceptance verifies the persisted bid quantity (or legacy remaining quantity), exact price, crop/unit, participants, status and deterministic order ID. It reads listing, proposal, existing order and Farmer profile before writes. It decrements stock once, updates only the accepted proposal, creates the immutable location snapshots and deterministic Farmer +1 score event/profile aggregate in the same existing Spark transaction. Rules independently require source proposal, exact quantity, atomic stock delta, non-negative stock and acceptance event. No competing-proposal rejection loop exists or was added.

Actionability derives from pending UI state plus current listing status, draft/harvest/mode, expiry, available stock and valid locations. Insufficient stock disables Accept with `Insufficient stock — N KG available`; pending status is retained. Farmer bid inbox now observes current listing data instead of a captured listing map. Subscribed listing streams synchronize Firestore into Room, so remote stock restoration also updates the screens. Rows leaving the OPEN query are refreshed rather than erased from history. Supplier bids retain Pending semantics and display their requested quantity; no extra temporary status was added.

## 8–9. Cancellation, restoration and sold out

The valid pre-pickup cancellation protocol and actor-only -2 penalty (floor zero) remain unchanged. Stock restoration occurs exactly once through the existing transaction. A fully sold-out listing retains the existing FULFILLED storage status and disabled bidding. On valid restoration, the purchase mode is recovered from the order's bidId: bid orders reopen bidding; direct-request orders reopen fixed-price requests. Legacy missing bidId defaults to direct mode. Previously restoration always enabled bidding, including direct listings; this is corrected in both Kotlin and Rules.

Partial acceptance leaves other submissions pending. The 100/40/30/70 scenario is tested against actual Rules: accepting A40 leaves 60; C70 cannot accept but stays pending; cancellation restores 100; C70 and then B30 can accept. Concurrent 70/70 acceptance yields one winner, 30 remaining and exactly one +1. An expired listing may have its stock restored and storage status reopened, but all new actions remain blocked by its deadline. Historical orders are unaffected.

## 10–13. Location flows, pickup, delivery and legacy compatibility

The pre-implementation matrix above covers profiles, listing editor, submission destinations, marketplace preview, shared picker and role-specific order maps. Farmer/Supplier profile values remain Firestore/Room strings and use the shared picker. Farmer listing pickup is independent of later profile edits. Supplier request/bid delivery is selected before submission and is copied from the accepted proposal, never from the Supplier's current profile.

Encoding stays `Address\n[latitude, longitude]`, with locale-independent decimal formatting. Parsing checks finite latitude [-90,90] and longitude [-180,180]; malformed or address-only legacy values return null without inventing a pin. MapLibre/GeoJSON receives longitude first in its API, while persisted text remains latitude first. Empty geocoder address does not invalidate valid coordinates.

For an active, published, unexpired, non-failed legacy listing missing usable pickup coordinates, Edit presents a direct Add pickup pin picker. It submits an exact copy changing only location. The use case bypasses pending-bid/order-history edit guards only for this exact repair; unrelated commercial, stock, expiry or identity changes still follow the existing guards. The repository rechecks the current document in a transaction and writes only location. A narrow owner-only Rules branch permits this repair. Existing order pickup snapshots are never rewritten. Closed/expired/failed listings do not receive this repair exception.

Delivery snapshot correction was an explicit exception in the previous code/Rules. To meet this task's immutable destination requirement, the repository, UI and Rules now allow only one-time repair of an older order with missing/invalid delivery coordinates, by its Supplier before completion/cancellation. A valid accepted delivery snapshot cannot be replaced, including during IN_TRANSIT. This intentionally tightens the former edit behavior; profile changes never rewrite either destination.

## 14–17. Maps, lifecycle, failures, preview and snapshots

MapLibre + MapTiler Streets, local.properties/BuildConfig key configuration, Android Geocoder and LocationManager remain. No new map SDK, Directions API, billing service or navigation engine was introduced. Existing external maps-app directions remain and receive saved snapshot coordinates.

The picker still uses safeDrawing union IME insets, a reserved visible confirmation footer, flexible map height, scrollable address/results and movableContentOf to preserve the native map during layout changes. LocationPickerLayout was not changed. Selection is rememberSaveable; reopening restores the saved coordinate/address. Reverse geocoding now runs only for a new selection, so merely reopening does not rewrite a saved/manual address. No-result/error falls back to Selected location and a readable message; confirmation remains enabled for valid coordinates.

Permission is requested only when the user invokes the current-location action. Denial leaves map/search selection available and offers Open app settings, including the permanently-denied case. Disabled providers/no last-known fix return a readable unavailable state. The existing 15-second fix timeout and listener cancellation cleanup remain. Cached fixes must be within two minutes, not future-dated in elapsed realtime; non-finite/out-of-range/default 0,0 device fixes are ignored. Purposefully selected valid coordinates remain compatible, including coordinate-only stored locations.

MapView is remembered rather than recreated on recomposition/resize; the existing single style load and one selected-pin source/layer remain. Start/resume/pause/stop and low-memory callbacks were already forwarded. Destruction is now idempotent and handles owner ON_DESTROY as well as composition disposal. Marker/current-fix sources clear when their inputs disappear. Camera focus waits for style readiness and updates order destinations; manual picker panning does not continuously trigger camera movement.

Marketplace cards keep 120dp bitmap snapshots, a serialized renderer, 8MB LRU, 720px width cap and 15-second timeout/cancellation. They do not instantiate interactive MapViews per card. Missing coordinates and failed preview render retain compact fallbacks. Native URL/key logging remains disabled. Map load messages are readable and no longer expose configuration instructions to ordinary users.

Supplier AWAITING_PICKUP maps use the immutable pickup snapshot; later transport uses saved delivery. Farmer maps display saved delivery and have no Supplier GPS/navigation controls. Legacy missing snapshots display their stored addresses and disable navigation. Firebase write errors are mapped to readable expiry/changed-state, connectivity or stock-conflict messages; coroutine cancellation propagates.

Reference: [MapLibre lifecycle configuration](https://maplibre.org/maplibre-native/android/examples/configuration/).

## 18–19. Rules and security

Changed Rules: trusted expiry checks; immutable deadline; expired-history deletion guard; bounded bid quantity and exact order quantity; acceptance must create a new deterministic order; narrow pickup-coordinate repair; delivery snapshot immutability with legacy repair; correct cancellation purchase-mode restoration; bounded changed listing location.

Existing pickup verifier/session guards, PIN rotation/lockout, participant authorization, immutable scoring events, exact scoring amounts, delivery and review protections remain. The existing participant-attested Spark PIN trust boundary remains; this task does not claim that Rules can prove PBKDF2 correctness. No Functions runtime dependency was introduced. No API key, token, PIN or verifier is printed in this report. Nothing was deployed.

## 20–23. Validation and build

| Check | Final outcome |
|---|---|
| Full Android unit regression | **213 passed**, zero failures/errors/skips. Listing/request/bid/stock/snapshot/cancellation/Spark/PIN/reviews/location/notification/navigation/theme suites included. |
| Current Firestore Rules | **163 passed**, zero failures. Original Rules and Spark suites plus 13 expiry/quantity/legacy-location cases; exact stock scenario and concurrent acceptance tested. |
| Retained backend TypeScript/build and unit checks | Build successful; **4 passed**. |
| Retained backend lifecycle emulator checks | **26 passed**. |
| Debug APK | **BUILD SUCCESSFUL**. `app/build/outputs/apk/debug/app-debug.apk` (copied from isolated output; identical SHA-256). |
| Complete presentation/instrumentation APK | **BUILD SUCCESSFUL**; includes existing 24 picker cases and added expiry/stock/Farmer-map cases. |
| Connected presentation execution | **Not run: no connected device or complete local emulator system image**. Existing 24 picker cases retained, not counted as executed. |
| Whitespace/diff validation | git diff --check passed. |

Final evidence: listing-stock-android-release.log, listing-stock-rules-release.log, listing-stock-backend-unit.log and listing-stock-backend-emulator.log in this audit directory. Android XML: build/listing-stock/app/test-results/testDebugUnitTest. Presentation APK: build/listing-stock/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk. **406 executed tests passed** across Android, Rules and retained backend suites; presentation compilation is excluded from that total.

The broader unfiltered unit suite exposed two date-dependent reminder fixtures fixed at 2026-10-10, now in the past. Those test inputs now use a future UTC day; production reminder logic was not changed. The new restoration transaction fixture was corrected to deserialize bidId. The earlier Rules assertion allowing replacement of a valid delivery destination was updated to exercise legacy-only repair and immutable snapshots.

Standard output builds encountered stale generated Java sources and a Windows R.jar lock. Validation uses a separate output tree through `business-logic-audit/listing-stock-build.init.gradle`, with the installed Java 21 and offline Gradle cache. Exact Android command:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\Hasitha\.gradle'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline --no-daemon --init-script business-logic-audit/listing-stock-build.init.gradle '-Pkotlin.compiler.execution.strategy=in-process'
```

Rules were tested only with demo-agroroute-rules on localhost, using an isolated port 8094 when 8082 was occupied. Each rules-unit-testing environment loads the current firestore.rules. Reproduce with the existing firebase.rules-test.json emulator config when its port is free, then `npm.cmd --prefix rules-tests test`. The retained backend checks are `npm.cmd --prefix functions test` and the three functions/test/*.emulator.cjs lifecycle files against the same local demo emulator; these do not establish a runtime dependency.

## 21. Files changed

- `app/src/androidTest/java/com/agroroute/OrderFlowPresentationTest.kt`
- `app/src/main/java/com/agroroute/core/components/ListingActionPresentation.kt`
- `app/src/main/java/com/agroroute/core/components/ListingClock.kt`
- `app/src/main/java/com/agroroute/core/components/LocationMap.kt`
- `app/src/main/java/com/agroroute/core/components/LocationPickerDialog.kt`
- `app/src/main/java/com/agroroute/core/database/DatabaseMigrations.kt`
- `app/src/main/java/com/agroroute/core/di/DatabaseModule.kt`
- `app/src/main/java/com/agroroute/core/location/LocationServices.kt`
- `app/src/main/java/com/agroroute/core/location/SelectedLocation.kt`
- `app/src/main/java/com/agroroute/core/model/Bid.kt`
- `app/src/main/java/com/agroroute/core/model/HarvestListing.kt`
- `app/src/main/java/com/agroroute/core/remote/SparkOrderLifecycle.kt`
- `app/src/main/java/com/agroroute/core/repository/BidRepositoryImpl.kt`
- `app/src/main/java/com/agroroute/core/repository/HarvestListingRepositoryImpl.kt`
- `app/src/main/java/com/agroroute/core/repository/OrderRepositoryImpl.kt`
- `app/src/main/java/com/agroroute/core/repository/OrderRequestRepositoryImpl.kt`
- `app/src/main/java/com/agroroute/core/util/Constants.kt`
- `app/src/main/java/com/agroroute/core/util/PresentationClock.kt`
- `app/src/main/java/com/agroroute/core/util/Result.kt`
- `app/src/main/java/com/agroroute/features/farmer/bids/AcceptBidUseCase.kt`
- `app/src/main/java/com/agroroute/features/farmer/bids/BidsViewModel.kt`
- `app/src/main/java/com/agroroute/features/farmer/bids/FarmerBidsScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/bids/ViewBidsScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/listing/CreateListingScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/listing/MyListingsScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/listing/UpdateListingUseCase.kt`
- `app/src/main/java/com/agroroute/features/farmer/orderrequests/FarmerOrderRequestsViewModel.kt`
- `app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/bids/SupplierBidsScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/marketplace/BrowseListingsScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/marketplace/MarketplaceViewModel.kt`
- `app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderDetailScreen.kt`
- `app/src/test/java/com/agroroute/core/location/LocationServicesFailureTest.kt`
- `app/src/test/java/com/agroroute/core/notification/HarvestReminderSchedulerTest.kt`
- `app/src/test/java/com/agroroute/core/remote/SparkOrderLifecycleTest.kt`
- `app/src/test/java/com/agroroute/core/repository/OrderLocationFlowTest.kt`
- `app/src/test/java/com/agroroute/features/orders/ListingActionPresentationTest.kt`
- `app/src/test/java/com/agroroute/features/orders/ListingStockExpiryTest.kt`
- `business-logic-audit/LISTING_STOCK_MAP_AUDIT-2026-10-12.md`
- `business-logic-audit/listing-stock-build.init.gradle`
- `firestore.rules`
- `rules-tests/firestore.rules.test.mjs`
- `rules-tests/spark.lifecycle.test.mjs`

## 24–26. Deployment, indexes and limitations

**Firestore Rules deployment is required** before production can enforce expiry, partial bid quantities and the snapshot protections. No deployment was performed. No new composite indexes are required: subscriptions use existing farmerId/status/listingId filters. Install the new APK with the normal Room 7→8 migration; no destructive cache reset is required.

No phone was connected. The only installed emulator system-image directory is incomplete (.installer only), so connected presentation execution was unavailable. All existing 24 LocationPicker cases remain in source and are compiled; they are not reported as passed. The added expiry, stock-restoration and Farmer-map presentation cases likewise require a device. Visual/GPU behavior, real rotation, real GPS/Geocoder provider behavior and two-account production network behavior remain manual checks. Unit doubles cover permission denial, providers disabled, no fix timeout, absent Geocoder, bounded coordinate save/restore and transaction snapshots. Actual authenticated Rules clients cover expiry, stock concurrency and immutable destination security.

No scheduler persists EXPIRED or cancels pending submissions; expiry/actionability are derived without Spark-incompatible background infrastructure. Client-clock skew may temporarily affect presentation; Rules remain authoritative. Legacy bids without requestedQuantity continue buying all remaining stock. Legacy submissions without usable delivery coordinates still follow the existing re-submit requirement; no commercial terms are silently rewritten. Backend retained Functions implementation was left unchanged and is regression-only.

## 27. Exact manual checklist

1. **Expiry:** Farmer creates a listing with Available for = 0.02 hours (about 72 seconds). Submit a request or bid before expiry and leave pending review open. After the deadline, check active marketplace removal and Farmer Expired history label; Request/Bid/Accept are disabled or fail with readable expiry/changed-state feedback. Try submitting from a screen opened before expiry. Move the phone clock backward and verify the server still rejects. An already accepted order remains eligible; expiry produces no score event.
2. **Competing bids:** On a 100kg bidding listing, place A40/B30/C70 with valid delivery pins. Accept A; verify 60kg, A Accepted, B Pending/actionable, C Pending/disabled with 60kg available. Cancel A through valid pre-pickup cancellation; verify 100kg, actor-only -2 with floor zero, B still Pending and C actionable without recreating it. In a separate run accept two oversubscribing bids from separate Farmer sessions concurrently; verify no negative stock, duplicate orders or duplicate +1. Also sell out a fixed-price listing, cancel before pickup and verify it reopens in fixed-price mode.
3. **Farmer location:** Select a pickup point, save and reopen; verify identical marker/address/coordinates. Edit the profile afterward and verify listing/order destinations do not silently change. On an active address-only legacy listing with pending bids/order history, use Add pickup pin; verify only listing.location changes. Try changing price/quantity simultaneously and verify the normal history guard still applies.
4. **Supplier delivery:** Select distinct request and bid delivery pins, submit and accept. Verify each order copies the original selected proposal destination. Change the Supplier profile and confirm existing snapshots are unchanged. Try editing a valid order destination and verify it is unavailable/denied; only an older missing-coordinate destination can be repaired once by its Supplier.
5. **Order maps:** Supplier before collection sees Farmer pickup and Navigate to Farmer; after collection sees saved delivery. Farmer sees a saved destination map without Supplier navigation/GPS controls. Verify legacy missing coordinates show readable address fallback and disabled navigation. Change listing/profile location and verify the order map remains fixed.
6. **Picker responsiveness/failures:** Small and large portrait viewports, gesture navigation, three-button navigation and keyboard open: Use this location stays visible. Save/reopen and rotate; resize must preserve selection/map instance. Deny and permanently deny location permission; use map/search or settings. Disable GPS; test no fix and stale fix, unavailable search/Geocoder, offline map style and preview failure. Valid coordinates remain saveable without an address; no default 0,0 is silently selected. Scroll marketplace cards and check compact cached previews.
7. **Spark order flow:** Accept → Supplier six-digit code → Farmer verified handover → Supplier pickup → IN_TRANSIT → delivery → DELIVERED → reviews. Verify Farmer +1/+3/+6 and Supplier +4/+6 once, PIN rotation/lockout, authorized participants and one eligible review per participant. Repeat pre-pickup cancellation from each role and confirm actor-only -2 and exactly-once restoration. Repeat pickup/delivery after the listing deadline; historical orders remain valid.
