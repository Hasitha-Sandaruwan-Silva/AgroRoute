# Normal app Local Demo Mode audit - 2026-10-13

## Inspection and architecture

MainActivity hosts the theme and NavGraph. NavGraph restores the Firebase-backed AuthViewModel session, enforces role routes and selects the Farmer/Supplier scaffold. Screens request Hilt ViewModels; use cases read repository interfaces. Production Firestore fetches/listeners - repositories - existing agroroute_db Room v8 DAOs - Flow/StateFlow ViewModels - Compose screens. Server writes/transactions precede cache updates. Application notification bootstrap and token registration are additional background paths. Order details also used direct Firestore profile-contact lookup; Supplier order-list photos used a direct Hilt GetListingsUseCase cache lookup. Both required scoped read boundaries, independent of the ViewModel factory.

The retained DemoDatabase/DemoDataSeeder/DemoDataset use the same v8 AppDatabase and migrations in agroroute_local_demo_db. No demo rows enter agroroute_db. Original dataset, seeding/idempotence and literal demo_ clearing are retained; the original utility remains compiled for development but loses its separate exported launcher intent.

## Integration design

Source-specific AppExperience hosts normal NavGraph in release. In debug, the normal welcome page offers Try Demo Mode through a source-specific entry slot. Entry pauses application background registration/watchers, opens the isolated database and seeds only if the Tomato anchor is absent. Role choice defaults to Nimal Perera/demo_farmer_001 or FreshMart Traders/demo_supplier_001. DemoSessionManager contains only local enabled/role/user state; it never receives FirebaseAuth or a production repository. An activity ViewModel retains the selection across rotation; process recreation starts normal mode. The menu can choose any of the three existing profiles per role to inspect every lifecycle stage.

The scoped screen factory supplies allowlisted Room-only adapters for Auth, listings, bids, requests, orders, reviews and leaderboards. Read operations reuse existing DAOs and return existing Result/Flow types. Local refreshes are reads/no-op refreshes, never transaction success. Every mutation, pickup-verification/code operation and upload fails with Unavailable in Local Demo Mode. The factory constructs the existing ViewModels and use cases from their Inject constructors using local interfaces, with no Hilt fallback or remote service construction. A generic screen factory hook defaults to the original Hilt call in production. Existing screen injection imports point at that hook; there are no per-screen database/mode conditionals.

The same NavGraph, route guards, FarmerScaffold/SupplierScaffold, bottom bars, dashboards, listing cards/details, bids, requests, orders/details, profile pages, review eligibility, leaderboards and OrderLocationSection are reused. A compact debug-only badge/menu surrounds the existing app UI. Role/sample-user changes discard the old ViewModelStore and route tree before creating the correct role home. No second fake role UI was built. Generic read-only UI policy disables primary mutations, acceptance/rejection, withdrawal, order cancellation/delivery, pickup workflow, review submission and profile/avatar editing. Adapters provide a second safety boundary even if a UI callback is invoked directly.

## Navigation and data behavior

Farmer: Home - Listings (existing detail/View Bids route), Bids via home/top bar, Requests, Orders/details, Profile, Calendar and leaderboard. Tomato shows pending40/30/70 through the existing stock/actionability calculation. Supplier: Home - Marketplace (existing filters, eligibility, stock/expiry and bitmap map previews), listing bid/fixed-request details, My Bids, Orders with requests/history, Profile and leaderboard. Expired/sold-out rows remain in Farmer history and are excluded by the existing marketplace eligibility logic.

Existing dashboard ViewModels derive their normal counts from the scoped local repository streams; no production aggregation code changes. Selected-profile ownership filtering is preserved. Supplier1 has waiting Carrot, transit Cabbage and delivered Chilli; Supplier2 has delivered Potato/Beans; Supplier3 has cancelled Pumpkin and waiting Brinjal. Use the menu's sample-user choice to inspect those histories. Farmer1 has Tomato bidding and waiting/delivered orders; Farmer2 owns transit Cabbage; Farmer3 owns Beans/Pumpkin history. Existing reviews appear through local review/leaderboard reads.

Maps retain immutable order.pickupLocation/deliveryLocation snapshots. Supplier waiting maps show saved pickup, transit shows saved delivery, terminal maps are summaries; Farmers see saved delivery without Supplier controls. Current GPS/tiles/external navigation retain their existing behavior. ProfileContact reads local profiles in the scope, and order-list listing-photo presentation reads the same demo listing stream. Backend PINs/scoring/transaction results are not fabricated. Cached records browse without Firestore connectivity; tiles and external maps still need normal connectivity/apps.

## Safety and release behavior

Production repository implementations, Firestore Rules, Room schema/version/migrations, expiry/stock/scoring/PIN/order-transition logic and map renderers are unchanged. Profile upload gained a minimal uploader interface bound to the existing implementation; the demo graph supplies a rejecting uploader. No demo account/token is created, no UID is assigned to FirebaseAuth, and the demo graph has no Firestore/Functions/FCM clients. Contact/cache escape paths and notification screen permissions are scoped or blocked before remote dependencies are requested.

Application token callbacks and notification bootstrap respect a source-specific background gate. Debug pauses it during demo; release always enables it. RealtimeEventNotifier's watches now belong to its session job so stop actually cancels the child listeners rather than leaving external-scope watches alive. No sign-out, token deletion, topic clearing or production cache deletion happens when entering/exiting demo. Real-account token registration resumes on exit, including tokens rotated while access was paused; it always obtains the UID from actual FirebaseAuth, never the local facade. Work already sent before entry cannot be recalled; no new demo operation starts a remote request.

Exit (including Back at role home) clears only DemoSession state, restores background access and returns to the normal start page while preserving a real signed-in session. Demo data persists until explicit Clear. Load reuses the existing idempotent seeder; Reset deliberately clears/reseeds the demo namespace with a fresh date anchor; Clear deletes only that namespace and returns to local role selection. These actions are in the debug menu only.

Release uses the existing real NavGraph/Hilt graph, empty entry slot, always-enabled background gate and default mutation policy. No demo dataset/session/adapter, role chooser, badge, menu, sample IDs or launcher is included. Artifact checks and final test evidence follow below.

## Validation

| Check | Final result |
|---|---|
| Full Android unit suite | **261 passed**, zero failures/errors/skips: 239 retained production, 12 retained demo-data, 8 local-session and 2 notification-listener cancellation cases. |
| Pixel 7 / Android 17 requested phone cases | **133 distinct cases passed**, combining 120 unchanged passing cases from the full run with a clean 13-case focused rerun. |
| Focused final phone rerun | **13 passed**, zero failures/skips: all 9 normal-demo UI cases and all 4 picker-dialog cases. |
| Debug, instrumentation and unsigned release APKs | **BUILD SUCCESSFUL**. |
| Artifact isolation | Debug contains the entry/data/adapters; release DEX and manifest contain no demo classes, IDs or entry. Both variants have one normal launcher. |
| Source validation | git diff --check passed; production repositories, models, database/migrations, Rules, Spark lifecycle and map/picker renderers unchanged. |

**394 distinct test cases passed** (261 unit + 133 phone). Repeated cases are counted once. The full 133-case phone run recorded 131 passes and two failures: the new Supplier-bids test incorrectly expected a crop name that the existing screen does not display, and an existing dark-keyboard picker case lost its Compose activity/root. The bids assertion now checks the actual requested-quantity label plus Pending and disabled, nonempty Withdraw controls. The final 13-case rerun passed both affected suites. The full-run log remains available and is not described as a clean run.

Earlier screen attempts also lost an active Compose root; activity events showed STOPPED without a fatal app exception. The wait tolerates a temporarily absent root but still requires the expected content within the timeout. No missing content is accepted as success, and no production assertion or behavior was weakened. Adapter tests cover fail-closed mutations, local reads, unchanged FirebaseAuth UID, immutable snapshots, separate production-cache preservation and construction of the real ViewModels without remote services. Room tests retain persistence, namespace-only clear, idempotence and the real 7-to-8 migration. The retained 110 presentation cases include order flow, six native MapLibre cases, pickup and all 24 picker cases.

Evidence: [final debug/unit build](normal-demo-final-ui-build.log), [release build](normal-demo-release-validation.log), [test assertion build](normal-demo-test-assertion-build.log), [full phone run](normal-demo-phone-final.log), [clean focused rerun](normal-demo-phone-focused-final.log), [artifact isolation](normal-demo-isolation-final.log). Unit XML: build/normal-demo/app/test-results/testDebugUnitTest.

Reviewed UI captures: [Farmer home](normal-demo-previews/normal-demo-farmer-home.png), [Supplier home](normal-demo-previews/normal-demo-supplier-home.png), [marketplace](normal-demo-previews/normal-demo-marketplace.png), [saved pickup order](normal-demo-previews/normal-demo-order-pickup.png). They show real dashboards, local users/counts, normal navigation, read-only controls and the compact banner. Visual review corrected the safe-area background and banner separator. These early/Compose captures do not establish settled native basemap tile rendering; the six retained native-map tests passed separately.

## APK and reproduction

Debug APK: app/build/outputs/apk/debug/app-debug.apk, copied from build/normal-demo/app/outputs/apk/debug/app-debug.apk with matching SHA-256 **795C100616DAAAAB9ADE0713D005C1277F63CA14DC6B296CCD3A4DBD1309C996**. Instrumentation APK: build/normal-demo/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk. Unsigned release isolation artifact: build/normal-demo/app/outputs/apk/release/app-release-unsigned.apk. Debug/test APKs were installed with install -r, preserving existing app data; tests use isolated databases.

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\Hasitha\.gradle'
$env:ANDROID_USER_HOME='C:\Users\Hasitha\.android'
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline --no-daemon --init-script business-logic-audit/normal-demo-build.init.gradle '-Pkotlin.compiler.execution.strategy=in-process'
python business-logic-audit/verify-normal-demo-isolation.py
```

Final focused phone reproduction after installing those debug/test APKs:

```powershell
$adb='C:\Users\Hasitha\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r 'build\normal-demo\app\outputs\apk\debug\app-debug.apk'
& $adb install -r 'build\normal-demo\app\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
& $adb shell am instrument -w -e delay_msec 1500 -e class 'com.agroroute.NormalDemoPresentationTest,com.agroroute.LocationPickerPresentationTest' com.agroroute.test/androidx.test.runner.AndroidJUnitRunner
```

The full requested phone set additionally includes DemoAdaptersTest, DemoDatabaseTest, DemoBrowserTest, OrderFlowPresentationTest, OrderMapNativePresentationTest, OrderPickupPresentationTest and LocationPickerLayoutTest. It excludes the unrelated premium-component assertions documented in the previous map audit.

## Manual instructions

Install the final debug APK; open the single normal AgroRoute icon. At the welcome/start page (or a debug profile page) tap Try Demo Mode, wait for local loading, then Continue as Farmer or Continue as Supplier. Use the normal role tabs and home actions. Open Demo menu to switch role/sample user, Load/Reset/Clear records, or Exit Demo Mode. If a real restored session opens its home directly, open Profile and tap Try Demo Mode; no real account is signed out automatically.

## Files and limitations

No deployment, Rules/index change, schema migration or map-key change is required or performed. Demo browsing is read-only; this task does not simulate server-authoritative acceptance, cancellation, scoring, PIN handover or reviews. Existing sample point totals are illustrative. There is no Room notification fixture or new routing provider. Actual production login, server transactions and trips retain their existing normal-app behavior. Map tiles, external navigation and GPS retain normal availability requirements; a production trip was not simulated. Process recreation returns to normal mode while local demo rows persist. Clear/reload refreshes dated sample deadlines.

## Changed files

Most existing-screen changes are a single import alias to the generic scoped ViewModel provider. The additional main-source changes are the read-only UI policy, contact/cache lookup seams, upload interface, start-page redirect option and background notification/token gating/cancellation cleanup. Production repositories/models/database/migrations/Rules are unchanged.

- app/src/androidTest/java/com/agroroute/DemoAdaptersTest.kt
- app/src/androidTest/java/com/agroroute/NormalDemoPresentationTest.kt
- app/src/debug/AndroidManifest.xml
- app/src/debug/java/com/agroroute/core/runtime/AppExperience.kt
- app/src/debug/java/com/agroroute/core/runtime/BackgroundNetworkAccess.kt
- app/src/debug/java/com/agroroute/demo/DemoRepositories.kt
- app/src/debug/java/com/agroroute/demo/DemoSessionManager.kt
- app/src/debug/java/com/agroroute/demo/DemoViewModelFactory.kt
- app/src/main/java/com/agroroute/AgroRouteApp.kt
- app/src/main/java/com/agroroute/MainActivity.kt
- app/src/main/java/com/agroroute/core/components/LoadingButton.kt
- app/src/main/java/com/agroroute/core/components/OrderFlowSections.kt
- app/src/main/java/com/agroroute/core/components/OrderPickupSection.kt
- app/src/main/java/com/agroroute/core/components/ProfilePhoneContact.kt
- app/src/main/java/com/agroroute/core/di/RepositoryModule.kt
- app/src/main/java/com/agroroute/core/navigation/NavGraph.kt
- app/src/main/java/com/agroroute/core/notification/FirebaseNotificationService.kt
- app/src/main/java/com/agroroute/core/notification/NotificationBootstrap.kt
- app/src/main/java/com/agroroute/core/notification/RealtimeEventNotifier.kt
- app/src/main/java/com/agroroute/core/remote/ProfilePhotoStorage.kt
- app/src/main/java/com/agroroute/core/runtime/ScreenEnvironment.kt
- app/src/main/java/com/agroroute/features/auth/AuthViewModel.kt
- app/src/main/java/com/agroroute/features/auth/LoginScreen.kt
- app/src/main/java/com/agroroute/features/auth/RegisterScreen.kt
- app/src/main/java/com/agroroute/features/farmer/bids/FarmerBidsScreen.kt
- app/src/main/java/com/agroroute/features/farmer/bids/ViewBidsScreen.kt
- app/src/main/java/com/agroroute/features/farmer/calendar/HarvestCalendarScreen.kt
- app/src/main/java/com/agroroute/features/farmer/home/FarmerHomeScreen.kt
- app/src/main/java/com/agroroute/features/farmer/listing/CreateListingScreen.kt
- app/src/main/java/com/agroroute/features/farmer/listing/MyListingsScreen.kt
- app/src/main/java/com/agroroute/features/farmer/orderrequests/FarmerOrderRequestsScreen.kt
- app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderDetailScreen.kt
- app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrdersScreen.kt
- app/src/main/java/com/agroroute/features/farmer/profile/FarmerProfileScreen.kt
- app/src/main/java/com/agroroute/features/farmer/shell/FarmerBottomBar.kt
- app/src/main/java/com/agroroute/features/farmer/shell/FarmerTopBar.kt
- app/src/main/java/com/agroroute/features/leaderboard/HomeLeaderboard.kt
- app/src/main/java/com/agroroute/features/leaderboard/LeaderboardScreen.kt
- app/src/main/java/com/agroroute/features/reviews/SubmitReviewScreen.kt
- app/src/main/java/com/agroroute/features/settings/NotificationComponents.kt
- app/src/main/java/com/agroroute/features/settings/SettingsScreens.kt
- app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidScreen.kt
- app/src/main/java/com/agroroute/features/supplier/bids/SupplierBidsScreen.kt
- app/src/main/java/com/agroroute/features/supplier/home/SupplierHomeScreen.kt
- app/src/main/java/com/agroroute/features/supplier/marketplace/BrowseListingsScreen.kt
- app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestScreen.kt
- app/src/main/java/com/agroroute/features/supplier/orders/OrderListingPresentation.kt
- app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderDetailScreen.kt
- app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrdersScreen.kt
- app/src/main/java/com/agroroute/features/supplier/profile/SupplierProfileScreen.kt
- app/src/main/java/com/agroroute/features/supplier/shell/SupplierBottomBar.kt
- app/src/main/java/com/agroroute/features/supplier/shell/SupplierTopBar.kt
- app/src/main/java/com/agroroute/features/welcome/WelcomeScreen.kt
- app/src/release/java/com/agroroute/core/runtime/AppExperience.kt
- app/src/release/java/com/agroroute/core/runtime/BackgroundNetworkAccess.kt
- app/src/test/java/com/agroroute/core/notification/RealtimeEventNotifierSessionTest.kt
- app/src/testDebug/java/com/agroroute/demo/DemoIsolationTest.kt
- app/src/testDebug/java/com/agroroute/demo/DemoSessionTest.kt
- business-logic-audit/NORMAL_APP_DEMO_MODE-2026-10-13.md
- business-logic-audit/normal-demo-build.init.gradle
- business-logic-audit/verify-normal-demo-isolation.py
