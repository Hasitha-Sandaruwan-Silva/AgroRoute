# AgroRoute — ව්‍යාපාරික තර්කන විගණනය (A සිට Z)

**විගණන දිනය:** 2026-10-03
**විගණනය කළ branch එක:** `v3`
**විගණනය කළ commit එක:** `899b893` ("beffore editing") — වාර්තාව ලියන විට working tree එක **පිරිසිදුයි**
**විෂය පථය:** පරීක්ෂා කිරීම සහ සත්‍යාපනය පමණි. **App source, tests, Gradle, Firebase/Cloudinary config, schemas, rules කිසිවක් වෙනස් කර නැත.** කිසිවක් deploy, commit, push, publish කර නැත. මෙම වාර්තාව පමණක් නිර්මාණය කළ එකම ගොනුවයි.

> ⚠️ **විගණන කාලයේ repository තත්ත්වය පිළිබඳ සටහන.** විගණනය ආරම්භ කරන විට `git status` මගින් **commit නොකළ** වෙනස් කළ ගොනු පහක් (`FarmerOrderRequestsViewModel.kt`, `OrderRequestUseCases.kt`, `OrderRequestFlowTest.kt`, `firestore.rules`, `rules-tests/firestore.rules.test.mjs`) සහ track නොකළ `firestore-debug.log` එකක් පෙන්නුවා. **විගණනය අතරතුර, බාහිර පාර්ශවයක් (user/IDE — මෙම විගණනය නොවේ) `899b893` commit එක සාදා** එම වෙනස්කම් සහ `firestore-debug.log` commit කළා. දැන් working tree එක පිරිසිදු වන අතර `v3` යනු `origin/v3` ට වඩා commit 1කින් ඉදිරියෙනි (push කර නැත). පහත සියලු සොයාගැනීම් විස්තර කරන්නේ **දැනට commit කර ඇති තත්ත්වය** (`899b893`) ගැනයි; එය විගණනය ආරම්භයේ තිබූ working tree එකට අන්තර්ගතයෙන් සමානයි. මෙම විගණනය කිසිදු commit එකක් සාදා, වෙනස් කර, revert කර හෝ track කළ ගොනුවක් වෙනස් කර නැත.

---

## A. විධායක සාරාංශය

AgroRoute යනු **ගොවීන්** (අස්වැන්න ලැයිස්තුගත කිරීම, bids/order-requests සමාලෝචනය, orders ඉටු කිරීම) සහ **සැපයුම්කරුවන්** (marketplace බැලීම, bid කිරීම හෝ ස්ථාවර මිල order ඉල්ලීම්, orders ලුහුබැඳීම) සම්බන්ධ කරන Kotlin/Jetpack-Compose Android app එකකි (MVVM + Repository + Hilt + Room + Firebase). Code base එක හොඳින් සැකසී ඇති අතර **මූලික මුදල්/තොග ගනුදෙනු මාර්ග client code එකේ නිවැරදිව ක්‍රියාත්මක කර ඇත.** කෙසේ වෙතත්, එක **විවේචනාත්මක commit කළ අසංගතතාවයක්** සහ අර්ධ/සත්‍යාපනය නොකළ ප්‍රදේශ කිහිපයක් තිබේ.

### මුල සිට අග දක්වා ක්‍රියාත්මක වන දේ (code මට්ටමින්; live service සත්‍යාපනය කර නැත)
- **Authentication සහ roles** — email/password register+login, පළමු වරට role තේරීම සමඟ Google sign-in, Firestore `users/{uid}` + Room හි role persistence, client (`AuthRepositoryImpl.updateProfile` :141) සහ rules (`firestore.rules` :60) යන දෙකෙහිම **role වෙනස් කළ නොහැකි බව** බලාත්මකයි. Role-based start destination සහ route guards (`NavGraph.kt` :60-100).
- **ගොවි listings (READY අස්වැන්න)** — සෑදීම/validate/publish, edit, close, bidding toggle, delete, draft (local-only). Ownership client-side සහ rules දෙකෙහිම බලාත්මකයි. Firestore වලට persist වී පසුව Room වලට mirror වේ.
- **සැපයුම්කරු marketplace** — තමන් bid කළ ඒවා පමණක් නොව **සුදුසුකම් ලත් සියලු OPEN published listings** දකී; `isEligibleForMarketplace()` filter එක; search/filter; `biddingEnabled` අනුව Place-Bid හෝ Place-Order-Request වෙත නිවැරදි routing.
- **Bidding (ගොවි පැත්ත)** — bid දැමීම (සැපයුම්කරු), ගොවි සමාලෝචනය/පිළිගැනීම/ප්‍රතික්ෂේපය; **පිළිගැනීම Firestore transaction එකක් ක්‍රියාත්මක කරයි** — bid→ACCEPTED, `recordSale()` මගින් තොගය හරියටම එක් වරක් අඩු කිරීම, සහ ආශ්‍රිත order එක atomically සෑදීම (`OrderRepositoryImpl.acceptBidAndCreateOrder` :67-110). Transaction තුළ `bid.status == PENDING` guard එක මගින් දෙවරක් පිළිගැනීම වළක්වයි.
- **ස්ථාවර මිල order requests** — සැපයුම්කරු request දමයි (තොගය/ඇස්තමේන්තුවට එරෙහිව validate කරයි), ගොවි පිළිගනී (transaction මගින් order සෑදීම + තොගය එක් වරක් අඩු කිරීම + request ACCEPTED කිරීම) හෝ ප්‍රතික්ෂේප කරයි; සැපයුම්කරුට cancel කළ හැක. **තොගය වෙනස් වන්නේ පිළිගැනීමේදී පමණි** (`OrderRepositoryImpl.acceptOrderRequestAndCreateOrder` :112-170).
- **Orders සහ ඉටු කිරීම** — ownership filtering සහිත ගොවි/සැපයුම්කරු order lists + details; තත්ත්ව මාරුවීම් AWAITING_PICKUP→IN_TRANSIT→DELIVERED (→CANCELLED), cancel කළ විට `releaseSale()` මගින් තොගය **නිදහස්** කිරීම (`OrderRepositoryImpl.updateOrderStatus` :172-212).
- **Listing ඡායාරූප** — Android `content://` URI හැසිරවීම, Cloudinary **unsigned** multipart upload (app එකේ API secret නැත), progress/retry/error states, ලැබෙන `secure_url` එක `imageUrls` හි සුරැකීම; පැරණි Firebase Storage URLs තවමත් Coil හරහා පෙන්වයි.

### අර්ධ වශයෙන් ඇති දේ
- **සැපයුම්කරු bid කළමනාකරණය** — `getBidsBySupplier()` සහ `withdrawBid()` data layer එකේ ඇත (`BidRepositoryImpl` :39, :103) නමුත් **කිසිදු screen/ViewModel එකක් ඒවා භාවිතා නොකරයි.** **"My Bids" screen එකක් හෝ withdraw UI එකක් නැත**; සැපයුම්කරු bids ප්‍රායෝගිකව "දමා අමතක කරන" ආකාරයි. (`supplier_module_documentation.md` §6 සමඟ ගැටේ.)
- **Password reset** — `AuthService.sendPasswordResetEmail()` (:69) ඇත, නමුත් කිසිදු ViewModel/UseCase/Screen එකකින් **කිසිවිටෙක call නොකෙරේ.** "Forgot password" ගලනයක් සම්බන්ධ කර නැත.
- **Order-status අධිකාරිය** — ගොවි සහ සැපයුම්කරු **දෙදෙනාටම** order එක IN_TRANSIT/DELIVERED කළ හැක; rules මගින් ඕනෑම සහභාගිකයෙකුට ඉඩ දෙයි (`firestore.rules` :250-266). ඉටු කිරීම මෙහෙයවන්නේ කවුදැයි role සීමාවක් නැත.

### අඩුපාඩු (නැති දේ)
- **ගෙවීම්** — **ගෙවීම් සැකසීමක් නැත.** Payment SDK නැත, `PAID` තත්ත්වයක් නැත, `Order`/`Bid` හි payment field නැත. `OrderStatus` = {AWAITING_PICKUP, IN_TRANSIT, DELIVERED, CANCELLED}. UI එක පැහැදිලිවම ප්‍රතික්ෂේප කරයි: *"Based on delivered orders; this does not indicate payment status."* (`FarmerOrdersScreen.kt` :130). මිලක් පෙන්වීම හෝ order එකක් සෑදීම **ගෙවීමක් නොවේ.**
- **README හි දැන්වූ විශේෂාංග** — Driver role, cold-chain, GPS transport pooling, cold-storage slot booking, සහ `core/service/` layer එක code එකේ **නැත** (§G / "පෙර ප්‍රකාශ" බලන්න).

### බිඳී ඇති දේ (විවේචනාත්මක)
- **අනාගත-අස්වැන්න (EXPECTED) listings commit කළ security rules සමඟ ක්‍රියා නොකරයි.** Kotlin app එක අනාගත-අස්වැන්න lifecycle එක සම්පූර්ණයෙන් ක්‍රියාත්මක කරයි (EXPECTED listing සෑදීම, ඇස්තමේන්තුව යාවත්කාලීන කිරීම, සත්‍ය ප්‍රමාණය සමඟ ready කිරීම, failed කිරීම, pre-harvest requests, failure වූ විට pre-harvest requests ස්වයංක්‍රීයව cancel කිරීම). **නමුත් commit කළ `firestore.rules` හි අවශ්‍ය සහාය කිසිවක් නැත** (`HEAD:firestore.rules` හි `harvestStatus|isExpectedHarvest|estimatedQty|isPreHarvest` සඳහා `grep` = **ගැළපීම් 0**). මෙම rules deploy කළහොත්:
  - EXPECTED listing එකක් සෑදීම **ප්‍රතික්ෂේප වේ** — create rule එකට `availableStock > 0` අවශ්‍යයි (:80), නමුත් EXPECTED listings `availableQuantity = 0.0` ලෙස සාදයි (`CreateListingScreen.kt` :252-253).
  - `markHarvestReady` / `markHarvestFailed` / ඇස්තමේන්තු යාවත්කාලීන **ප්‍රතික්ෂේප වේ** — ඒවා වෙනස් කරන `harvestStatus`/`estimatedQuantity`/`harvestFailureReason`/`harvestUpdatedAt` update rule එකේ අවසර ලත් `affectedKeys` හි **නැත** (:90-94).
  - `markHarvestFailed` හි පොරොත්තු pre-harvest requests atomically cancel කිරීම **ප්‍රතික්ෂේප වේ** — order_requests update rule එකේ farmer-cancel-pre-harvest clause එක නැත (:161-189).
  - Pre-harvest order requests **ප්‍රතික්ෂේප වේ** — create rule එකට `requestedQuantity <= availableStock` අවශ්‍යයි (:154-155), EXPECTED listings සඳහා එය 0 වේ.
  - EXPECTED listings සෑදිය නොහැකි නිසා, "Mark ready / Mark failed" බොත්තම් (`harvestStatus == EXPECTED` විට පමණක් පෙන්වන, `MyListingsScreen.kt` :473) මෙම rules යටතේ ප්‍රායෝගිකව **ළඟා විය නොහැක.**
  - **Test suite එක මෙය අල්ලා ගන්නේ නැත:** JVM unit tests in-memory fakes භාවිතා කරයි, සහ අනාගත-අස්වැන්න emulator rules tests එම වෙනස්කමේදීම **මකා දමා ඇත** (commit කළ `rules-tests/firestore.rules.test.mjs` හි දැන් tests 22ක් ඇත, කිසිවක් harvest lifecycle ආවරණය නොකරයි).

### සත්‍යාපනය නොකළ දේ (mutate කිරීමෙන් හෝ remote access නොමැතිව තහවුරු කළ නොහැක)
- **Deploy කළ rules vs local rules** — `agroroute` Firebase project එකට දැනට deploy කර ඇති rules local `firestore.rules` සමඟ ගැළපෙන්නේද යන්න. එය තහවුරු කිරීමට live project එකට authenticated access අවශ්‍යයි (remote තත්ත්වය ස්පර්ශ කිරීම/login අවශ්‍ය වීම මග හැරීමට read-only `firebase` rules fetch එකක් **උත්සාහ කළේ නැත**). **තත්ත්වය: සත්‍යාපනය නොකළ.**
- **Live Firebase Auth/Firestore/Storage හැසිරීම** — production Firebase එකට එරෙහිව සැබෑ sign-in, write, read කිසිවක් සිදු කර නැත (credentials/අවසර නැත; remote තත්ත්වය වෙනස් වේ). සියලු "persists/backend" තීන්දු **code + local emulator rules tests** මත පදනම්ය, live calls මත නොවේ.
- **සැබෑ Cloudinary upload** — tests වල fake කර ඇත්තේ පමණි (`CloudinaryUploadTest` `FakeCloudinaryStorage` භාවිතා කරයි). Unsigned preset එක Cloudinary console එකේ තිබේද/නිවැරදිව scope කර ඇත්ද යන්න සත්‍යාපනය **නොකළා** (සැබෑ network upload එකක් අවශ්‍ය).
- **සැබෑ device එකේ ඡායාරූප තේරීම / camera capture** — පරීක්ෂා කර නැත.

---

## B. විශේෂාංග / ගලන න්‍යාසය

සංකේත — **Code**: source හි ක්‍රියාත්මකයි · **UI**: app හරහා ළඟා විය හැක · **Persist**: locally (Room) / remotely (Firestore) සුරැකේ · **Rules**: local security rules වලින් ආවරණය · **Tests**: ස්වයංක්‍රීය ආවරණය · තීන්දු: ✅ ක්‍රියාත්මකයි (code මට්ටමින්), ⚠️ අර්ධ, ❌ බිඳී ඇත/නැත, ❓ live සත්‍යාපනය නොකළ.

| විශේෂාංගය/ගලනය | Code ක්‍රියාත්මකකරණය | App UI හි ළඟා විය හැකිද | Persistence/backend | Security-rule තත්ත්වය | Tests | තීන්දුව |
|---|---|---|---|---|---|---|
| Email/password register | ✅ `AuthRepositoryImpl.register` :112 | ✅ RegisterScreen | ✅ Firestore `users` + Room | ✅ users create :52-55 | ⚠️ fakes පමණි (`AuthRoleRoutingTest`) | ✅ / ❓live |
| Email/password login | ✅ `login`+`restoreSession` :46-66 | ✅ LoginScreen | ✅ Firestore profile → Room කියවයි | ✅ users get :50 | ⚠️ fakes පමණි | ✅ / ❓live |
| Google sign-in | ✅ `signInWithGoogle` :68 | ✅ LoginScreen + Credential Manager | ✅ Firestore profile | ✅ users create/get | ⚠️ fakes පමණි | ✅ / ❓live config |
| Role තේරීම (පළමු Google) | ✅ `completeGoogleSignIn` :87 | ✅ GoogleRoleSelectionScreen | ✅ role Firestore+Room වලට ලියයි | ✅ role in [FARMER,SUPPLIER] :55 | ⚠️ `AuthRoleRoutingTest` (3) | ✅ |
| Role persistence සහ වෙනස් කළ නොහැකි බව | ✅ :141 | ✅ n/a (බලාත්මක) | ✅ | ✅ role වෙනස් කළ නොහැක :60 | ⚠️ අර්ධ | ✅ |
| Profile load | ✅ `getCurrentUserProfile` :38 | ✅ Profile screens | ✅ Room-first | ✅ users get :50 | ⚠️ | ✅ |
| Sign-out | ✅ `logout` :130 | ✅ FarmerTopBar/Profile, SupplierProfile | ✅ Room profile මකයි | n/a | ⚠️ | ✅ |
| ගොවි/සැපයුම්කරු nav සහ access guards | ✅ `NavGraph` :60-100 | ✅ | n/a | n/a | ✅ `AuthRoleRoutingTest`, `FarmerNavigationTest` | ✅ (හිඩැස් 1, §D-LOW) |
| Listing සෑදීම/publish (READY) | ✅ `CreateListingUseCase`+`saveListing` | ✅ CreateListingScreen | ✅ Firestore→Room | ✅ create :73-80 | ✅ `HarvestListingRulesTest` (30, fakes) + emulator | ✅ |
| Listing fields validate කිරීම | ✅ use case :29-40 | ✅ inline errors | n/a | ⚠️ rules උප කුලකයක් පරීක්ෂා කරයි | ✅ | ✅ |
| Listing edit (READY) | ✅ `updateListing` :84-157 | ✅ EditListing | ✅ Firestore update→Room | ⚠️ §D-MED-3 බලන්න | ✅ `UpdateListingUseCaseTest` (5) | ✅ |
| Draft listing (local පමණි) | ✅ `saveDraft` :159 | ✅ "Save draft" | ✅ **Room පමණි** (අරමුණින්) | n/a (කිසිවිටක නොයැවේ) | ⚠️ | ✅ |
| Listing close කිරීම | ✅ `closeForNewBids` | ✅ MyListings dialog | ✅ | ✅ status වෙනසට ඉඩ ඇත | ⚠️ | ✅ |
| Bidding toggle | ✅ `toggleBiddingMode` :204 | ✅ MyListings switch | ✅ | ✅ | ⚠️ | ✅ |
| Listing delete | ✅ `deleteListing` :168 | ✅ | ✅ | ✅ delete :96 | ⚠️ | ✅ |
| Listing හිමිකාරිත්වය | ✅ repo/use cases හි checks | ✅ | ✅ | ✅ farmerId==uid | ✅ | ✅ |
| **අනාගත-අස්වැන්න: EXPECTED සෑදීම** | ✅ model+screen+repo | ✅ (status selector) | ❌ **rules ප්‍රතික්ෂේප කරයි** | ❌ create සඳහා stock>0 අවශ්‍ය :80 | ❌ **tests මකා ඇත** | ❌ **බිඳී ඇත** |
| **අනාගත-අස්වැන්න: ඇස්තමේන්තුව යාවත්කාලීන** | ✅ `updateHarvestEstimate` | ✅ EditListing | ❌ **rules ප්‍රතික්ෂේප කරයි** | ❌ keys අවසර නැත :90-94 | ❌ මකා ඇත | ❌ **බිඳී ඇත** |
| **අනාගත-අස්වැන්න: ready ලෙස සලකුණු** | ✅ `markHarvestReady` :218 | ✅ dialog (EXPECTED පමණි) | ❌ **rules ප්‍රතික්ෂේප කරයි** | ❌ harvestStatus key අවහිරයි | ❌ මකා ඇත | ❌ **බිඳී ඇත** |
| **අනාගත-අස්වැන්න: failed + requests cancel** | ✅ `markHarvestFailed` :243 (txn) | ✅ dialog | ❌ **rules ප්‍රතික්ෂේප කරයි** | ❌ listing keys + farmer-cancel clause ඉවත් කර ඇත | ❌ මකා ඇත | ❌ **බිඳී ඇත** |
| **Pre-harvest request** | ✅ `placeOrderRequest(isPreHarvest)` :77 | ✅ PlaceOrderRequestScreen | ❌ **rules ප්‍රතික්ෂේප කරයි** | ❌ qty<=availableStock(=0) :154 | ❌ මකා ඇත | ❌ **බිඳී ඇත** |
| Listing සුදුසුකම/දෘශ්‍යතාව | ✅ `isEligibleForMarketplace` ආදිය | ✅ marketplace filter | ✅ | ✅ supplier read OPEN+published :68-71 | ✅ `MarketplaceEligibilityTest` (5) | ✅ |
| සැපයුම්කරුවන්ට සියලු සුදුසු ඒවා පෙනේ (bid කළ ඒවා පමණක් නොවේ) | ✅ `syncListings` status=OPEN :195 | ✅ | ✅ | ✅ list rule :68 | ⚠️ | ✅ |
| ඡායාරූප තේරීම + URI හැසිරවීම | ✅ PhotoPicker / `content://` | ✅ | n/a | n/a | ⚠️ | ✅ |
| ඡායාරූප preview/progress/retry/error | ✅ CreateListingScreen | ✅ | n/a | n/a | ✅ `CloudinaryUploadTest` (13, fakes) | ✅ |
| Cloudinary unsigned upload | ✅ `CloudinaryPhotoStorage` | ✅ | ✅ `secure_url`→`imageUrls` ලබා දේ | n/a (Cloudinary, Firebase නොවේ) | ⚠️ **fake පමණි** | ✅code / ❓සැබෑ upload |
| පැරණි Firebase Storage URLs | ✅ Coil URL load කරයි; `StorageService` රඳවා ඇත | ✅ | ✅ (පවතින docs) | ✅ storage.rules read :23-29 | ❌ | ✅ / ❓live |
| Marketplace queries/filters | ✅ `MarketplaceViewModel`+screen | ✅ | ✅ Room cache | ✅ | ✅ eligibility test | ✅ |
| Listing විස්තර (මිල/ප්‍රමාණය/දිනය/bidding/ලබාගත හැකි බව) | ✅ BrowseListings card | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| Place Bid action gating | ✅ `acceptsNewBids()` | ✅ ඉඩ ඇති විට පමණි | ✅ | ✅ bid create සඳහා biddingEnabled අවශ්‍ය :111 | ⚠️ | ✅ |
| ස්ථාවර මිල request gating | ✅ `acceptsDirectOrders()` | ✅ ඉඩ ඇති විට පමණි | ✅ | ✅ request create සඳහා !biddingEnabled අවශ්‍ය :152 | ✅ emulator | ✅ |
| Bid සෑදීම/validate/ownership | ✅ `PlaceBidUseCase`+`placeBid` txn :49 | ✅ | ✅ Firestore→Room | ✅ supplierId==uid, PENDING :105-113 | ⚠️ fakes | ✅ |
| ගොවි bid සමාලෝචන/පිළිගැනීම/ප්‍රතික්ෂේපය | ✅ `AcceptBidUseCase`/`RejectBidUseCase` | ✅ FarmerBids/ViewBids | ✅ txn | ✅ accept coupling :128-133 | ✅ `HarvestListingRulesTest` | ✅ |
| Bid දෙවරක් පිළිගැනීමේ ආරක්ෂාව | ✅ in-txn PENDING guard :89 | ✅ | ✅ | ✅ | ✅ (fake) `OrderRequestFlowTest` | ✅ |
| පිළිගත් bid එකෙන් පසු order සෑදීම | ✅ txn order set කරයි :103 | ✅ | ✅ | ✅ orderId==bidId :204 | ✅ | ✅ |
| **සැපයුම්කරුට තමන්ගේ bids බැලීම/withdraw** | ⚠️ repo methods ඇත, **UI නැත** | ❌ **ළඟා විය නොහැක** | ✅ (call කළහොත්) | ✅ rules ඉඩ දෙයි :125-127 | ❌ | ⚠️ **අර්ධ/UI නැත** |
| ඉල්ලූ ප්‍රමාණ සීමා | ✅ use case + repo + rules | ✅ | ✅ | ✅ :149,:154 | ✅ emulator | ✅ |
| මිල අඛණ්ඩතාව (listedPrice==reservePrice) | ✅ `PlaceOrderRequestUseCase` :68 | ✅ | ✅ | ✅ :158-159 | ✅ emulator | ✅ |
| Request/listing හිමිකාරිත්වය | ✅ checks | ✅ | ✅ | ✅ farmerId ගැළපීම :156 | ✅ emulator | ✅ |
| ගොවි request පිළිගැනීම/ප්‍රතික්ෂේපය | ✅ `AcceptOrderRequestUseCase`/Reject | ✅ FarmerOrderRequests | ✅ txn | ✅ accept coupling :178-188 | ✅ `OrderRequestFlowTest` (25) + emulator | ✅ |
| සැපයුම්කරු request cancel | ✅ `cancelRequest` | ✅ SupplierOrders | ✅ | ✅ supplier CANCELLED :175-177 | ✅ emulator | ✅ |
| Order නිවැරදි අවස්ථාවේදී පමණක් සෑදීම | ✅ accept txn හි පමණි | ✅ | ✅ | ✅ | ✅ | ✅ |
| Pre-harvest තොගය නොවෙනස්ව තබයි | ✅ accept සඳහා READY අවශ්‍ය :136 | ✅ | ✅ | ⚠️ rules READY guard ඉවත් කර ඇත :235 (client තවමත් බලාත්මක කරයි) | ✅ (fake) | ✅code / ❌ pre-harvest මාර්ගය ඉහළින්ම බිඳී ඇත |
| තොගය හරියටම එක් වරක් අඩු වීම | ✅ txn හි `recordSale` | ✅ | ✅ | ✅ soldQty/avail සූත්‍ර :217-222,:242-247 | ✅ (fake) duplicate-tap test | ✅ |
| ප්‍රමාණවත් නොවන තොගය / oversell ආරක්ෂාව | ✅ client + txn + rules | ✅ | ✅ | ✅ qty<=availableStock | ✅ emulator + fake | ✅ |
| Pending/rejected/cancelled තොගය නොවෙනස් කරයි | ✅ | ✅ | ✅ | ✅ | ✅ `cancelledAndRejected…` test | ✅ |
| Firestore↔Room අනුකූලතාව | ✅ Firestore ලියා පසුව Room; sync overwrite කරයි | ✅ | ✅ | n/a | ❌ integration test නැත | ⚠️ (§D-MED බලන්න) |
| ගොවි/සැපයුම්කරු order lists + විස්තර | ✅ DAOs + screens | ✅ | ✅ Room-first | ✅ get/list ownership :195-197 | ⚠️ | ✅ |
| Ownership filtering (orders) | ✅ `getOrdersForFarmer/Supplier` | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| Order තත්ත්ව මාරුවීම් + අධිකාරිය | ✅ `updateOrderStatus` :172 | ✅ detail screens දෙකම | ✅ | ⚠️ **ඕනෑම සහභාගිකයෙකු** :250-266 | ⚠️ | ⚠️ (§D-MED-4 බලන්න) |
| Order cancel කිරීමෙන් තොගය නිදහස් වේ | ✅ `releaseSale` txn :192 | ✅ | ✅ | ✅ idempotent | ⚠️ | ✅ |
| හිස්/loading/error states | ✅ Result + UI states | ✅ | n/a | n/a | ⚠️ | ✅ |
| **ගෙවීම් සැකසීම** | ❌ **නැත** | ❌ | ❌ | ❌ | ❌ | ❌ **පවතින්නේ නැත** |
| Room entities/DAOs/converters | ✅ entities 5, converters | ✅ | ✅ | n/a | ❌ DAO/migration test නැත | ⚠️ |
| Room migrations (1→5) | ✅ `DatabaseMigrations` | ✅ | ✅ | n/a | ❌ **migration test නැත**, `exportSchema=false` | ⚠️ (§D-MED-6 බලන්න) |
| Offline හැසිරීම | ✅ Room-first + connectivity sync | ✅ | ✅ | n/a | ❌ | ⚠️ පැරණි දත්ත අවදානම |
| Deploy කළ rules == local rules | ❓ | n/a | ❓ | ❓ | ❌ | ❓ **සත්‍යාපනය නොකළ** |

---

## C. ප්‍රධාන ගොවි→සැපයුම්කරු ගමන (පියවරෙන් පියවර, ක්‍රියාත්මක වන ආකාරයට)

මෙය **ස්ථාවර මිල** මාර්ගය සහ **bidding** මාර්ගය යන දෙකම පෙන්වයි. පේළි යොමු වන්නේ වත්මන් `HEAD` වෙතයි.

1. **ගොවි listing එකක් සාදයි.** `CreateListingScreen.buildListing()` (:228) → `ListingViewModel.createListing()` (:116) → `CreateListingUseCase` (cropName/category/quantity≥0/price validate කරයි; `content://` ඡායාරූපයක් Cloudinary වෙත upload කර `secure_url` මගින් ප්‍රතිස්ථාපනය කරයි) → `HarvestListingRepositoryImpl.saveListing()` (:73) Firestore `listings/{id}` (merge) වලට ලියා පසුව Room වලට mirror කරයි. Rules වලට farmer role, `farmerId==uid`, `status==OPEN`, `isDraft==false`, සහ **`availableStock>0`** අවශ්‍යයි (`firestore.rules` :73-80).
   - ⚠️ ගොවි **EXPECTED (අනාගත-අස්වැන්න)** තෝරන්නේ නම්, `availableQuantity` `0.0` ලෙස සකසයි (`CreateListingScreen.kt` :252) එබැවින් `availableStock==0` → **commit කළ create rule එක write එක ප්‍රතික්ෂේප කරයි.** වත්මන් rules යටතේ අනාගත-අස්වැන්න ගමන මෙතැනින් නතර වේ.
2. **Marketplace දෘශ්‍යතාව.** සැපයුම්කරුගේ `MarketplaceViewModel` connectivity මත sync වේ (`syncListings()` Firestore `status==OPEN` query කරයි, drafts ඉවත් කරයි, SUPPLIER සඳහා — `HarvestListingRepositoryImpl` :195-199) Room වලට; `BrowseListingsScreen` Room කියවා `isSupplierMarketplaceEligible()` (:74) = `isEligibleForMarketplace()` (OPEN, draft නොවන, තහවුරු කළ තොගය **හෝ** EXPECTED) මගින් filter කරයි. Rules මගින් ඕනෑම සැපයුම්කරුවෙකුට published OPEN listings කියවීමට ඉඩ දෙයි (`firestore.rules` :68-71), එබැවින් සැපයුම්කරුවන්ට අන්තර්ක්‍රියා කළ ඒවා පමණක් නොව **සියලු** සුදුසු listings පෙනේ.
3. **සැපයුම්කරු ක්‍රියා කරයි.** Card එකකට `acceptsNewBids()` → **Place Bid** route; `acceptsDirectOrders()` → **Place Order Request** route (`BrowseListingsScreen` :197-203).
   - **Bid මාර්ගය:** `PlaceBidUseCase` (price>0, supplier role/ownership, `listing.acceptsNewBids()` validate කරයි) → `BidRepositoryImpl.placeBid()` listing කියවා `status=PENDING` සහිතව `bids/{bidId}` ලියන transaction එකක් ධාවනය කරයි (:60-65). Rules වලට supplier ownership, PENDING, listing OPEN+biddingEnabled+published+stock>0 අවශ්‍යයි (:105-113).
   - **ස්ථාවර මිල මාර්ගය:** `PlaceOrderRequestUseCase` (qty, supplier role, `acceptsDirectOrders()`, qty ≤ availableStock හෝ pre-harvest සඳහා ≤ estimate validate කරයි) → `OrderRequestRepositoryImpl.placeOrderRequest()` transaction එක `order_requests/{id}` `status=PENDING` ලියයි (:57-95). Rules වලට supplier ownership, PENDING, qty>0, listing OPEN+!biddingEnabled+published, `qty ≤ availableStock`, `farmerId` ගැළපීම, සහ **`listedPrice == reservePrice`** අවශ්‍යයි (:145-159). **Request කාලයේදී තොගය වෙන් කෙරෙන්නේ නැත.**
4. **ගොවි තීරණය.**
   - **Bids:** `FarmerBidsScreen`/`ViewBidsScreen` → `AcceptBidUseCase` (ගොවි, bid PENDING, listing OPEN+bidding+stock>0 verify කරයි; `orderId=bidId`, `quantity = listing.availableStock()` සහිත `Order` එකක් සාදයි — එනම් ජයග්‍රාහී bid එක **ඉතිරි සම්පූර්ණ තොගයම** ගනී) → `OrderRepositoryImpl.acceptBidAndCreateOrder()` **transaction** (:67-110): listing+bid නැවත කියවයි, listing OPEN/bidding/!draft, **`harvestStatus==READY`**, bid PENDING සහ ownership ගැළපීම, `0 < qty ≤ availableStock` assert කරයි; ඉන්පසු `recordSale(qty)` (available අඩු, sold වැඩි, තොග ඉවර නම් FULFILLED), bid→ACCEPTED, order ලිවීම — සියල්ල atomically. ප්‍රතික්ෂේපය → `RejectBidUseCase` → `updateBidStatus(REJECTED)` (තොග වෙනසක් නැත).
   - **Order requests:** `FarmerOrderRequestsScreen` → `AcceptOrderRequestUseCase` (ගොවි, request PENDING, listing ownership, **`harvestStatus==READY`**, `requestedQuantity ≤ availableStock` verify කරයි) → `OrderRepositoryImpl.acceptOrderRequestAndCreateOrder()` **transaction** (:112-170): request+listing නැවත කියවයි, request PENDING, listing OPEN/!draft/!bidding, **READY**, `0 < qty ≤ availableStock` assert කරයි; `Order(orderId=requestId, bidId="")` සාදයි, `recordSale(qty)`, request→ACCEPTED — atomically. ප්‍රතික්ෂේපය → `updateRequestStatus(REJECTED)` (තොග වෙනසක් නැත). සැපයුම්කරු cancel → `CANCELLED` (තොග වෙනසක් නැත).
5. **Order සෑදීම.** සිදුවන්නේ ඉහත accept transactions **තුළ පමණි** (bid/request කාලයේදී කිසිවිටෙකත් නොවේ). Rules ද ස්වාධීනව coupling බලාත්මක කරයි: order එකක් සෑදිය හැක්කේ listing එකේ ගොවියාට පමණි, `orderId==bidId` (bid මාර්ගය) හෝ `bidId==''` + ගැළපෙන `order_requests/{orderId}` (request මාර්ගය) සමඟ, bid/request **එම write එකේම** ACCEPTED කර, `soldQuantity`/`availableStock` ගණිතය හරියටම ගැළපෙන සේ (`firestore.rules` :199-248).
6. **තොග යාවත්කාලීනය.** `recordSale()` (`HarvestListing.kt` :132) transaction තුළ, PENDING status check එක මගින් ආරක්ෂා කර, `availableQuantity` අඩු කර `soldQuantity` වැඩි කරන්නේ **එක් වරක්** → නැවත නැවත tap කිරීම/සමගාමී accepts මගින් දෙවරක් අඩු කළ නොහැක (දෙවන උත්සාහය status≠PENDING නැවත කියවා නවතී). `OrderRequestFlowTest.repeatedTapsOrDuplicateAcceptanceCannotDeductStockMoreThanOnce` මගින් (guards පිළිබිඹු කරන fake එකකට එරෙහිව) සත්‍යාපිතයි.
7. **ඉටු කිරීම.** `FarmerOrderDetailScreen` / `SupplierOrderDetailScreen` → `updateOrderStatus()` (:172). Cancel නොවන මාරුවීම් Firestore read-then-write වේ; **CANCELLED** තොගය ආපසු දීමට `releaseSale()` call කරන transaction එකක් ධාවනය කරන අතර idempotent වේ (දැනටමත් cancel වී ඇත්නම් කලින් return වේ). Rules මගින් මාරුවීම් AWAITING_PICKUP→{IN_TRANSIT,CANCELLED}, IN_TRANSIT→{DELIVERED,CANCELLED}, DELIVERED→{CANCELLED} ලෙස සීමා කරන අතර **සහභාගිකයින් දෙදෙනාටම** ඒවා කිරීමට ඉඩ දෙයි (:250-266). සාර්ථක Firestore write එකකින් පසු Room යාවත්කාලීන වේ; `syncOrders()` Firestore වලින් refresh කරයි.

**සාරාංශය:** **READY** listings සඳහා ගමන code තුළ සංගතයි සහ transaction අනුව ආරක්ෂිතයි. **EXPECTED/අනාගත-අස්වැන්න** listings සඳහා commit කළ rules යටතේ ගමන **1 වන පියවරේදීම බිඳී ඇත.**

---

## D. දත්ත-අඛණ්ඩතා සහ ආරක්ෂණ අවදානම් (ශ්‍රේණිගත)

### 🔴 ඉහළ
1. **අනාගත-අස්වැන්න rules/app අසංගතතාව (commit කර ඇත).** App එක සම්පූර්ණ EXPECTED-harvest lifecycle එක ක්‍රියාත්මක කරයි, නමුත් `HEAD:firestore.rules` හි ඒ සඳහා **කිසිදු** සහායක් නැත (`harvestStatus`/`estimatedQuantity`/`isPreHarvest` හැසිරවීමක් නැත; create සඳහා `availableStock>0` අවශ්‍ය; update `affectedKeys` හි harvest fields නැත; farmer pre-harvest-cancel clause ඉවත් කර ඇත). Deploy කළහොත්, සෑම අනාගත-අස්වැන්න write එකක්ම `PERMISSION_DENIED` වී විශේෂාංගය ළඟා විය නොහැකි වේ. **මෙය මග හැරුණේ ඇයි:** JVM unit tests fakes භාවිතා කරයි (rules කිසිවිටක ස්පර්ශ නොකරයි), සහ harvest lifecycle ආවරණය කළ emulator rules tests එම commit එකේම **මකා දමා ඇත.** *ගොනු:* `firestore.rules` :73-97,:145-192,:235; `HarvestListingRepositoryImpl` :218-291; `CreateListingScreen.kt` :252; `OrderRequestRepositoryImpl` :77-91.
2. **Deploy කළ-vs-local rules නොදනී.** Production rules local ගොනුවට ගැළපේදැයි (authenticated, mutate විය හැකි remote access නොමැතිව) තහවුරු කළ නොහැක. #1 අවදානම නිසා, production හි අනාගත-අස්වැන්න හැසිරීම අවිනිශ්චිතයි. **සත්‍යාපනය නොකළ.**
3. **සැබෑ Firestore transactions හෝ app හි සැබෑ write payloads අභ්‍යාස කරන integration test එකක් නැත.** `OrderRepositoryImpl`/`OrderRequestRepositoryImpl`/`BidRepositoryImpl` transactions සත්‍යාපනය වන්නේ **එම guards ම නැවත ක්‍රියාත්මක කරන fakes** මගින් පමණි (`OrderRequestFlowTest` `FakeOrderRepository` :705-769). Emulator rules tests **සරල කළ payloads** භාවිතා කරයි (උදා. edit test එක ලියන්නේ `cropName` පමණි, :76-82, නමුත් `updateListing` harvest fields ඇතුළු fields 14ක map එකක් ලියයි, `HarvestListingRepositoryImpl` :135-155). එබැවින් සැබෑ client-write-vs-rules drift අල්ලා ගැනෙන්නේ නැත.

### 🟠 මධ්‍යම
4. **ඉටු කිරීමේ අධිකාරිය role අනුව සීමා කර නැත.** ඕනෑම පාර්ශවයකට IN_TRANSIT/DELIVERED සැකසිය හැක (`firestore.rules` :250-266; detail screens දෙකම එම බොත්තම් ම පෙන්වයි). සැපයුම්කරුවෙකුට තමාම order එකක් DELIVERED ලෙස සලකුණු කළ හැක. ව්‍යාපාරික-රීති අතිච්ඡාදනයකි, ownership කඩ කිරීමක් නොවේ.
5. **Pre-harvest request lifecycle හිඩැස්.** `markHarvestReady(actual)` සහ `updateHarvestEstimate()` පවතින PENDING pre-harvest requests නැවත validate හෝ සකස් **නොකරයි.** ඇස්තමේන්තුව අඩු කළහොත්, හෝ සත්‍ය ready-ප්‍රමාණය ඉල්ලූ ප්‍රමාණයට වඩා අඩු නම්, request එක PENDING ලෙස පවතී නමුත් කිසිවිටක පිළිගත නොහැක (`AcceptOrderRequestUseCase` :63 තොගය ප්‍රමාණවත් නැති නිසා ප්‍රතික්ෂේප කරයි). පොරොත්තු pre-harvest requests cancel කරන්නේ `markHarvestFailed` පමණි (:243-291). සැපයුම්කරුවන්ට පැරණි, පිළිගත නොහැකි requests ඉතිරි විය හැක. (#1 නිරාකරණය වන තුරු අදාළ නොවේ, නමුත් ගුප්තව පවතී.)
6. **`fallbackToDestructiveMigration()` + `exportSchema=false` + migration tests නැත** (`DatabaseModule.kt` :41, `AppDatabase.kt` :25). Migration එකක් අස්ථානගත/වැරදි නම් local Room දත්ත නිශ්ශබ්දව **මැකේ.** Migrations 1→5 ඇත, නමුත් කිසිදු test එකකින් සත්‍යාපනය නොකෙරේ.
7. **Marketplace/offline පැරණි දත්ත.** සැපයුම්කරු listings Room cache එකෙන් එන අතර, connectivity වෙනස් වන විට පමණක් `syncListings()` මගින් refresh වේ (`MarketplaceViewModel` :34-38) — marketplace සඳහා **live Firestore snapshot listener එකක් නැත.** Syncs අතර පෙන්වන තොගය/තත්ත්වය පැරණි විය හැක; සැපයුම්කරුවන් දෙදෙනෙකුට පැරණි ලබාගත හැකි බව මත ක්‍රියා කළ හැක (server-side transaction එක තවමත් oversell වළක්වයි, නමුත් UX පැරණි දත්ත පෙන්වයි).
8. **`updateOrderStatus` cancel නොවන මාර්ගය transactional නොවේ** (`getOnce` පසුව `set`, :202-210). බලපෑම අඩුයි (status පමණි), නමුත් සමගාමී cancel එකකට read එක race කළ හැක.

### 🟡 අඩු
9. **Password reset සම්බන්ධ කර නැත** — `sendPasswordResetEmail` dead code (UI caller නැත).
10. **සැපයුම්කරුට bids බැලීමට/withdraw කිරීමට නොහැක** — backend ඇත, screen නැත; සැපයුම්කරුට bids දමා අමතක කළ යුතුයි.
11. **Dead/placeholder UI** — FarmerTopBar "Settings" (:351) සහ "Help & Support" (:361) menu එක වසන්නේ පමණි; notification centre එක hard-coded mock දත්ත (:127-157); `features/auth/WelcomeScreen.kt` භාවිතා නොකරන අනුපිටපතකි; `PlaceBidScreen` :94-95 / `PlaceOrderRequestScreen` :96-97 හි හිස් error-clear lambdas.
12. **`supplierRoutes` හි `PlaceOrderRequest` route එක නැත** (`SupplierTopBar.kt` :154-158) → එම route සඳහා supplier scaffold/top-bar නැත සහ **NavGraph role-guard redirect නැත**; `PlaceOrderRequestViewModel` හි data-layer role checks මගින් අඩු කර ඇත.
13. **`firestore-debug.log` (බයිට් 0 emulator artifact) commit/track කර ඇත** — repo සනීපාරක්ෂාව; git-ignore කළ යුතුයි.
14. **`storage.rules` ප්‍රායෝගිකව භාවිතා නොකරන මාර්ගයක් පාලනය කරයි.** නව uploads Cloudinary වෙත යයි; Firebase Storage `listing_images/...` rules වැදගත් වන්නේ පැරණි ප්‍රදර්ශනයට පමණි. හානිකර නොවේ, නමුත් rule set එක සක්‍රීය upload මාර්ගයට තවදුරටත් නොගැළපේ.
15. **Documentation ක්‍රියාත්මකකරණය අධිතක්සේරු කරයි** (පහත "පෙර ප්‍රකාශ" බලන්න) — README/spec මගින් බොරු විශ්වාසයක් ඇති වීමේ අවදානම.

### පෙර ප්‍රකාශ vs වත්මන් workspace
- Root `README.md`: **Driver role** (code හි ඇත්තේ FARMER/SUPPLIER පමණි, `Enums.kt` :8-11), order status **"through to paid/delivered"** (PAID status නැත), bids **"with payment terms"** (එවැනි field නැත), **cold-chain / GPS transport pooling / cold-storage booking** (ක්‍රියාත්මක කර නැත), සහ per-action UseCases ප්‍රතිස්ථාපනය කරන **`core/service/` layer** (`core/service/` dir නැත; code per-action UseCases භාවිතා කරයි) යනුවෙන් කියයි. `google-services.json` commit කර ඇත (Firebase සඳහා සාමාන්‍ය, නමුත් track වන බව සලකන්න).
- `supplier_module_documentation.md`: **"Bids" ඇතුළු tab 5ක supplier bottom nav** (සැබෑ: tabs 4, Bids නැත — `SupplierBottomBar.kt` :55-60), වෙන්වූ **Item Details screen** (සැබෑ: card → කෙලින්ම PlaceBid/PlaceOrderRequest), **Withdraw සහිත My Bids screen** (ක්‍රියාත්මක කර නැත), **Supplier Home pending-bids counter** (හමු නොවීය), සහ **profile editing / photo upload / address fields** (SupplierProfileScreen logout හැර read-only) විස්තර කරයි.

---

## E. ඉතිරි අතින් කළ යුතු පියවර (මෙම විගණනයෙන් **නොකළ** ඒවා)

මේවාට read-only විගණනයකින් ඔබ්බට තීරණ/අවසර අවශ්‍ය නිසා **හිතාමතාම සිදු නොකළා:**

1. **අනාගත-අස්වැන්න අසංගතතාව සමාදාන කරන්න (එකක් තෝරන්න):**
   - (a) **Rules සහාය ප්‍රතිෂ්ඨාපනය** — `isExpectedHarvest()`/`estimatedQty()` helpers නැවත එක් කරන්න, `estimatedQuantity>0` සහිත EXPECTED create ට ඉඩ දෙන්න, `harvestStatus`/`estimatedQuantity`/`harvestFailureReason`/`harvestUpdatedAt` listing-update `affectedKeys` හි එක් කරන්න, `order_requests` හි farmer pre-harvest-cancel clause නැවත එක් කරන්න, සහ order creation මත `harvestStatus=='READY'` guard නැවත එක් කරන්න; **හෝ**
   - (b) App එකෙන් **අනාගත-අස්වැන්න විශේෂාංගය ඉවත් කරන්න** (models, repository, use cases, screens, enums, migration columns).
   - ඉන්පසු harvest lifecycle සඳහා **මකා දැමූ emulator rules tests නැවත එක් කරන්න.** *(පෙර commit ඉතිහාසය `414a0d2`/HEAD~ හි මෙම rules+tests තිබුණි; ඒවා `899b893` හි ඉවත් කරන ලදී.)*
2. `agroroute` project එකට **කුමන rules deploy කර ඇත්දැයි සත්‍යාපනය කරන්න**, අදහස් කළහොත් සමාදාන කළ `firestore.rules`/`storage.rules` deploy කරන්න (`firebase deploy --only firestore,storage`). **නොකළා — mutate වන අතර අවසර අවශ්‍යයි.**
3. **Cloudinary තහවුරු කරන්න** — unsigned preset එක ඇති බවත් listing-photos folder එකට scope කර ඇති බවත්; end-to-end සත්‍යාපනයට **සැබෑ device upload** එකක් කරන්න (වත්මන් tests එය fake කරයි). **නොකළා.**
4. **Firebase project config තහවුරු කරන්න** — `app/google-services.json` අදහස් කළ project එකට යොමු වන බව; console එකේ Email/Password සහ Google providers සක්‍රීය බව; **සැබෑ sign-in** smoke test එකක් කරන්න. **Live සත්‍යාපනය නොකළා.**
5. **Dead backend සම්බන්ධ කරන්න හෝ ඉවත් කරන්න** — password reset UI, සැපයුම්කරු "My Bids"/withdraw screen, හෝ භාවිතා නොකරන methods මකන්න. **නොකළා.**
6. **Repo සනීපාරක්ෂාව** — `firestore-debug.log` (සහ `*.log`) `.gitignore` හි එක් කර untrack කරන්න. **නොකළා.**
7. Migrations ආරක්ෂා කිරීමට **instrumented/migration tests** (`androidTest`) සහ Room schema export එක් කරන්න. **නොකළා.**
8. **ඉටු කිරීමේ අධිකාරිය තීරණය කරන්න** — අදහස් කළ ව්‍යාපාරික රීතිය එය නම් IN_TRANSIT/DELIVERED මාරුවීම් rules + UI හි සුදුසු role එකට සීමා කරන්න. **නොකළා.**

---

## F. ධාවනය කළ විධාන සහ සැබෑ ප්‍රතිඵල

සියලු විධාන `D:\Projects\AgroRoute` වෙතින් ධාවනය කරන ලදී. කිසිවක් deploy, commit, push හෝ remote වශයෙන් mutate කර නැත.

| # | විධානය | ප්‍රතිඵලය |
|---|---|---|
| 1 | `git status` / `git branch -vv` / `git log --oneline -20` | Branch `v3`; මුලින් වෙනස් කළ ගොනු 5ක් + track නොකළ `firestore-debug.log`; මෑත commits `414a0d2 maybe`, `143cdcb with errors`, `21f5906 Add supplier order request flow`. |
| 2 | `git diff` (rules, tests, Kotlin) | `firestore.rules` සහ `rules-tests` වලින් අනාගත-අස්වැන්න logic **ඉවත් කිරීම**, `FarmerOrderRequestsViewModel` හි `combine()` එක් කිරීම, `AcceptOrderRequestUseCase` හි තොග guard එකක් එක් කිරීම, `OrderRequestFlowTest` හි නව tests 3ක් පෙන්වීය. |
| 3 | `git status` (නැවත පරීක්ෂා, පසුව) | **පිරිසිදුයි.** `git log` මගින් විගණනය අතරතුර බාහිරව සාදන ලද **නව commit `899b893 "beffore editing"`** (2026-10-03 20:14) පෙන්වීය; `v3` `origin/v3` ට වඩා 1කින් ඉදිරියෙන් (push කර නැත). `firestore-debug.log` දැන් tracked. |
| 4 | `git show HEAD:firestore.rules \| grep -cE "harvestStatus\|isExpectedHarvest\|estimatedQty\|isPreHarvest"` | **0** — commit කළ rules වල අනාගත-අස්වැන්න සහාය නැත. |
| 5 | `git show HEAD:.../HarvestListingRepositoryImpl.kt \| grep -cE "markHarvestReady\|markHarvestFailed"` | **6** — commit කළ app code එක harvest lifecycle ක්‍රියාත්මක කරයි. (අසංගතතාව #1 තහවුරු කරයි.) |
| 6 | `./gradlew testDebugUnitTest --offline` (පළමු ධාවනය) | `BUILD SUCCESSFUL in 1s` — නමුත් සියලු tasks **UP-TO-DATE** (cached; tests ක්‍රියාත්මක නොවීය). |
| 7 | `./gradlew testDebugUnitTest --offline --rerun-tasks` | **`BUILD SUCCESSFUL in 57s`**, tasks 31ක් ක්‍රියාත්මක විය. Debug Kotlin compile විය (deprecation warnings පමණි). |
| 8 | `app/build/test-results/**.xml` parse කිරීම | **Suites 7, tests 83, failures 0, errors 0, skipped 0**: `AuthRoleRoutingTest` 3, `FarmerNavigationTest` 2, `CloudinaryUploadTest` 13, `HarvestListingRulesTest` 30, `UpdateListingUseCaseTest` 5, `OrderRequestFlowTest` 25, `MarketplaceEligibilityTest` 5. |
| 9 | `cd rules-tests && npm test` (emulator නොමැතිව) | **අසාර්ථක** — සෑම test එකක්ම error විය: *"The host and port of the firestore emulator must be specified."* (`npm test` script එක emulator ආරම්භ නොකරයි; කලින් "exit 0" එක `\| tail` pipe එකෙන් වසං විය.) |
| 10 | `firebase emulators:exec --only firestore --project demo-agroroute-rules "cd rules-tests && npm test"` | **ආරම්භ වීමට අසාර්ථක** — *"Port 8081 is not open … Could not start Firestore Emulator, port taken."* (පැරණි process එකක් 8081 රඳවා ඇත; kill කළේ නැත.) |
| 11 | `firebase emulators:exec --only firestore --config firebase.rules-test.json --project demo-agroroute-rules "cd rules-tests && npm test"` | **`tests 22, pass 22, fail 0`** (port 8082 හි emulator, demo project, login නැත). Rules tests **වත්මන් (ඉවත් කළ) rules** වලට එරෙහිව සමත් වේ — එහෙත් ඒවායේ **අනාගත-අස්වැන්න cases තවදුරටත් නැති** බව සලකන්න. |
| 12 | `./gradlew assembleDebug --offline` | **`BUILD SUCCESSFUL in 4s`** → `app/build/outputs/apk/debug/app-debug.apk` (≈25 MB). |

**ධාවනය නොකළ විධාන (සහ හේතු):**
- `firebase deploy` / ඕනෑම rules publish — **remote තත්ත්වය mutate කරයි; අවසර නැත.**
- **Deploy කළ** production rules කියවීම — live `agroroute` project එකට authenticated access අවශ්‍ය; remote තත්ත්වය ස්පර්ශ නොකිරීමට උත්සාහ නොකළා. **Deploy කළ rules සත්‍යාපනය නොකළ ලෙසම පවතී.**
- ඕනෑම **live** Firebase Auth/Firestore/Storage call එකක් හෝ **සැබෑ Cloudinary upload** — credentials/network අවශ්‍ය වන අතර remote තත්ත්වය mutate කරයි; අවසර නැත. සියලු backend තීන්දු code-මට්ටම + local-emulator පමණි.
- 8081 port එකේ process එක kill කිරීම — මග හැරියා (එය user ගේ වැඩ විය හැක); ඒ වෙනුවට විකල්ප 8082 config එක භාවිතා කළා.

---

## G. ප්‍රමුඛතා අනුව නිර්දේශිත නිවැරදි කිරීම් (මෙම විගණනයේදී ක්‍රියාත්මක **නොකරන්න**)

1. **[විවේචනාත්මක] අනාගත-අස්වැන්න rules/app අසංගතතාව විසඳන්න** (අවදානම #1). Rules ප්‍රතිෂ්ඨාපනය vs විශේෂාංගය ඉවත් කිරීම තීරණය කර, `firestore.rules`, Kotlin lifecycle code, සහ emulator rules tests සංගත කරන්න. ඉන් පසුව දක්වා, EXPECTED listings ක්‍රියා නොකරන බව සලකන්න.
2. **[විවේචනාත්මක] Harvest lifecycle සඳහා emulator rules tests නැවත එක් කරන්න** (EXPECTED සෑදීම, ready/failed සලකුණු කිරීම, ඇස්තමේන්තු යාවත්කාලීනය, pre-harvest request create/cancel, EXPECTED අතරතුර accept අවහිර වීම) මෙවැනි regression ස්වයංක්‍රීයව අල්ලා ගැනීමට.
3. **[ඉහළ] සැබෑ repository transactions ධාවනය කරන අවම වශයෙන් එක integration test එකක් එක් කරන්න** (උදා. Firestore emulator හරහා) app හි **සැබෑ write payloads** භාවිතා කරමින්, එවිට client writes fakes වලට නොව rules වලට එරෙහිව සත්‍යාපනය වේ.
4. **[ඉහළ] Deploy කළ rules** සමාදාන කළ local rules සමඟ තහවුරු කර පෙළගස්වන්න; deploy පියවර ලේඛනගත කරන්න. Production භාවිතයට පෙර `agroroute` project තත්ත්වය සත්‍යාපනය කරන්න.
5. **[මධ්‍යම] ඉටු කිරීමේ මාරුවීම් role අනුව සීමා කරන්න** rules සහ UI දෙකෙහිම (උදා. සැපයුම්කරු IN_TRANSIT/DELIVERED ඉදිරියට ගෙන යයි; ගොවියාට cancel කළ හැක), එය අදහස් කළ policy එක නම්.
6. **[මධ්‍යම] ඇස්තමේන්තුව වෙනස් වන විට / mark-ready විට pre-harvest requests හසුරුවන්න** — ඇස්තමේන්තුව පහත වැටේ නම් හෝ සත්‍ය ප්‍රමාණය ප්‍රමාණවත් නොවේ නම් පොරොත්තු requests නැවත validate හෝ ස්වයංක්‍රීයව cancel/දැනුම් දෙන්න.
7. **[මධ්‍යම] Room දත්ත ආරක්ෂා කරන්න** — schemas export කරන්න, migration tests එක් කරන්න, production සඳහා `fallbackToDestructiveMigration()` නැවත සලකා බලන්න.
8. **[මධ්‍යම] Marketplace පැරණි දත්ත අඩු කරන්න** — සැපයුම්කරු listings/තොගය සඳහා Firestore snapshot listener එකක් (හෝ pull-to-refresh sync) එක් කරන්න.
9. **[අඩු] Dead backend/UI සම්බන්ධ කරන්න හෝ ඉවත් කරන්න** — password reset, සැපයුම්කරු My-Bids/withdraw, Settings/Help අයිතම, mock notifications, භාවිතා නොකරන `auth/WelcomeScreen.kt`, හිස් error-clear lambdas; `supplierRoutes` වලට `PlaceOrderRequest` එක් කරන්න.
10. **[අඩු] සැබෑ Cloudinary upload එකක් device එකේ සත්‍යාපනය කරන්න** සහ unsigned preset scope එක තහවුරු කරන්න; API secret client එකෙන් බැහැර තබන්න (දැන් නිවැරදියි).
11. **[අඩු] Repo සනීපාරක්ෂාව** — `firestore-debug.log` git-ignore/untrack කරන්න; README/`supplier_module_documentation.md` ක්‍රියාත්මක විෂය පථයට අනුව නිවැරදි කරන්න (Driver role, "paid" status, cold-chain/GPS/cold-storage, `core/service/`, tab 5ක supplier nav, My-Bids/Item-Details/profile-edit ප්‍රකාශ ඉවත් කරන්න).

---

### සත්‍යාපන-තත්ත්ව යතුර (විගණන සටහනට අනුව)
1. **Source හි ක්‍රියාත්මකයි** — "Code" තීරුවේ ✅ ලෙස සලකුණු කළ සියල්ලටම ඔව්.
2. **UI හරහා ළඟා විය හැක** — ඔව්, මේවා හැර: අනාගත-අස්වැන්න ක්‍රියා (වත්මන් rules යටතේ EXPECTED listings සෑදිය නොහැකි නිසා ළඟා විය නොහැක), සැපයුම්කරු bid බැලීම/withdraw (screen නැත), password reset (UI නැත).
3. **Locally/remotely persist වේ** — සියලු මූලික entities සඳහා Room + Firestore; drafts අරමුණින්ම Room පමණි.
4. **Tests වලින් ආවරණය** — fake-පාදක JVM unit tests 83ක් + emulator rules tests 22ක් සමත් වේ; **instrumented, migration, සැබෑ-transaction, හෝ සැබෑ-upload tests නැත.**
5. **Local project හි config කර ඇත** — `google-services.json`, `firebase.json`, `firebase.rules-test.json`, `.firebaserc` (project `agroroute`), Cloudinary config ඇත.
6. **Firebase වෙත deploy කර ඇත** — **සත්‍යාපනය නොකළ** (පරීක්ෂා නොකළා; remote access අවශ්‍ය වේ).
7. **Live service එකකට එරෙහිව සත්‍යාපනය කළා** — **නැත** (live Auth/Firestore/Storage/Cloudinary calls සිදු කර නැත).

*විගණනය අවසන්. මෙම විගණනයෙන් app ගොනු, tests, Gradle ගොනු, Firebase/Cloudinary configuration, schemas, හෝ security rules කිසිවක් වෙනස් කර නැත; නිර්මාණය කළ එකම ගොනුව `business-logic-audit/README.md` වේ.*
