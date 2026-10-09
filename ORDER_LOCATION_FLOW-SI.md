# Order pickup / delivery snapshots — AgroRoute

## Analysis සහ schema තීරණය

Implementation එකට පෙර profiles, listing create/edit, bid/request placement, farmer acceptance transactions, Order/Bid/OrderRequest Room entities, FirestoreService serialization, details screens, SelectedLocation codec සහ picker පරීක්ෂා කළා. වෙනම order DTO/API layer එකක් නැහැ: Kotlin model එකම Room entity සහ Firestore `toObject`/write payload එක වේ. පෙර code backup `delivery/order-flow-before.zip` සහ rules backup `delivery/order-flow-before.rules` තුළ ඇත. Analysis record `delivery/order-flow-analysis.txt` වේ.

Profile location එක profile document එකට පමණක් save වේ. Listing location එක listing එකේම string එකකි. Fixed-price/pre-harvest request එකක් හෝ bid එකක් supplier දමයි; confirmed order එකක් සෑදෙන්නේ farmer accept කරන විට පමණි. පෙර code එක delivery location එක supplier submission වෙලාවේ ලබා නොගත් අතර bid pickup එක cached listing එකෙන් transaction එකට පෙර ගත්තා.

`SelectedLocation` හි existing `Address\n[latitude, longitude]` format එක coordinates ඇතුළුව සම්පූර්ණ location එකක් රඳවාගනී. ඒ නිසා listing/order සඳහා duplicate numeric columns සහ තවත් backfill එකක් අවශ්‍ය නැහැ. Existing location strings retain කර typed snapshot helpers එකතු කළා:

| අවශ්‍ය value | Order එකෙන් ලබාගන්නා ආකාරය |
| --- | --- |
| pickupAddress | `order.pickupSnapshot()?.address` |
| pickupLatitude | `order.pickupSnapshot()?.latitude` |
| pickupLongitude | `order.pickupSnapshot()?.longitude` |
| deliveryAddress | `order.deliverySnapshot()?.address` |
| deliveryLatitude | `order.deliverySnapshot()?.latitude` |
| deliveryLongitude | `order.deliverySnapshot()?.longitude` |

මේ values order එකේ own strings වලින් පමණක් parse කරයි; listing/profile lookup එකක් නැහැ. Firestore/Room actual fields `pickupLocation` සහ `deliveryLocation` වේ. Coordinate-only value එකකදී UI address වෙනුවට coordinates පෙන්වයි.

## Order එකට data යන ආකාරය

1. Farmer Create Listing හි map එකෙන් pickup pin/address තෝරයි. Publish කිරීමට valid coordinate suffix එක අවශ්‍යයි; local drafts හි incomplete locations තබාගත හැක.
2. Supplier fixed-price **Request Order** හෝ **Place Bid** screen එකේ delivery pin තෝරයි. Delivery pin නැතිව submit button enabled නොවේ. Listing pickup coordinates නැති නම් farmerගෙන් pickup pin එක update කරන ලෙස message එකක් පෙන්වයි.
3. Delivery encoded string එක එම specific `order_requests/{requestId}` හෝ `bids/{bidId}` document/Room row එකේ save වෙයි. Current supplier profile එකෙන් delivery auto-replace නොකරයි.
4. Farmer accept කරන transaction එක authoritative backend listing සහ pending request/bid කියවයි. Pickup snapshot එක එම මොහොතේ listing.location වෙතින්ද delivery snapshot එක saved request/bid.deliveryLocation වෙතින්ද ගනී. Cached order input locations trust නොකරයි.
5. Locations validate කර stock update, proposal ACCEPTED status සහ order create එක atomic transaction එකේම සිදුවේ. Coordinates නැති නම් order/stock writes කිසිවක් සිදු නොවේ. Firestore සාර්ථක වූ පසු exact persisted order Room cache එකට යයි.
6. Order create වීමෙන් පසු listing/profile edits එම order එකේ strings වෙනස් නොකරයි. Order-specific delivery editing දැනට තිබූ supplier action එකෙන් පමණක් කළ හැක; එය profile update එකක් නොවේ. Pickup editing order screen එකෙන් allow නොකරයි.

Pending proposal එක supplier submit කළ පසු listing pickup වෙනස් වුණොත් order create කරන acceptance මොහොතේ pickup එක copy කරයි. Order සෑදීමෙන් පසුව එය freeze වේ. Supplier delivery එක proposal submit කළ අවස්ථාවේම save වෙයි.

## Database / backend compatibility

Room version **6 → 7**:

```sql
ALTER TABLE bids ADD COLUMN deliveryLocation TEXT NOT NULL DEFAULT '';
ALTER TABLE order_requests ADD COLUMN deliveryLocation TEXT NOT NULL DEFAULT '';
```

Entities වල `@ColumnInfo(defaultValue = "''")` annotation එක සහ registered MIGRATION_6_7 එක ගැළපේ. Orders/listings/profile tables rewrite හෝ delete කරන්නේ නැහැ. 1→7 migration chain පවත්වා ඇත. Unknown schema එකකට destructive fallback එක ඉවත් කළ නිසා unsupported version එකක app එක fail කරයි; data delete කිරීමෙන් rebuild නොකරයි. App upgrade test එකේ uninstall/Clear Data කරන්න එපා.

Firestore schemaless documents සඳහා new Bid/OrderRequest `deliveryLocation` string පමණක් එකතු වේ. Missing old field එක Kotlin default `""` ලෙස deserialize වේ. Existing Order fields/names සහ Cloud Functions payloads වෙනස් නොකළා. Functions deployment මේ feature එක සඳහා අවශ්‍ය නැහැ.

Updated `firestore.rules`:

- New listing publish සහ new bid/request සඳහා bounded coordinate-bearing pickup/delivery අවශ්‍යයි.
- Order pickup listing එකේ location එකට සහ delivery accepted request/bid එකේ delivery එකට exact match විය යුතුයි.
- Existing order pickup immutable වේ. Existing supplier-only explicit delivery update එක valid map pin එකක් සහ permitted order status එකක් ඉල්ලයි.
- Proposal location පසුව status update එකෙන් mutate කරන්න බැහැ. Legacy proposals cancel/reject කිරීමට partial status/timestamp writes භාවිත කරන නිසා missing delivery field එක add නොවේ.

Old address-only orders readable වේ: address text පෙන්වයි, unavailable-coordinate message එකක් දෙනවා, guessed profile pins හෝ automatic geocoding backfill එකක් යොදාගන්නේ නැහැ. Supplier navigation coordinates නැති විට disabled වේ. Empty/malformed location values crash නොකරයි. Old requests/bids delivery pin නැති නිසා accept කළ නොහැක: supplier ඒවා cancel/withdraw කර valid delivery pin සමඟ නැවත place කළ යුතුයි. Old listing එක new requests ලැබීමට farmer map pickup එක update කළ යුතුයි.

## Map preview සහ navigation

`LocationMap` එක shared native MapLibre renderer එකයි. Existing picker එකත් previews එකත් එය භාවිත කරයි; map lifecycle/logging/markers සඳහා duplicate implementations නැහැ. Existing `BuildConfig.MAPTILER_API_KEY` setup එක එලෙසම භාවිත කරයි. Google Maps SDK/dependency/API key කිසිවක් එක් කර නැහැ.

Supplier details: saved farmer pickup marker, pickup/delivery addresses, GPS fix available නම් blue dot, **Show my current location** සහ **Navigate to Farmer**. Location permission button එකෙන් ඉල්ලයි; permission දැනට granted නම් details load වීමේදී current fix ඉල්ලයි. Blue dot සහ destination දෙකම preview bounds තුළ frame කරයි. Permission deny/GPS unavailable වුණත් saved map destination/navigation usable වේ.

Farmer details: saved supplier delivery marker සහ both addresses. Preview tap එකෙන් saved order වෙනස් නොවේ. App green theme එකේ rounded cards/map corners/buttons භාවිත කරයි.

Navigation saved pickup coordinates යොදා `google.navigation:q=latitude,longitude&mode=d` intent එක Google Maps package එකට යවයි. Handler නැති/disabled නම් HTTPS Maps directions URL එකටත් ඉන්පසු geo intent එකටත් fallback කරයි. No maps/browser handler නම් UI message එකක් දෙනවා. Google Maps API key අවශ්‍ය නැහැ. [Official Google Maps intent documentation](https://developer.android.com/guide/components/google-maps-intents).

## Validation

- Android unit tests **157 passed**, failures/errors **0**.
- Kotlin/Java compile සහ **assembleDebug BUILD SUCCESSFUL**. JBR21, Kotlin 2.0.21, existing Gradle/AGP භාවිත කළා. APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Firestore Emulator rules tests **97 passed**, failures **0**. Request/bid acceptance, destination tampering, missing/out-of-range pins, old records සහ profile/listing edits පසු frozen order locations ඇතුළත්.
- Actual SQLite 6→7 SQL tables 7 මත execute කර old rows, order strings සහ indexes රැකෙන බවත් generated fresh Room v7 schema සමඟ columns/defaults ගැළපෙන බවත් පරීක්ෂා කළා.
- Local demo fixture documents 8ක් updated delivery snapshots සමඟ seed/validate කළා. Live Firebase/backend writes/deployment කළේ නැහැ.
- Logs/verification: `delivery/order-flow-final-build.log`, `delivery/order-flow-rules-final.log`, `delivery/order-flow-migration.log`, `delivery/order-flow-sample.log`, `delivery/order-flow-verification.json`.

Initial unit failures වූයේ පෙර valid-fixture listings/bids/requests තුළ coordinates/delivery නොතිබීම නිසායි; ඒ fixtures නව mandatory flow එකට ගැළපෙන ලෙස update කර නැවත tests run කළා. New tests actual repository transactions සහ legacy failure/stock preservation පරීක්ෂා කරනවා.

මෙම නව order-details/navigation flow එක physical device එකේ end-to-end ධාවනය කර නැහැ. SQLite පරීක්ෂාව host SQLite මතයි; එය device instrumentation test එකක් ලෙස වාර්තා නොකරයි. Device verification පහත පියවර අනුව කරන්න.

## ඔබ manually කළ යුතු exact steps

1. Existing app/data backup එකක් ගන්න. Upgrade කර Room data retention පරීක්ෂා කිරීමට app uninstall/Clear Data නොකරන්න.
2. Existing local.properties/MapTiler key තබාගෙන Android Studio Gradle Sync කර build/run කරන්න. Kotlin/Gradle/AGP upgrade අවශ්‍ය නැහැ.
3. Updated rules පළමුව ඔබගේ **test Firebase project** එකට යොදන්න. Root තුළ `firebase deploy --only firestore:rules --project YOUR_TEST_PROJECT_ID` ධාවනය කිරීමට පෙර placeholder එක intended test project ID එකෙන් replace කරන්න. මෙහි live rules deploy කර නැහැ. New mandatory locations නොයවන old clients updated rules සමඟ new orders place කළ නොහැකි නිසා app/rules rollout එක සම්බන්ධීකරණය කරන්න.
4. Farmer account එකෙන් ready fixed-price listing එකක් create කරන්න. Map එකේ pickup **A** select කර address label එක බලන්න; Publish කරන්න. Listing location string එක `[latitude, longitude]` suffix එක දරන බව test backend එකේ බලන්න.
5. Supplier එකෙන් එම listing එකට Request Order යන්න. Delivery තෝරා නැති විට button disabled බව බලන්න. Delivery **B** select කර request submit කරන්න. Request document එකේ deliveryLocation B බව බලන්න.
6. Farmer request accept කරන්න. Order document එකේ pickup A / delivery B snapshots ඇති බවත් stock/quantity flow පෙර පරිදි බවත් බලන්න.
7. Supplier details හි pickup A marker සහ both addresses බලන්න. **Show my current location** ඔබා permission allow කරන්න: blue dot සහ destination දෙකම පෙනිය යුතුයි. Approximate permission/GPS disabled අවස්ථාත් පරීක්ෂා කරන්න.
8. **Navigate to Farmer** ඔබන්න. Google Maps installed නම් A coordinates වෙත navigation යා යුතුයි. වෙනම test device/emulator එකක Google Maps නොමැති අවස්ථාවෙන් browser/geo fallback පරීක්ෂා කරන්න. ඔබගේ personal Maps app uninstall කිරීම අවශ්‍ය නැහැ.
9. Farmer details හි delivery B marker/address බලන්න.
10. Farmer profile location **C**, listing pickup **D**, supplier profile location **E** ලෙස වෙනස් කරන්න. Order sync/reopen කළ පසු පැරණි order තවම pickup A / delivery B බව බලන්න.
11. Same supplierගෙන් වෙනත් request එකක් delivery **F** සමඟ place/accept කරන්න. Orders දෙකේ delivery B/F වෙන වෙනම තිබිය යුතුයි.
12. Bidding-enabled ready listing එකක bid එක delivery pin සමඟ place කර farmer accept කරන්න. එම order එකට authoritative listing pickup සහ saved bid delivery copy වී ඇති බව පරීක්ෂා කරන්න.
13. Pre-harvest fixed-price request එකක් delivery pin සමඟ submit කරන්න. Farmer harvest ready කළ පසු accept කරන්න; same location flow පවත්වා තිබිය යුතුයි.
14. Old address-only order එක open කරන්න: address පෙන්විය යුතුයි, crash නොවිය යුතුයි, unavailable pickup coordinates සඳහා navigation disabled විය යුතුයි. Supplier explicit delivery edit එකෙන් ඒ order එකේ delivery pin repair කළ හැක.
15. Old coordinate-less listing එකට request/bid place කිරීමට missing-pickup message ලැබිය යුතුයි. Farmer listing edit එකෙන් map pin තෝරා update කර නැවත උත්සාහ කරන්න.
16. Picker Back/cancel කළ විට request/bid delivery තේරීම නොවෙනස් බවත් permission denied වුණත් map selection usable බවත් බලන්න. App restart කිරීමෙන් saved confirmed order locations රැකෙන බව බලන්න.
17. Debug tests: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`. SQLite check: `python rules-tests/validate-room-location-migration.py`. Emulator running නම් PowerShell තුළ `$env:FIRESTORE_EMULATOR_HOST='127.0.0.1:8082'` සකසා `node --test rules-tests/firestore.rules.test.mjs` run කරන්න.

## වෙනස් කළ files සියල්ල

| File | වෙනස |
| --- | --- |
| [app/src/main/java/com/agroroute/core/components/LocationMap.kt](app/src/main/java/com/agroroute/core/components/LocationMap.kt) | Picker සහ order preview දෙකටම shared MapLibre renderer, markers, lifecycle සහ GPS/destination framing. |
| [app/src/main/java/com/agroroute/core/components/LocationPickerDialog.kt](app/src/main/java/com/agroroute/core/components/LocationPickerDialog.kt) | Existing picker shared LocationMap භාවිත කරන ලෙස සම්බන්ධ කළා; search/save callbacks පවත්වා ඇත. |
| [app/src/main/java/com/agroroute/core/components/OrderLocationSection.kt](app/src/main/java/com/agroroute/core/components/OrderLocationSection.kt) | Request/bid delivery chooser සහ role-specific order map/address/navigation card. |
| [app/src/main/java/com/agroroute/core/database/DatabaseMigrations.kt](app/src/main/java/com/agroroute/core/database/DatabaseMigrations.kt) | Data-preserving Room MIGRATION_6_7: delivery columns දෙකක්. |
| [app/src/main/java/com/agroroute/core/di/DatabaseModule.kt](app/src/main/java/com/agroroute/core/di/DatabaseModule.kt) | 6→7 migration register කළා; destructive fallback ඉවත් කළා. |
| [app/src/main/java/com/agroroute/core/location/OrderNavigation.kt](app/src/main/java/com/agroroute/core/location/OrderNavigation.kt) | Order coordinate navigation intent සහ HTTPS/geo fallbacks. |
| [app/src/main/java/com/agroroute/core/model/Bid.kt](app/src/main/java/com/agroroute/core/model/Bid.kt) | Default-empty persisted deliveryLocation field එක එක් කළා. |
| [app/src/main/java/com/agroroute/core/model/OrderLocations.kt](app/src/main/java/com/agroroute/core/model/OrderLocations.kt) | Order-only typed snapshot readers සහ coordinate validation/copy helpers. |
| [app/src/main/java/com/agroroute/core/model/OrderRequest.kt](app/src/main/java/com/agroroute/core/model/OrderRequest.kt) | Default-empty persisted deliveryLocation field එක එක් කළා. |
| [app/src/main/java/com/agroroute/core/repository/BidRepositoryImpl.kt](app/src/main/java/com/agroroute/core/repository/BidRepositoryImpl.kt) | Submission destination guards සහ backward-compatible partial status writes. |
| [app/src/main/java/com/agroroute/core/repository/HarvestListingRepositoryImpl.kt](app/src/main/java/com/agroroute/core/repository/HarvestListingRepositoryImpl.kt) | Published listing pickup coordinates validate කරයි. |
| [app/src/main/java/com/agroroute/core/repository/OrderRepositoryImpl.kt](app/src/main/java/com/agroroute/core/repository/OrderRepositoryImpl.kt) | Atomic authoritative pickup/delivery snapshots, matching Room cache, explicit delivery validation. |
| [app/src/main/java/com/agroroute/core/repository/OrderRequestRepositoryImpl.kt](app/src/main/java/com/agroroute/core/repository/OrderRequestRepositoryImpl.kt) | Submission destination guards සහ legacy-safe partial status writes. |
| [app/src/main/java/com/agroroute/core/util/Constants.kt](app/src/main/java/com/agroroute/core/util/Constants.kt) | Room database version 7. |
| [app/src/main/java/com/agroroute/features/farmer/bids/AcceptBidUseCase.kt](app/src/main/java/com/agroroute/features/farmer/bids/AcceptBidUseCase.kt) | Bid-selected delivery එක acceptance input එකට ලබා දෙයි; repository authoritative copy කරයි. |
| [app/src/main/java/com/agroroute/features/farmer/listing/CreateListingUseCase.kt](app/src/main/java/com/agroroute/features/farmer/listing/CreateListingUseCase.kt) | Pickup pin නැති listing publish කිරීමෙන් පෙර clear validation message. |
| [app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderDetailScreen.kt](app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderDetailScreen.kt) | Order-specific supplier delivery preview සහ location section. |
| [app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidScreen.kt](app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidScreen.kt) | Required delivery picker; missing pickup message/submit gating. |
| [app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidUseCase.kt](app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidUseCase.kt) | Submission pickup/delivery validation. |
| [app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestScreen.kt](app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestScreen.kt) | Required delivery picker; missing pickup message/submit gating. |
| [app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestUseCase.kt](app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestUseCase.kt) | Direct/pre-harvest request pickup/delivery validation. |
| [app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderDetailScreen.kt](app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderDetailScreen.kt) | Saved farmer pickup preview, current-position dot සහ navigation section. |
| [app/src/test/java/com/agroroute/core/repository/OrderLocationFlowTest.kt](app/src/test/java/com/agroroute/core/repository/OrderLocationFlowTest.kt) | Actual repository snapshot transactions, independence, legacy rejection/no stock write, locale-safe navigation tests. |
| [app/src/test/java/com/agroroute/features/farmer/listing/CloudinaryUploadTest.kt](app/src/test/java/com/agroroute/features/farmer/listing/CloudinaryUploadTest.kt) | Existing valid listing fixtures නව pickup requirement එකට ගැළපෙන ලෙස update කළා. |
| [app/src/test/java/com/agroroute/features/farmer/listing/HarvestListingRulesTest.kt](app/src/test/java/com/agroroute/features/farmer/listing/HarvestListingRulesTest.kt) | Existing listing/bid fixtures coordinate/delivery requirements අනුව update කළා. |
| [app/src/test/java/com/agroroute/features/orderrequests/OrderRequestFlowTest.kt](app/src/test/java/com/agroroute/features/orderrequests/OrderRequestFlowTest.kt) | Valid fixtures update සහ missing pickup/delivery submission test. |
| [DELIVERY_REPORT-SI.md](DELIVERY_REPORT-SI.md) | Latest order flow සහ Room7 report pointer/compatibility notes. |
| [firestore.rules](firestore.rules) | Destination requirements/source matching සහ bounded-pin validation; existing ownership/status protections පවත්වා ඇත. |
| [INTEGRATION_README.txt](INTEGRATION_README.txt) | Current order flow report link. |
| [MAPTILER_SETUP-SI.md](MAPTILER_SETUP-SI.md) | Order-flow/Room7 report link; prior map migration report retained. |
| [ORDER_LOCATION_FLOW-SI.md](ORDER_LOCATION_FLOW-SI.md) | Analysis, data flow/schema decision, verification, file-by-file changes සහ manual steps. |
| [rules-tests/firestore.rules.test.mjs](rules-tests/firestore.rules.test.mjs) | New location fixtures සහ snapshot/tampering/legacy/independence security coverage. |
| [rules-tests/validate-room-location-migration.py](rules-tests/validate-room-location-migration.py) | Actual SQL data preservation සහ generated Room schema comparison. |
| [sample-data/firestore.json](sample-data/firestore.json) | Dummy bids/requests/orders සඳහා order-specific delivery fixtures. |
