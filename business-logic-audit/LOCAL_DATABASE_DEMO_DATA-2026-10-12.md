# Local Room demo-data audit ? 2026-10-12

## Existing architecture and data flow

Inspection before implementation: Firebase Auth supplies the real UID ? AuthRepository observes that UID's Room profile and restores its Firestore document ? production repositories fetch/listen to Firestore and upsert the existing Room cache ? DAO Flows feed ViewModels. Server transactions/writes occur before their successful results are mirrored to Room. There is no Room-to-Firestore outbox. Listing subscriptions synchronize remotely changed stock and retain history. Bids, requests, participant orders, reviews and leaderboard profiles have their existing explicit synchronization paths. Notifications use SharedPreferences plus the existing remote inbox, not a Room entity. The existing sample-data/firestore.json and emulator seeding script are unrelated and were not used or changed.

AppDatabase is version **8**, named agroroute_db in production, with UserProfile, HarvestListing, Bid, OrderRequest, Order, OrderCompletionRecord and Review. Existing seven DAOs and Converters are reused. All 1?8 migrations remain unchanged/registered; no schema/version change, destructive migration or new DAO/entity was introduced.

## Seeder and isolation

All implementation is under src/debug. DemoDatabase opens the same AppDatabase class/schema/DAOs in **agroroute_local_demo_db**, with the existing migration chain. This is a separate debug local file, not a second database architecture or a replacement production cache. This choice avoids changing production query/session/sync semantics: demo profiles cannot become the real current user, sample listings cannot enter the live marketplace, and production synchronization cannot overwrite demo rows. The filename is fixed, never supplied by an Intent.

DemoDataSeeder requires BuildConfig.DEBUG. Loading is explicit and atomic using Room.withTransaction, existing DAO upserts and deterministic demo_ IDs. Repeated loading preserves the first seed's timestamp anchor, deadlines, snapshots and row counts. Clear uses a transaction and literal substr(primaryKey,1,5) = 'demo_' across the seven existing tables, including any demo completion records. It never clears all tables, deletes a database file, alters a real cache, calls a repository or accesses Firebase. Non-demo rows in the demo file are preserved too.

There is no automatic seeding hook, auth/session switch, fake account, business-action ViewModel or transaction control in the utility. Debug source-set classes and the launcher manifest are absent from release. The existing Application notification bootstrap remains unchanged: if a real account is signed in it may perform its normal token/inbox work, but it never receives demo IDs/data. Maps retain their normal tile/GPS/external-navigation behavior; local-only refers to the sample records, not downloaded basemap tiles.

## Sample records

- **6 profiles:** Nimal Perera / Green Valley Farm / Kurunegala / 120 points; Kasun Bandara / Sunrise Agro Farm / Dambadeniya / 90; Saman Kumara / Fresh Harvest Farm / Giriulla / 105; FreshMart Traders / Kurunegala / 80; Lanka Fresh Distributors / Negombo / 70; City Vegetable Centre / Colombo / 110. Fictional names, reserved example.invalid emails, empty phone/image fields. UserProfile has no farm-name or stored rating field; farms appear in location addresses, and no rating/schema field was invented. Points are illustrative cached totals, not newly awarded scoring events.
- **10 listings:** Tomato 100kg bidding LKR320; Carrot 75kg fixed400; Cabbage150kg bidding280; Potato50kg fixed450; Beans80kg bidding380; Pumpkin100kg fixed180; Brinjal60kg bidding240; Chilli10kg fixed650; Onion90kg bidding420 (expired); Banana estimated120kg fixed200 (EXPECTED, future harvest, zero confirmed available stock). Existing enums only. Potato and Chilli are fully sold out/FULFILLED; other reservations leave consistent remaining stock.
- **9 bids:** Tomato A40/B30/C70 pending, plus a rejected20; accepted Cabbage40, Beans30, Brinjal25; pending Cabbage120 exceeds its current110 remaining and demonstrates the real derived insufficient-stock explanation; pending Brinjal20 fits its35 remaining. No proposal status is changed by presentation.
- **6 direct requests:** accepted Carrot20, Potato50, Pumpkin20 and Chilli10; pending Carrot15 and Banana40 pre-harvest. Prices, owners, crops, units and destinations match their listing/proposal relations.
- **7 orders:** Carrot20 and Brinjal25 AWAITING_PICKUP; Cabbage40 IN_TRANSIT; Potato50, Beans30 and Chilli10 DELIVERED; Pumpkin20 CANCELLED with restored stock. IDs equal the accepted source proposal ID, preserving deterministic order conventions; bid orders retain bidId, direct orders use blank bidId. All snapshots are explicit saved strings. Waiting Carrot pickup is Kurunegala and delivery Giriulla; transit Cabbage pickup is Dambadeniya and delivery Kurunegala. Profile edits are never consulted to reconstruct destinations. Only non-cancelled orders contribute to sold/reserved quantity; available plus sold equals READY total quantity. No PIN/verifier, score event or completion receipt is fabricated.
- **4 reviews:** two reciprocal participant pairs on delivered Potato/Chilli orders, ratings4?5, deterministic orderId_reviewerId IDs, one per participant/order.
- **Notifications:** none seeded because there is no existing Room notification entity. The existing notification architecture is unchanged.

All profile/listing/proposal/order coordinates are finite, bounded, nonzero Sri Lankan sample locations. Stored format remains Address followed by newline and [latitude, longitude], through SelectedLocation.toStoredText(). No remote images are required.

## Load, clear and offline browsing

Install the debug APK. Open the separate **AgroRoute Local Demo** launcher entry (no login required). Tap **Load Demo Data**. Browse Profiles, Listings, Bids, Requests, Orders and Reviews. Orders provide **View saved order map** and Supplier/Farmer map choices using the existing stage-aware OrderLocationSection. There are no accept, pickup, cancel, scoring or review-submit actions. The normal AgroRoute launcher and real authenticated flows are unchanged.

Tap **Clear Demo Data** to remove only demo_ records from the debug local file. Loading again establishes a fresh date anchor, useful after the 30-day sample availability window. Cached text/relations remain browsable offline and across process restarts; basemap tiles, external directions and GPS retain their normal availability requirements. No remote operation is claimed successful offline. Tomato pending bids are static fixtures; this utility does not simulate accepting/cancelling them. Real business transactions remain in the normal authenticated app.

## Validation

Final validation completed on 2026-10-13 (requested report filename retained):

| Check | Result |
|---|---|
| Full Android unit suite | **251 passed**, zero failures/errors/skips: 239 retained business/repository/Spark/PIN/stock/expiry/location/maps cases plus 10 dataset and 2 isolation cases. |
| Pixel 7 / Android 17 Room tests | **6 passed**: all entity persistence, idempotence/deadline/snapshot stability, namespace-only clear preserving non-demo rows in all seven tables, empty database/no automatic seed/version8, real Room reopen/migration7?8 with legacy bid preservation, and direct migration SQL nullable-quantity preservation. |
| Pixel 7 browser tests | **2 passed**: explicit load displays offline local records without a session or transaction buttons; explicit clear returns to the empty browser. Both use in-memory test databases. |
| Debug APK, instrumentation APK, release APK | **BUILD SUCCESSFUL**. Release APK is unsigned, used for isolation verification only. |
| Built artifact isolation | Debug DEX/merged manifest contain demo package/launcher; release DEX/merged manifest contain neither. Production source has no demo dependency/seed hook. |
| Source scope/whitespace | git diff --check passed; all implementation is new debug source, tests or audit tooling. No existing production files changed. |

**259 executed tests passed**. APK/manifest inspections are checks, not extra test cases. Existing migration1?7 implementations were inspected and remain registered/unchanged; this task executes the relevant7?8 migration, not newly invented historical schema fixtures. Existing map/lifecycle/picker phone suites were not rerun for this debug-only addition; their unit regressions are included above and their source is unchanged.

Final evidence: [build log](local-demo-build-final.log), [phone test log](local-demo-phone.log), [APK isolation log](local-demo-isolation.log). Unit XML: build/local-demo/app/test-results/testDebugUnitTest. Initial build compilation saw the earlier private browser function while the UI test access was being added; the final rebuilt source and complete8-case phone run pass. No production behavior or assertions were weakened.

Debug APK: app/build/outputs/apk/debug/app-debug.apk, copied from build/local-demo/app/outputs/apk/debug/app-debug.apk with identical SHA-256: 75BA2E5F6ED582AB8EA25132E64133A9BECB072581047DB0B850EF77EBA466FE. Instrumentation APK: build/local-demo/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk. Release artifact: build/local-demo/app/outputs/apk/release/app-release-unsigned.apk. The debug/test APKs were installed on the connected phone with install -r, preserving existing app data; tests use dedicated/in-memory databases.

Reproduce builds with the installed Java21/SDK and cached dependencies:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\Hasitha\.gradle'
$env:ANDROID_USER_HOME='C:\Users\Hasitha\.android'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:assembleRelease --offline --no-daemon --init-script business-logic-audit/local-demo-build.init.gradle '-Pkotlin.compiler.execution.strategy=in-process'
python business-logic-audit/verify-local-demo-isolation.py
```

Phone reproduction after installing the above debug/test APKs:

```powershell
$env:ANDROID_USER_HOME='C:\Users\Hasitha\.android'
$adb='C:\Users\Hasitha\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r 'build\local-demo\app\outputs\apk\debug\app-debug.apk'
& $adb install -r 'build\local-demo\app\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
& $adb shell am instrument -w -e delay_msec 750 -e class 'com.agroroute.DemoDatabaseTest,com.agroroute.DemoBrowserTest' com.agroroute.test/androidx.test.runner.AndroidJUnitRunner
```

## Remaining limitations

No Firebase deployment, index, migration, map-key change, remote sample upload or fake authentication account is required. Samples are intentionally isolated from the normal marketplace and signed-in Farmer/Supplier histories; use the dedicated local-demo launcher to browse them. No offline order acceptance, PIN handover, cancellation simulation or scoring/review submission is implemented. Phone validation covers persistence/migration and Load/Clear browsing, not a production trip or fresh native map visual QA. The unchanged map component already has its separate native-map audit; map tiles/external road navigation require normal connectivity and there is still no configured embedded routing provider. No notifications are seeded, and aggregate profile ratings are not stored because the current schema has no such field. Sample deadlines are anchored to the initial load; clear and reload to refresh the dated fixtures after expiry.

## Files changed

- app/src/debug/AndroidManifest.xml
- app/src/debug/java/com/agroroute/demo/DemoDataset.kt
- app/src/debug/java/com/agroroute/demo/DemoDataSeeder.kt
- app/src/debug/java/com/agroroute/demo/DemoActivity.kt
- app/src/testDebug/java/com/agroroute/demo/DemoDatasetTest.kt
- app/src/testDebug/java/com/agroroute/demo/DemoIsolationTest.kt
- app/src/androidTest/java/com/agroroute/DemoDatabaseTest.kt
- app/src/androidTest/java/com/agroroute/DemoBrowserTest.kt
- business-logic-audit/local-demo-build.init.gradle
- business-logic-audit/verify-local-demo-isolation.py
- This report and validation logs.

No production source, schema, repository, Firebase configuration, Rules, lifecycle, score, PIN, notification, expiry, stock, review or map/picker implementation changed. Nothing was deployed.
