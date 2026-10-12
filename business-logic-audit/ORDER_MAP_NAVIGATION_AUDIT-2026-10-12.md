# Order map navigation audit — 2026-10-12

## 1. Existing implementation

Supplier and Farmer order details both call `OrderLocationSection`. Its destinations already came from `Order.pickupLocation` and `Order.deliveryLocation`, parsed by the existing SelectedLocation helpers. The embedded renderer was the shared `LocationMap`, also used by the location picker. Supplier controls were shown even for terminal orders. Pickup and delivery used the same marker; the current-position button was below the map. Navigation opened an external maps application.

This upgrade changes presentation and local map/routing state only. Authentication, roles, order transitions, Spark scoring, PIN verification, cancellation, reviews, expiry, stock, Firestore Rules, Room and immutable order snapshots are unchanged. The existing MapLibre 11.8 / MapTiler Streets stack and key configuration remain.

## 2. Why the previous view could be too wide

The shared map's no-selection fallback is Sri Lanka at zoom 7. That fallback is useful to the editable picker but inappropriate as an order destination fallback. The old order camera had independent effects for destination street zoom and GPS/destination bounds. Bounds were attempted after one frame, only if MapView width/height were already nonzero; layout completion was not an effect input. A skipped fit might never retry, and subsequent GPS updates could recenter after manual panning. Padding was a uniform 48 physical pixels, rather than density-aware padding for markers and controls.

These are code-level failure mechanisms; not every historical wide view can be attributed to one cause. If the actual GPS position and destination are genuinely far apart, correctly fitting both still needs a wide geographic view.

## 3. Camera changes

Order maps opt into a dedicated `OrderMapViewport` and `OrderMapCameraController`. Picker callers retain their existing camera behavior. An order MapView starts at its saved destination, never an invented country point. Initial camera placement waits for a loaded style, the native View's actual layout size, and the initial location/route result.

- With valid current position and destination, fit both; include provider route geometry if available.
- With only the destination, center it at zoom 15.5.
- Identical points avoid degenerate bounds. Invalid/default 0,0 device fixes are ignored.
- Bounds use density-aware left/top/right/bottom padding of 32/68/64/40dp, bounded for smaller views. The information panel is below the map, so it cannot obscure pins.
- One automatic fit per stage/destination. Subsequent position updates and recompositions do not reset the camera. Gestures before an initial GPS result also suppress automatic movement.
- Explicit recenter increments a request only after the location attempt finishes and fits the latest position/destination, or centers the destination if GPS fails.
- Stage changes create a fresh camera policy but retain the same MapView; reopening a screen gets a fresh initial fit.

The native Pixel tests check actual MapLibre projection coordinates for visibility, street/city zoom, manual camera preservation, recentering and map-instance reuse. [MapLibre bounds API](https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.camera/-camera-update-factory/new-lat-lng-bounds.html) and [camera gesture reasons](https://maplibre.org/maplibre-native/android/api/-map-libre%20-native%20-android/org.maplibre.android.maps/-map-libre-map/-on-camera-move-started-listener/index.html) informed the adapter.

## 4. Markers and card composition

Current Supplier position remains a blue dot with a translucent halo and white stroke. Farmer pickup uses AgroRoute green; delivery uses a distinct harvest amber pin. A compact legend identifies You and Pickup/Delivery. Marker coordinates are taken exactly from the immutable order snapshot; GeoJSON conversion explicitly uses longitude first, while saved strings remain latitude first.

The card has a modest stage heading, rounded map, 48dp floating recenter button, compact destination address and route/status line immediately below the map. Active maps are 220–300dp, depending on screen height. Terminal Supplier maps are a 140dp summary; Farmer maps remain a read-only delivery summary. There are no extra nested cards or large headings.

## 5. Stage-aware destinations

| Viewer/state | Destination | Controls |
|---|---|---|
| Supplier AWAITING_PICKUP | Farmer pickup snapshot | Go to Farmer pickup; recenter; Navigate to Farmer |
| Supplier IN_TRANSIT | Supplier delivery snapshot | Go to delivery destination; recenter; Navigate to Delivery |
| Supplier DELIVERED | Saved delivery summary | No GPS/navigation controls |
| Supplier CANCELLED | Saved pickup summary, matching pre-pickup cancellation | No GPS/navigation controls |
| Farmer, every state | Saved delivery snapshot/summary | No Supplier GPS/navigation controls |

Profile locations are never read for routing destinations. No destination is saved or rewritten by GPS, recentering or route rendering. Legacy missing coordinates show the existing saved address and disable external navigation.

## 6. Current-location behavior

The existing LocationServices implementation remains: fine/coarse permission checks, provider availability, fresh last-known fixes, a 15-second fix timeout, listener cleanup, bounded coordinates and rejection of silent default 0,0. On entry/stage change, permitted Supplier location is fetched once. The floating control requests permission when needed, updates GPS and recenters. Denial/no fix uses readable messages and preserves the saved destination/external-navigation fallback. Terminal and Farmer maps do not request GPS.

This is a one-shot position/recenter experience, not a continuously tracking or turn-by-turn navigation engine. Current GPS never becomes the order's destination.

## 7. Routing architecture

`Routing.kt` adds provider-neutral `RouteProvider`, `RouteRepository`, `RouteResult`, `RoadRoute` and optional `RouteManeuver`. RoadRoute contains supplied geometry, distanceMeters and durationSeconds. It validates geometry/metrics. The cache owns copied geometry/maneuver lists so a provider cannot mutate displayed data after returning it.

`CachedRouteRepository` keeps up to four request results per session, deduplicates concurrent requests, bounds provider waits to ten seconds, propagates coroutine cancellation and maps provider failures/timeouts to Unavailable without leaking raw diagnostics. The screen requests a route for the first usable position in a stage; ordinary successful recenter/position updates reuse the current route. There is no automatic rerouting engine, off-route detector or new per-GPS network call.

MapLibre draws provider geometry through one existing GeoJSON source and two line layers (white casing plus botanical green route), created once for an order map's style. Updates replace source data; unavailable/terminal routes clear it. Picker maps do not receive route layers. No line is synthesized between current position and destination.

## 8. Live routing provider

**No live road-routing provider was added or called.** Inspection found no configured road-routing provider/endpoint. The existing MapTiler style/tile key is not a configured routing service. The default UnconfiguredRouteProvider returns NotConfigured. MapLibre/MapTiler were preserved; no Google Directions, Google Navigation, Mapbox Navigation, paid SDK or new dependency was added.

Native renderer tests include explicitly synthetic provider fixtures to verify geometry rendering/bounds. These fixtures are test-only, not road-routing data or a production straight-line driving route.

## 9. Distance and ETA

Distance and ETA appear only for an Available provider RoadRoute: kilometers plus rounded-up minutes, for example `12.4 km • 24 min`. No geodesic distance is presented as a road distance, and no ETA is invented. Without a route, the card says `Road route unavailable. Use external navigation.` Loading states distinguish getting a position from finding a road route. The route summary area already supports provider metrics without another card redesign.

## 10. External navigation

The existing external maps-app/browser/geo fallback is unchanged and receives the stage's immutable snapshot. Labels are Navigate to Farmer before pickup and Navigate to Delivery during transport. GPS, provider or map-style failure does not disable a valid destination's external action. DELIVERED/CANCELLED and Farmer views have no active navigation controls.

## 11. Files changed

- `app/src/main/java/com/agroroute/core/components/OrderLocationSection.kt`
- `app/src/main/java/com/agroroute/core/components/LocationMap.kt`
- `app/src/main/java/com/agroroute/core/location/OrderMapPresentation.kt`
- `app/src/main/java/com/agroroute/core/location/Routing.kt`
- `app/src/test/java/com/agroroute/core/location/OrderMapPresentationTest.kt`
- `app/src/test/java/com/agroroute/core/location/RoutingTest.kt`
- `app/src/androidTest/java/com/agroroute/OrderFlowPresentationTest.kt`
- `app/src/androidTest/java/com/agroroute/OrderMapNativePresentationTest.kt`
- `business-logic-audit/order-map-build.init.gradle`
- This report; any captured native previews listed in final validation below.

No Firestore Rules, Firebase configuration, backend lifecycle, business repositories/models or picker dialog/layout files changed.

## 12. Tests

| Check | Final result |
|---|---|
| Full Android unit suite | **239 passed**, zero failures/errors/skips (213 retained + 26 new map/routing cases). Includes existing Spark lifecycle/scoring/PIN/stock/location regressions. |
| Requested connected-phone suite, Pixel 7 / Android 17 | **110 passed**, zero failures/skips: 72 order presentation + 6 native-map + 8 pickup + 20 picker layout + 4 picker dialog. |
| Native render/visual QA after safe-area/render-settling capture adjustment | **6 passed again**, zero failures/skips. Basemap tiles, green pickup, amber delivery and marker framing visually inspected. |
| Debug and presentation APKs | **BUILD SUCCESSFUL**. |
| Source diff/whitespace | git diff --check passed. Protected business/lifecycle/Rules files unchanged. |

**349 distinct requested test cases passed**; the additional native rerun is not counted twice. The 750ms runner delay lets test activities settle between cases; it is a test-run argument, not an app behavior change. Final phone evidence: order-map-phone-final.log and order-map-native-final.log. Unit XML: build/order-map/app/test-results/testDebugUnitTest. Build evidence: order-map-build-final.log and order-map-preview-build.log. Historical Gradle phone XML/HTML still record the earlier interrupted attempts, not the final clean direct-run results.

Reviewed native screenshots (dummy snapshot locations): [pickup card](order-map-previews/pickup-card.png), [current position plus pickup bounds](order-map-previews/pickup-bounds.png), [amber delivery destination](order-map-previews/delivery-destination.png). Initial captures were too early for native tile/symbol rendering; the reviewed captures wait for rendering to settle and include test safe-area padding.

New unit cases cover stage selection, immutable destinations, missing coordinates, GPS/bounds/destination-only fallback, pending initial fits, gestures, recompositions, explicit recenter, scene resets, correct coordinate order, road geometry bounds, unconfigured/failed/timed-out providers, cache/concurrency, immutable cache copies and cancellation. Existing permission-denied/provider-disabled/no-fix location tests and Spark lifecycle regressions are included in the unfiltered unit suite.

Six native MapLibre phone cases verify projection visibility for current/destination/route geometry, street zoom, camera preservation/recenter, stage changes with the same MapView and a deliberate invalid **local** style JSON failure retaining the saved address and external action. Order presentation adds stage changes, terminal summary controls and the floating recenter button across light/dark and compact/normal layouts. All 24 existing picker cases remain unchanged.

First unfiltered phone run: 114 attempted, 98 passed, 16 failed. All six native-map and all 24 picker tests passed in that run. Twelve order tests lost their Compose activity; the activity logs show immediate PAUSED/STOPPED transitions without a fatal app crash. Four unrelated existing PremiumComponentsTest cases expected two AM nodes but found one. Those premium assertions/components were not changed as part of this map-only task. Final results below distinguish the requested focused suite from that exploratory broader run.

## 13. APK and reproduction

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, copied from `build/order-map/app/outputs/apk/debug/app-debug.apk` with matching SHA-256. Presentation APK: `build/order-map/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. The final production source built successfully; all unit results match that source. Nothing was deployed to Firebase or any hosting service.

The build uses isolated `build/order-map` outputs to avoid the previously encountered Windows locks. With the installed Java 21 and Gradle cache:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\Hasitha\.gradle'
$env:ANDROID_USER_HOME='C:\Users\Hasitha\.android'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest --offline --no-daemon --init-script business-logic-audit/order-map-build.init.gradle '-Pkotlin.compiler.execution.strategy=in-process' '-Pandroid.testInstrumentationRunnerArguments.class=com.agroroute.OrderFlowPresentationTest,com.agroroute.OrderMapNativePresentationTest,com.agroroute.OrderPickupPresentationTest,com.agroroute.LocationPickerLayoutTest,com.agroroute.LocationPickerPresentationTest' '-Pandroid.testInstrumentationRunnerArguments.delay_msec=750'
```

The final clean phone run used the same built APKs directly, retaining screenshots before cleanup:

```powershell
$adb='C:\Users\Hasitha\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r 'build\order-map\app\outputs\apk\debug\app-debug.apk'
& $adb install -r 'build\order-map\app\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
& $adb shell am instrument -w -e delay_msec 750 -e class 'com.agroroute.OrderFlowPresentationTest,com.agroroute.OrderMapNativePresentationTest,com.agroroute.OrderPickupPresentationTest,com.agroroute.LocationPickerLayoutTest,com.agroroute.LocationPickerPresentationTest' com.agroroute.test/androidx.test.runner.AndroidJUnitRunner
```

The existing four unrelated premium-component assertions remain a broader-suite limitation; none of their production components or assertions were changed to obtain the requested suite's pass.

## 14. Required manual setup

**No Firestore Rules deployment, index, database migration or map-key change is required for this upgrade. Nothing was deployed.**

Live embedded road routes require a separately configured non-billing RouteProvider/endpoint and a key if that provider requires one. Implement its roadRoute adapter, return actual road geometry and metrics, and provide `CachedRouteRepository(provider)` to OrderLocationSection through the existing optional repository parameter. Keep credentials in appropriate local/BuildConfig configuration; never reuse or print the tile key as a routing credential. No service, account or paid SDK has been selected automatically.

## 15. Remaining limitations and manual checks

Without a routing provider the embedded view is a well-framed current-position/destination map with external road navigation; it cannot display live road geometry/ETA. No continuous GPS tracking, maneuver guidance, turn-by-turn voice or rerouting is implemented. A future active navigation feature should add meaningful off-route thresholds and a cooldown before requesting another route.

Phone validation uses dummy accepted-order snapshots and controlled native-map fixtures, not a production Farmer/Supplier trip. Manual checks: open an actual accepted order; confirm green Farmer pickup plus blue current position; pan then update/recenter; confirm the camera stays put until requested; complete pickup through the unchanged PIN flow; confirm amber saved delivery destination; deny permission/disable GPS/test map connectivity; verify readable fallback and external navigation; confirm terminal/ Farmer views contain no Supplier controls. Profile edits must leave destinations unchanged. Genuine long-distance trips will necessarily need wider bounds to show both endpoints.
