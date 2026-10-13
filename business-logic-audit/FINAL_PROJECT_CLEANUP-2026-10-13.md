# Final project cleanup audit - 2026-10-13

## Outcome and scope

Minimum safe cleanup for university submission/demo. No application logic, source resource, test, Gradle build configuration, manifest, Firebase configuration used by the app, Room schema/migration or security Rules was changed. No deployment, commit, push or database reset was performed. The working tree was clean before cleanup.

Inspection covered the repository root, all Android source sets/resources, build/configuration files, navigation, Hilt bindings, backend/tests, sample-data, audit documents, generated outputs and historical delivery snapshots. Deletion targets were resolved and checked to remain inside D:/Projects/AgroRoute before native PowerShell Remove-Item. A pre-cleanup SHA-256 snapshot protects 298 source/configuration/backend files; final verification follows below.

## Classification

| Category | Files/directories | Decision and reason |
|---|---|---|
| REQUIRED | app/src/main, debug, release; all res and manifests; test, testDebug, androidTest | Preserved byte-for-byte. Real application, integrated debug demo, release isolation and regression coverage. |
| REQUIRED | Room entities/DAOs/migrations; firestore.rules; storage.rules; app/google-services.json; .firebaserc; firebase.json; firebase.rules-test.json | Preserved. Persistence, authorization, service configuration and emulator reproduction. |
| REQUIRED | Gradle wrapper/JAR, version catalog, build scripts, settings, gradle.properties, local.properties and example; package.json/lockfiles, backend tsconfig | Preserved. Installed SDK/key configuration is private and not printed. Lockfiles recreate dependencies. |
| REQUIRED | functions/src and test; rules-tests source/scripts; sample-data | Preserved. Functions are configured and notification callables still referenced. Retained lifecycle implementation has tests and explicit historical purpose. |
| SAFE TO DELETE | Unreferenced intermediate audit logs, heap dumps, one-off firebase.premium-test.tmp.json, PowerShell ModuleAnalysisCache | Deleted. No active Gradle/manifest/navigation/DI reference; duplicate or temporary evidence. Real Firebase configurations remain. |
| GENERATED / RECREATABLE | Root/app build trees; Kotlin session caches; installed node_modules; functions/lib; experiment outputs and npm cache | Deleted as listed below. Gradle/SDK caches outside the repository were preserved for offline rebuilding. |
| GENERATED / RECREATABLE | .gradle and fresh validation build outputs | Retained for current tooling/validation; exclude from submission archive. They can be regenerated. |
| OPTIONAL / ARCHIVE | delivery historical ZIPs, before-snapshots, old map/location-smoke experiments, screenshots and packaging notes | Retained for manual archival. Unique historical evidence is not assumed disposable. settings.gradle.kts includes only :app; none of these experiments participates in the application build. |
| OPTIONAL / ARCHIVE | Existing audit Markdown, linked final logs/screenshots, four referenced build init scripts and two APK-isolation verifiers; historical root documentation | Retained. Reports reference these evidence/tooling files. They are not application inputs, but removing them would break documented reproduction/evidence. |
| SUSPICIOUS / DO NOT DELETE WITHOUT REVIEW | Apparently unused components, legacy service implementations, .freebuff/project-id and .idea project metadata | Retained. Textual non-use does not prove safe deletion for injected, preview or retained alternative code. |

## Deleted files and folders

Folders below were deleted recursively, including their stale APKs, generated classes, test results and intermediate resources. 61,087 files totaling 4.31 GiB were removed before rebuilding. This is gross removal, not net disk savings: fresh validation outputs are recreated.

| Deleted path | File count | MiB |
|---|---:|---:|
| `build` | 30,335 | 2092.16 |
| `app/build` | 6,742 | 389.26 |
| `.kotlin` | 6 | 0.07 |
| `Microsoft` | 1 | 0.01 |
| `java_pid16604.hprof` | 1 | 732.58 |
| `java_pid6304.hprof` | 1 | 759.56 |
| `firebase.premium-test.tmp.json` | 1 | 0.00 |
| `firestore-debug.log` | 1 | 0.00 |
| `rules-tests/node_modules` | 8,071 | 99.89 |
| `functions/node_modules` | 7,121 | 83.71 |
| `functions/lib` | 15 | 0.10 |
| `delivery/npm-cache` | 684 | 21.43 |
| `delivery/location-smoke/app/build` | 545 | 130.79 |
| `delivery/location-smoke/.gradle` | 16 | 0.73 |
| `business-logic-audit/listing-stock-android-final.log` | 1 | 0.01 |
| `business-logic-audit/listing-stock-build-checked.log` | 1 | 0.01 |
| `business-logic-audit/listing-stock-build-final.log` | 1 | 0.00 |
| `business-logic-audit/listing-stock-build-isolated.log` | 1 | 0.00 |
| `business-logic-audit/listing-stock-build-verified.log` | 1 | 0.00 |
| `business-logic-audit/listing-stock-build.log` | 1 | 0.00 |
| `business-logic-audit/listing-stock-rules-complete.log` | 1 | 0.00 |
| `business-logic-audit/listing-stock-rules-final-checked.log` | 1 | 0.08 |
| `business-logic-audit/listing-stock-rules-final.log` | 1 | 0.00 |
| `business-logic-audit/listing-stock-rules-verified.log` | 1 | 0.08 |
| `business-logic-audit/listing-stock-rules.log` | 1 | 0.13 |
| `business-logic-audit/local-demo-build.log` | 1 | 0.01 |
| `business-logic-audit/normal-demo-adapters-first.log` | 1 | 0.00 |
| `business-logic-audit/normal-demo-build-final.log` | 1 | 0.01 |
| `business-logic-audit/normal-demo-build.log` | 1 | 0.01 |
| `business-logic-audit/normal-demo-marketplace-retry.log` | 1 | 0.00 |
| `business-logic-audit/normal-demo-ui-first.log` | 1 | 0.00 |
| `business-logic-audit/order-map-build.log` | 1 | 0.01 |
| `business-logic-audit/order-map-connected.log` | 1 | 0.02 |
| `business-logic-audit/order-map-phone-install.log` | 1 | 0.00 |
| `business-logic-audit/order-map-preview-install.log` | 1 | 0.00 |
| `business-logic-audit/order-map-validation-final.log` | 1 | 0.01 |
| `business-logic-audit/spark-all-rules-run.log` | 1 | 1.33 |
| `business-logic-audit/spark-android-run.log` | 1 | 0.00 |
| `business-logic-audit/spark-device-run.log` | 1 | 0.01 |
| `business-logic-audit/spark-final-android-run.log` | 1 | 0.00 |
| `business-logic-audit/spark-final-emulator-run.log` | 1 | 1.84 |
| `business-logic-audit/spark-final-rules-run.log` | 1 | 1.45 |
| `business-logic-audit/spark-future-backend-run.log` | 1 | 0.00 |
| `business-logic-audit/spark-lifecycle-run.log` | 1 | 0.60 |
| `business-logic-audit/spark-protocol-run.log` | 1 | 0.00 |
| `business-logic-audit/spark-rules-run.log` | 1 | 0.75 |
| `delivery/baseline/working-files/.kotlin` | 5 | 0.05 |
| `delivery/baseline/working-files/rules-tests/node_modules` | 7,510 | 98.10 |
| `delivery/location-smoke/.kotlin` | 0 | 0.00 |

The tracked deletion set consists of 7,510 installed dependency files under rules-tests/node_modules, five Kotlin session files, the PowerShell cache and the temporary emulator configuration. Other removed artifacts were ignored/untracked. The existing node_modules ignore rule prevents regenerated dependency files being newly added. Restore Node dependencies with npm ci in rules-tests/functions; functions/lib is regenerated by npm run build. Node tests were not requested in this cleanup and are not counted as newly executed.

## Reference checks and review candidates

- All 31 named Screen routes have NavGraph references. Authentication/role navigation remains unchanged.
- No exact Kotlin/Java source duplicates were found. No Google Maps SDK integration was found in active Android source/dependencies; MapLibre/MapTiler classes remain. Historical Google Maps/map smoke artifacts live only in the retained delivery archive.
- No non-values resource file lacked a direct source/XML reference in the resource scan; no unused asset was established as safe to delete.
- Identical drawable agroroute.png/logo.png are both referenced by real screens/XML. Identical adaptive launcher/round-launcher XML files are both referenced by the manifest. These resources were kept.
- DemoActivity/DemoBrowser remain referenced by the debug manifest, DemoBrowserTest, DemoIsolationTest and the old isolation verifier. The separate launcher intent was already removed by the previous task. Removing that utility would require changing valid tests; it remains.
- OrderCompletionService is an unbound retained Functions-backed OrderLifecycle alternative; RepositoryModule binds SparkOrderLifecycle. AgroRouteFirebaseMessagingService has no active manifest registration; FirebaseNotificationService is the registered implementation. Both legacy source files are review/archive candidates, not removed in this source-preserving cleanup.
- GradientHeader, PhotoPicker, AnimatedLogoHeader and AppCard's AgroDetailRow/AgroChipRow have no discovered call sites in current source. Preview functions have no ordinary call sites by design. These are potential unused components, not proof of obsolete behavior; all remain.
- Hilt modules may have no textual callers because generated dependency wiring consumes their annotations. Local private data types are used inside their files. These were not mistaken for unused files.
- Import inspection found apparent unreferenced imports, such as MainActivity's NavGraph import, PremiumComponents' RoundedCornerShape and SnackbarHost's Spacer. Kotlin delegated-property getValue/setValue and operator imports can be implicitly used, so raw text counts are not a compiler verdict. No source/import cleanup was performed.
- The Gradle release configuration references app/proguard-rules.pro, which was already absent before cleanup. Release currently has minification disabled. No ProGuard file was deleted or fabricated; review this configuration if enabling R8 later.

## Tests retained and validation

All 50 Android Kotlin test source files remain: 37 test, 3 testDebug and 10 androidTest. Backend and Rules test source also remains. Full unit regression includes auth/navigation, Room-related dataset/isolation, order/maps/routing, Spark/PIN/scoring/reviews, listing/stock/expiry and notification behavior. Phone tests exercise actual Room persistence/migration, demo adapters/browser/UI and native order/map/picker presentation.

| Check | Result |
|---|---|
| Fresh debug APK | BUILD SUCCESSFUL. |
| Fresh unsigned release APK | BUILD SUCCESSFUL; unsigned artifact for submission/build verification. |
| Instrumentation APK | BUILD SUCCESSFUL. |
| Full Android unit suite | **261 passed**, zero failures/errors/skips across 40 suites. |
| Pixel 7 / Android 17 requested phone suites | **133 passed**, zero failures/skips in one clean run (338.68 seconds). |
| Artifact isolation | Debug-only demo boundary verified; release clean; one MainActivity launcher in each variant. |
| Protected files | All 298 pre-cleanup SHA-256 values match. |
| Whitespace | git diff --check passed. |

The unit run includes SparkOrderLifecycleTest (13), PickupPinTest (4), OrderPickupRepositoryTest (7), PointAwardingTest (3), ListingStockExpiryTest (6), HarvestListingRulesTest (30), OrderRequestFlowTest (18), map presentation/routing (26), AuthRoleRoutingTest (3), password reset (5), Farmer/Supplier navigation (8), and all retained repository/review/demo/location cases. These are subsets of the 261 total, not additional cases.

**394 distinct executed tests passed**: 261 unit + 133 phone. Phone breakdown: 9 normal-demo UI, 6 local adapters, 6 actual Room/migration, 2 demo browser, 72 order presentation, 6 native MapLibre, 8 pickup, 20 picker layout and 4 picker dialog. No repeated cases are counted. The unrelated PremiumComponentsTest source remains; the broader unfiltered instrumentation suite was not run. No production Firebase trip or deployment was performed.

Build evidence: [fresh build/unit log](final-cleanup-build.log). Unit XML: build/normal-demo/app/test-results/testDebugUnitTest. [Artifact isolation](final-cleanup-isolation.log). [Phone evidence](final-cleanup-phone.log). [Source integrity](final-cleanup-source-integrity.log).

Debug APK: app/build/outputs/apk/debug/app-debug.apk. SHA-256: 1C98CA96D6849F2E6289DA8DA9D7D64647FC4FE881AF51124238776AE4ECD99D.
Release APK: app/build/outputs/apk/release/app-release-unsigned.apk. SHA-256: 07DC1D6D9BFD4405DBA217EA946D3EB765E5B4637C0404B999DBEA6B43FA6211.
Both copies match their fresh build/normal-demo outputs. Rebuilt artifact hashes need not match older APKs because build metadata can differ despite unchanged source.

Reproduction uses the retained normal-demo-build.init.gradle to keep one fresh isolated output tree:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$env:GRADLE_USER_HOME='C:\Users\Hasitha\.gradle'
$env:ANDROID_USER_HOME='C:\Users\Hasitha\.android'
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:assembleDebugAndroidTest --offline --no-daemon --init-script business-logic-audit/normal-demo-build.init.gradle '-Pkotlin.compiler.execution.strategy=in-process'
python business-logic-audit/verify-normal-demo-isolation.py
```


Phone reproduction after installing the fresh debug/test APKs:

```powershell
$adb='C:\Users\Hasitha\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r 'build\normal-demo\app\outputs\apk\debug\app-debug.apk'
& $adb install -r 'build\normal-demo\app\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
& $adb shell am instrument -w -e delay_msec 1500 -e class 'com.agroroute.NormalDemoPresentationTest,com.agroroute.DemoAdaptersTest,com.agroroute.DemoDatabaseTest,com.agroroute.DemoBrowserTest,com.agroroute.OrderFlowPresentationTest,com.agroroute.OrderMapNativePresentationTest,com.agroroute.OrderPickupPresentationTest,com.agroroute.LocationPickerLayoutTest,com.agroroute.LocationPickerPresentationTest' com.agroroute.test/androidx.test.runner.AndroidJUnitRunner
```

No obsolete application code was removed. Only generated/temporary artifacts were deleted. The temporary hash inventory used during inspection was also removed after recording the passing integrity check.

## Final project structure

```text
AgroRoute/
  app/
    src/main/             application, resources, manifest
    src/debug/            isolated local demo and entry integration
    src/release/          normal production root/isolation
    src/test/             production unit regressions
    src/testDebug/        demo dataset/session/isolation units
    src/androidTest/      Room/demo/order/map/picker presentation tests
    build.gradle.kts
    google-services.json
  gradle/                 wrapper and version catalog
  functions/              TypeScript source, tests, config and lockfile
  rules-tests/            Rules tests, tools, config and lockfile
  sample-data/            retained emulator sample fixtures
  business-logic-audit/    reports, referenced evidence and reproduction tools
  delivery/               optional historical archive, outside active build
  build/normal-demo/       fresh generated validation outputs
  firestore.rules, storage.rules, firebase*.json
  Gradle wrapper/root configuration and project documentation
```

For a university submission ZIP, include the required project/source/configuration and selected reports. Exclude .git, .gradle, .idea, .kotlin, build, app/build and node_modules. Keep the wrapper JAR. Preserve local.properties locally; distribute its example rather than private workstation/map-key configuration. Archive delivery and older documentation/evidence separately if a smaller submission is desired. No optional historical source/archive was silently deleted.
