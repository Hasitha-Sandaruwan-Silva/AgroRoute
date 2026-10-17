# Supplier order map implementation

Added Supplier navigation: Home, Marketplace, Map, Orders, Profile. The existing bottom bar dimensions, style, and routes are retained.

The map uses SupplierOrderViewModel's existing Room-backed order flow and entry/resume/reconnect synchronization. There are no marker-specific listeners, repositories, ViewModels, route requests, or polling loops. The listing presentation cache supplies farmer names when available; destinations always come from saved order snapshots.

Only the signed-in supplier's AWAITING_PICKUP and IN_TRANSIT orders are displayed. Filters affect presentation only. Pickup markers use pickup snapshots; delivery markers use delivery snapshots. Green leaf and amber truck pins have contrasting outlines. A green halo indicates a straight-line distance within 10 km when GPS is available, without excluding any active orders.

Marker selection opens a scrollable bottom preview. The order strip also opens previews, making overlapping markers and legacy orders without coordinates accessible. View Pickup, View Delivery, and Order Details open the existing supplier detail route. Navigate calls the existing openOrderDirections helper with the snapshot destination.

Location uses the existing permission contract and LocationServices, once on entry if already permitted and on explicit button presses. Denial and unavailable GPS show friendly messages. The initial camera uses GPS or marker bounds; manual movement suppresses automatic recentering. Theme-aware controls and map styles support light/dark. The supplier scaffold reserves the bottom navigation area.

No changes to repositories, Firestore/Room schemas, security rules, auth, roles, order creation/status transitions, stock, scoring, pickup PINs, location snapshot writes, notifications, bids, requests, or chat.

## Exact source and test files changed

- app/src/main/java/com/agroroute/core/components/LocationMap.kt
- app/src/main/java/com/agroroute/core/navigation/NavGraph.kt
- app/src/main/java/com/agroroute/core/navigation/Screen.kt
- app/src/main/java/com/agroroute/features/supplier/home/SupplierHomeComponents.kt
- app/src/main/java/com/agroroute/features/supplier/shell/SupplierTopBar.kt
- app/src/main/java/com/agroroute/features/supplier/map/SupplierMapPresentation.kt (new)
- app/src/main/java/com/agroroute/features/supplier/map/SupplierMapScreen.kt (new)
- app/src/test/java/com/agroroute/features/supplier/home/SupplierNavigationTest.kt
- app/src/test/java/com/agroroute/features/supplier/map/SupplierMapPresentationTest.kt (new)
- app/src/androidTest/java/com/agroroute/SupplierHomePresentationTest.kt
- app/src/androidTest/java/com/agroroute/SupplierMapPresentationTest.kt (new)

Additional artifact: SUPPLIER_MAP_IMPLEMENTATION.md (this report). Ignored local build logs: supplier-map-build.log, supplier-map-verification.log, supplier-map-device.log, supplier-map-final-build.log.

## Validation

- Full unit suite: 328 tests passed, zero failures/errors/skips, including five new map projection tests.
- Debug APK and instrumentation APK: built successfully.
- Device UI validation attempted with SupplierMapPresentationTest and SupplierHomePresentationTest. The wireless Pixel 7 disconnected before APK installation (ADB device not found); zero device tests executed. Reconnect the phone and rerun the command below.
- git diff --check: passed.

## Practical limits

- Stable markers are used without clustering. The order strip provides access to colocated orders.
- Farmer names use the existing listing cache and show Name unavailable when absent.
- Device tests cover order-strip selection and preview callbacks; native marker hit testing, external navigation app launches, map provider outages, permission-system dialogs, and both system navigation modes still need manual device QA.


Device rerun command (use the project Android Studio JDK):

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\Hasitha\.gradle'
$env:USERPROFILE='C:\Users\Hasitha'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.agroroute.SupplierMapPresentationTest,com.agroroute.SupplierHomePresentationTest'
```

Device tests are parameterized for light/dark and 320/400 dp widths, with 1.3x font scale in compact cases. They are compiled but not yet executed due to the device disconnection.
