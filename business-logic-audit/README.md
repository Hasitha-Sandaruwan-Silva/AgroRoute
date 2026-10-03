# AgroRoute — Business-Logic Audit (A-to-Z)

**Audit date:** 2026-10-03
**Branch audited:** `v3`
**Commit audited:** `899b893` ("beffore editing") — working tree **clean** at time of writing
**Scope:** Inspection & verification only. **No app source, tests, Gradle, Firebase/Cloudinary config, schemas, or rules were modified.** Nothing was deployed, committed, pushed, or published. The only file created is this report.

> ⚠️ **Note on repository state during the audit.** When this audit began, `git status` showed five **uncommitted** modified files (`FarmerOrderRequestsViewModel.kt`, `OrderRequestUseCases.kt`, `OrderRequestFlowTest.kt`, `firestore.rules`, `rules-tests/firestore.rules.test.mjs`) plus an untracked `firestore-debug.log`. **Partway through the audit, an external actor (the user/IDE — not this audit) created commit `899b893`**, which committed those changes and the `firestore-debug.log` artifact. The working tree is now clean and `v3` is 1 commit ahead of `origin/v3` (not pushed). All findings below describe the **current committed state** (`899b893`), which is identical in content to the working tree that existed at audit start. This audit did not create, amend, or revert any commit and did not alter any tracked file.

---

## A. Executive Summary

AgroRoute is a Kotlin/Jetpack-Compose Android app (MVVM + Repository + Hilt + Room + Firebase) connecting **Farmers** (list harvests, review bids/order-requests, fulfil orders) and **Suppliers** (browse marketplace, bid or request fixed-price orders, track orders). The codebase is well-structured and the **core transactional money/stock paths are soundly implemented in client code**. However, there is one **critical committed inconsistency** and several partial/unverified areas.

### What works end-to-end (in code; live-service verification not performed)
- **Authentication & roles** — email/password register+login, Google sign-in with first-time role selection, role persistence in Firestore `users/{uid}` + Room, **role immutability** enforced in both client (`AuthRepositoryImpl.updateProfile` :141) and rules (`firestore.rules` :60). Role-based start destination and route guards (`NavGraph.kt` :60-100).
- **Farmer listings (READY harvests)** — create/validate/publish, edit, close, toggle bidding, delete, draft (local-only). Ownership enforced client-side and in rules. Persists to Firestore then mirrors to Room.
- **Supplier marketplace** — sees **all eligible OPEN published listings** (not only ones it bid on); eligibility filter `isEligibleForMarketplace()`; search/filter; correct routing to Place-Bid vs Place-Order-Request based on `biddingEnabled`.
- **Bidding (farmer side)** — place bid (supplier), farmer review/accept/reject; **accept runs a Firestore transaction** that flips bid→ACCEPTED, decrements stock exactly once via `recordSale()`, and creates the coupled order atomically (`OrderRepositoryImpl.acceptBidAndCreateOrder` :67-110). Duplicate acceptance is blocked by the in-transaction `bid.status == PENDING` guard.
- **Fixed-price order requests** — supplier places request (validated against stock/estimate), farmer accepts (transaction creates order + decrements stock once + marks request ACCEPTED) or rejects; supplier cancels. **Stock changes only on acceptance** (`OrderRepositoryImpl.acceptOrderRequestAndCreateOrder` :112-170).
- **Orders & fulfilment** — farmer/supplier order lists + details with ownership filtering; status transitions AWAITING_PICKUP→IN_TRANSIT→DELIVERED (→CANCELLED) with stock **released** on cancel via `releaseSale()` (`OrderRepositoryImpl.updateOrderStatus` :172-212).
- **Listing photos** — Android `content://` URI handling, Cloudinary **unsigned** multipart upload (no API secret in app), progress/retry/error states, returned `secure_url` saved into `imageUrls`; legacy Firebase Storage URLs still render via Coil.

### What is partial
- **Supplier bid management** — `getBidsBySupplier()` and `withdrawBid()` exist in the data layer (`BidRepositoryImpl` :39, :103) but are **not referenced by any screen/ViewModel**. There is **no "My Bids" screen and no withdraw UI**; supplier bids are effectively fire-and-forget. (Contradicts `supplier_module_documentation.md` §6.)
- **Password reset** — `AuthService.sendPasswordResetEmail()` (:69) exists but is **never called** from any ViewModel/UseCase/Screen. No "Forgot password" flow is wired.
- **Order-status authority** — **both** farmer and supplier can advance an order to IN_TRANSIT/DELIVERED; rules allow any participant (`firestore.rules` :250-266). No role restriction on who drives fulfilment.

### What is missing
- **Payments** — **no payment processing exists.** No payment SDK, no `PAID` status, no payment field on `Order`/`Bid`. `OrderStatus` = {AWAITING_PICKUP, IN_TRANSIT, DELIVERED, CANCELLED}. The UI explicitly disclaims it: *"Based on delivered orders; this does not indicate payment status."* (`FarmerOrdersScreen.kt` :130). Displaying a price or creating an order is **not** a payment.
- **README-advertised features** — Driver role, cold-chain, GPS transport pooling, cold-storage slot booking, and a `core/service/` layer are **not present** in code (see §G / "Prior claims").

### What is broken (critical)
- **Future-harvest (EXPECTED) listings are non-functional against the committed security rules.** The Kotlin app fully implements the future-harvest lifecycle (create EXPECTED listing, update estimate, mark ready with actual quantity, mark failed, pre-harvest requests, auto-cancel of pre-harvest requests on failure). **The committed `firestore.rules` contains none of the required support** (`grep` for `harvestStatus|isExpectedHarvest|estimatedQty|isPreHarvest` in `HEAD:firestore.rules` = **0 matches**). Consequences if these rules are deployed:
  - Creating an EXPECTED listing is **rejected** — the create rule requires `availableStock > 0` (:80), but EXPECTED listings are built with `availableQuantity = 0.0` (`CreateListingScreen.kt` :252-253).
  - `markHarvestReady` / `markHarvestFailed` / estimate updates are **rejected** — they change `harvestStatus`/`estimatedQuantity`/`harvestFailureReason`/`harvestUpdatedAt`, which are **not** in the update rule's allowed `affectedKeys` (:90-94).
  - `markHarvestFailed`'s atomic cancellation of pending pre-harvest requests is **rejected** — the farmer-cancel-pre-harvest clause is absent from the order_requests update rule (:161-189).
  - Pre-harvest order requests are **rejected** — the create rule requires `requestedQuantity <= availableStock` (:154-155), which is 0 for EXPECTED listings.
  - Because EXPECTED listings cannot be created, the "Mark ready / Mark failed" buttons (shown only when `harvestStatus == EXPECTED`, `MyListingsScreen.kt` :473) are **unreachable in practice** under these rules.
  - **The test suite does not catch this:** JVM unit tests use in-memory fakes, and the future-harvest emulator rules tests were **deleted** in the same change (committed `rules-tests/firestore.rules.test.mjs` now has 22 tests, none covering harvest lifecycle).

### What is unverified (cannot be confirmed without mutating/remote access)
- **Deployed vs local rules** — whether the rules currently deployed to the `agroroute` Firebase project match the local `firestore.rules`. Confirming this requires authenticated access to the live project (a read-only `firebase` rules fetch was **not** attempted to avoid touching remote state / requiring login). **Status: UNVERIFIED.**
- **Live Firebase Auth/Firestore/Storage behaviour** — no real sign-in, write, or read was executed against production Firebase (no credentials/permission; would mutate remote state). All "persists/backend" verdicts are based on **code + local emulator rules tests**, not live calls.
- **Real Cloudinary upload** — only faked in tests (`CloudinaryUploadTest` uses `FakeCloudinaryStorage`). Whether the unsigned preset exists/is correctly scoped in the Cloudinary console was **not** verified (would require a real network upload).
- **Real device photo selection / camera capture** — not exercised.

---

## B. Feature / Flow Matrix

Legend — **Code**: implemented in source · **UI**: reachable through the app · **Persist**: persisted locally (Room) / remotely (Firestore) · **Rules**: covered by local security rules · **Tests**: automated coverage · Verdict keys: ✅ works (code-level), ⚠️ partial, ❌ broken/missing, ❓ unverified live.

| Feature/flow | Code implementation | Reachable in app UI | Persistence/backend | Security-rule status | Tests | Verdict |
|---|---|---|---|---|---|---|
| Email/password register | ✅ `AuthRepositoryImpl.register` :112 | ✅ RegisterScreen | ✅ Firestore `users` + Room | ✅ users create :52-55 | ⚠️ fakes only (`AuthRoleRoutingTest`) | ✅ / ❓live |
| Email/password login | ✅ `login`+`restoreSession` :46-66 | ✅ LoginScreen | ✅ reads Firestore profile → Room | ✅ users get :50 | ⚠️ fakes only | ✅ / ❓live |
| Google sign-in | ✅ `signInWithGoogle` :68 | ✅ LoginScreen + Credential Manager | ✅ Firestore profile | ✅ users create/get | ⚠️ fakes only | ✅ / ❓live config |
| Role selection (first-time Google) | ✅ `completeGoogleSignIn` :87 | ✅ GoogleRoleSelectionScreen | ✅ writes role to Firestore+Room | ✅ role in [FARMER,SUPPLIER] :55 | ⚠️ `AuthRoleRoutingTest` (3) | ✅ |
| Role persistence & immutability | ✅ :141 | ✅ n/a (enforced) | ✅ | ✅ role cannot change :60 | ⚠️ partial | ✅ |
| Profile load | ✅ `getCurrentUserProfile` :38 | ✅ Profile screens | ✅ Room-first | ✅ users get :50 | ⚠️ | ✅ |
| Sign-out | ✅ `logout` :130 | ✅ FarmerTopBar/Profile, SupplierProfile | ✅ clears Room profile | n/a | ⚠️ | ✅ |
| Farmer/Supplier nav & access guards | ✅ `NavGraph` :60-100 | ✅ | n/a | n/a | ✅ `AuthRoleRoutingTest`, `FarmerNavigationTest` | ✅ (1 gap, §D-LOW) |
| Create/publish listing (READY) | ✅ `CreateListingUseCase`+`saveListing` | ✅ CreateListingScreen | ✅ Firestore→Room | ✅ create :73-80 | ✅ `HarvestListingRulesTest` (30, fakes) + emulator | ✅ |
| Validate listing fields | ✅ use case :29-40 | ✅ inline errors | n/a | ⚠️ rules check subset | ✅ | ✅ |
| Edit listing (READY) | ✅ `updateListing` :84-157 | ✅ EditListing | ✅ Firestore update→Room | ⚠️ see §D-MED-3 | ✅ `UpdateListingUseCaseTest` (5) | ✅ |
| Draft listing (local only) | ✅ `saveDraft` :159 | ✅ "Save draft" | ✅ **Room only** (by design) | n/a (never sent) | ⚠️ | ✅ |
| Close listing | ✅ `closeForNewBids` | ✅ MyListings dialog | ✅ | ✅ status change allowed | ⚠️ | ✅ |
| Toggle bidding | ✅ `toggleBiddingMode` :204 | ✅ MyListings switch | ✅ | ✅ | ⚠️ | ✅ |
| Delete listing | ✅ `deleteListing` :168 | ✅ | ✅ | ✅ delete :96 | ⚠️ | ✅ |
| Listing ownership | ✅ checks in repo/use cases | ✅ | ✅ | ✅ farmerId==uid | ✅ | ✅ |
| **Future-harvest: create EXPECTED** | ✅ model+screen+repo | ✅ (status selector) | ❌ **rules reject** | ❌ create needs stock>0 :80 | ❌ **tests deleted** | ❌ **BROKEN** |
| **Future-harvest: update estimate** | ✅ `updateHarvestEstimate` | ✅ EditListing | ❌ **rules reject** | ❌ keys not allowed :90-94 | ❌ deleted | ❌ **BROKEN** |
| **Future-harvest: mark ready** | ✅ `markHarvestReady` :218 | ✅ dialog (EXPECTED only) | ❌ **rules reject** | ❌ harvestStatus key blocked | ❌ deleted | ❌ **BROKEN** |
| **Future-harvest: mark failed + cancel reqs** | ✅ `markHarvestFailed` :243 (txn) | ✅ dialog | ❌ **rules reject** | ❌ listing keys + farmer-cancel clause removed | ❌ deleted | ❌ **BROKEN** |
| **Pre-harvest request** | ✅ `placeOrderRequest(isPreHarvest)` :77 | ✅ PlaceOrderRequestScreen | ❌ **rules reject** | ❌ qty<=availableStock(=0) :154 | ❌ deleted | ❌ **BROKEN** |
| Listing eligibility/visibility | ✅ `isEligibleForMarketplace` etc. | ✅ marketplace filter | ✅ | ✅ supplier read OPEN+published :68-71 | ✅ `MarketplaceEligibilityTest` (5) | ✅ |
| Suppliers see all eligible (not just bid-on) | ✅ `syncListings` status=OPEN :195 | ✅ | ✅ | ✅ list rule :68 | ⚠️ | ✅ |
| Photo select + URI handling | ✅ PhotoPicker / `content://` | ✅ | n/a | n/a | ⚠️ | ✅ |
| Photo preview/progress/retry/error | ✅ CreateListingScreen | ✅ | n/a | n/a | ✅ `CloudinaryUploadTest` (13, fakes) | ✅ |
| Cloudinary unsigned upload | ✅ `CloudinaryPhotoStorage` | ✅ | ✅ returns `secure_url`→`imageUrls` | n/a (Cloudinary, not FB) | ⚠️ **fake only** | ✅code / ❓real upload |
| Legacy Firebase Storage URLs | ✅ Coil loads URL; `StorageService` kept | ✅ | ✅ (existing docs) | ✅ storage.rules read :23-29 | ❌ | ✅ / ❓live |
| Marketplace queries/filters | ✅ `MarketplaceViewModel`+screen | ✅ | ✅ Room cache | ✅ | ✅ eligibility test | ✅ |
| Listing details (price/qty/date/bidding/avail) | ✅ BrowseListings card | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| Place Bid action gating | ✅ `acceptsNewBids()` | ✅ only when allowed | ✅ | ✅ bid create needs biddingEnabled :111 | ⚠️ | ✅ |
| Fixed-price request gating | ✅ `acceptsDirectOrders()` | ✅ only when allowed | ✅ | ✅ request create needs !biddingEnabled :152 | ✅ emulator | ✅ |
| Bid create/validate/ownership | ✅ `PlaceBidUseCase`+`placeBid` txn :49 | ✅ | ✅ Firestore→Room | ✅ supplierId==uid, PENDING :105-113 | ⚠️ fakes | ✅ |
| Farmer review/accept/reject bid | ✅ `AcceptBidUseCase`/`RejectBidUseCase` | ✅ FarmerBids/ViewBids | ✅ txn | ✅ accept coupling :128-133 | ✅ `HarvestListingRulesTest` | ✅ |
| Duplicate-accept protection (bid) | ✅ in-txn PENDING guard :89 | ✅ | ✅ | ✅ | ✅ (fake) `OrderRequestFlowTest` | ✅ |
| Order creation after accepted bid | ✅ txn sets order :103 | ✅ | ✅ | ✅ orderId==bidId :204 | ✅ | ✅ |
| **Supplier view/withdraw own bids** | ⚠️ repo methods exist, **no UI** | ❌ **not reachable** | ✅ (if called) | ✅ rules allow :125-127 | ❌ | ⚠️ **PARTIAL/MISSING UI** |
| Requested-quantity limits | ✅ use case + repo + rules | ✅ | ✅ | ✅ :149,:154 | ✅ emulator | ✅ |
| Price integrity (listedPrice==reservePrice) | ✅ `PlaceOrderRequestUseCase` :68 | ✅ | ✅ | ✅ :158-159 | ✅ emulator | ✅ |
| Request/listing ownership | ✅ checks | ✅ | ✅ | ✅ farmerId match :156 | ✅ emulator | ✅ |
| Farmer accept/reject request | ✅ `AcceptOrderRequestUseCase`/Reject | ✅ FarmerOrderRequests | ✅ txn | ✅ accept coupling :178-188 | ✅ `OrderRequestFlowTest` (25) + emulator | ✅ |
| Supplier cancel request | ✅ `cancelRequest` | ✅ SupplierOrders | ✅ | ✅ supplier CANCELLED :175-177 | ✅ emulator | ✅ |
| Order created only at correct point | ✅ only on accept txn | ✅ | ✅ | ✅ | ✅ | ✅ |
| Pre-harvest leaves stock unchanged | ✅ accept requires READY :136 | ✅ | ✅ | ⚠️ rules dropped READY guard :235 (client still enforces) | ✅ (fake) | ✅code / ❌ pre-harvest path broken upstream |
| Stock decremented exactly once | ✅ `recordSale` in txn | ✅ | ✅ | ✅ soldQty/avail formulas :217-222,:242-247 | ✅ (fake) duplicate-tap test | ✅ |
| Insufficient stock / oversell protection | ✅ client + txn + rules | ✅ | ✅ | ✅ qty<=availableStock | ✅ emulator + fake | ✅ |
| Pending/rejected/cancelled don't change stock | ✅ | ✅ | ✅ | ✅ | ✅ `cancelledAndRejected…` test | ✅ |
| Firestore↔Room consistency | ✅ write-Firestore-then-Room; sync overwrites | ✅ | ✅ | n/a | ❌ no integration test | ⚠️ (see §D-MED) |
| Farmer/Supplier order lists + details | ✅ DAOs + screens | ✅ | ✅ Room-first | ✅ get/list ownership :195-197 | ⚠️ | ✅ |
| Ownership filtering (orders) | ✅ `getOrdersForFarmer/Supplier` | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| Order status transitions + authority | ✅ `updateOrderStatus` :172 | ✅ both detail screens | ✅ | ⚠️ **any participant** :250-266 | ⚠️ | ⚠️ (see §D-MED-4) |
| Cancel order releases stock | ✅ `releaseSale` txn :192 | ✅ | ✅ | ✅ idempotent | ⚠️ | ✅ |
| Empty/loading/error states | ✅ Result + UI states | ✅ | n/a | n/a | ⚠️ | ✅ |
| **Payment processing** | ❌ **none** | ❌ | ❌ | ❌ | ❌ | ❌ **DOES NOT EXIST** |
| Room entities/DAOs/converters | ✅ 5 entities, converters | ✅ | ✅ | n/a | ❌ no DAO/migration test | ⚠️ |
| Room migrations (1→5) | ✅ `DatabaseMigrations` | ✅ | ✅ | n/a | ❌ **no migration test**, `exportSchema=false` | ⚠️ (see §D-MED-6) |
| Offline behaviour | ✅ Room-first + connectivity sync | ✅ | ✅ | n/a | ❌ | ⚠️ staleness risk |
| Deployed rules == local rules | ❓ | n/a | ❓ | ❓ | ❌ | ❓ **UNVERIFIED** |

---

## C. Main Farmer→Supplier Journey (step-by-step, as implemented)

This traces the **fixed-price** path and the **bidding** path. Line references are to current `HEAD`.

1. **Farmer creates a listing.** `CreateListingScreen.buildListing()` (:228) → `ListingViewModel.createListing()` (:116) → `CreateListingUseCase` (validates cropName/category/quantity≥0/price; uploads a `content://` photo to Cloudinary and replaces it with the `secure_url`) → `HarvestListingRepositoryImpl.saveListing()` (:73) writes to Firestore `listings/{id}` (merge) then mirrors to Room. Rules require farmer role, `farmerId==uid`, `status==OPEN`, `isDraft==false`, and **`availableStock>0`** (`firestore.rules` :73-80).
   - ⚠️ If the farmer selects **EXPECTED (future-harvest)**, `availableQuantity` is set to `0.0` (`CreateListingScreen.kt` :252) so `availableStock==0` → **the committed create rule rejects the write.** The future-harvest journey stops here under current rules.
2. **Marketplace visibility.** Supplier's `MarketplaceViewModel` syncs on connectivity (`syncListings()` queries Firestore `status==OPEN`, drops drafts, for SUPPLIER — `HarvestListingRepositoryImpl` :195-199) into Room; `BrowseListingsScreen` reads Room and filters by `isSupplierMarketplaceEligible()` (:74) = `isEligibleForMarketplace()` (OPEN, not draft, and confirmed stock **or** EXPECTED). Rules let any supplier read published OPEN listings (`firestore.rules` :68-71), so suppliers see **all** eligible listings, not only ones they interacted with.
3. **Supplier acts.** Per card, `acceptsNewBids()` → **Place Bid** route; `acceptsDirectOrders()` → **Place Order Request** route (`BrowseListingsScreen` :197-203).
   - **Bid path:** `PlaceBidUseCase` (validates price>0, supplier role/ownership, `listing.acceptsNewBids()`) → `BidRepositoryImpl.placeBid()` runs a transaction reading the listing and writing `bids/{bidId}` with `status=PENDING` (:60-65). Rules require supplier ownership, PENDING, listing OPEN+biddingEnabled+published+stock>0 (:105-113).
   - **Fixed-price path:** `PlaceOrderRequestUseCase` (validates qty, supplier role, `acceptsDirectOrders()`, qty ≤ availableStock or ≤ estimate for pre-harvest) → `OrderRequestRepositoryImpl.placeOrderRequest()` transaction writes `order_requests/{id}` `status=PENDING` (:57-95). Rules require supplier ownership, PENDING, qty>0, listing OPEN+!biddingEnabled+published, `qty ≤ availableStock`, `farmerId` match, and **`listedPrice == reservePrice`** (:145-159). **No stock is reserved at request time.**
4. **Farmer decision.**
   - **Bids:** `FarmerBidsScreen`/`ViewBidsScreen` → `AcceptBidUseCase` (verifies farmer, bid PENDING, listing OPEN+bidding+stock>0; builds an `Order` with `orderId=bidId`, `quantity = listing.availableStock()` — i.e. the winning bid takes the **entire remaining stock**) → `OrderRepositoryImpl.acceptBidAndCreateOrder()` **transaction** (:67-110): re-reads listing+bid, asserts listing OPEN/bidding/!draft, **`harvestStatus==READY`**, bid PENDING & ownership match, `0 < qty ≤ availableStock`; then `recordSale(qty)` (decrement available, increment sold, FULFILLED if sold out), bid→ACCEPTED, write order — all atomically. Reject → `RejectBidUseCase` → `updateBidStatus(REJECTED)` (no stock change).
   - **Order requests:** `FarmerOrderRequestsScreen` → `AcceptOrderRequestUseCase` (verifies farmer, request PENDING, listing ownership, **`harvestStatus==READY`**, `requestedQuantity ≤ availableStock`) → `OrderRepositoryImpl.acceptOrderRequestAndCreateOrder()` **transaction** (:112-170): re-reads request+listing, asserts request PENDING, listing OPEN/!draft/!bidding, **READY**, `0 < qty ≤ availableStock`; creates `Order(orderId=requestId, bidId="")`, `recordSale(qty)`, request→ACCEPTED — atomically. Reject → `updateRequestStatus(REJECTED)` (no stock change). Supplier cancel → `CANCELLED` (no stock change).
5. **Order creation.** Happens **only** inside the accept transactions above (never at bid/request time). Rules independently enforce the coupling: an order may be created only by the listing's farmer, with `orderId==bidId` (bid path) or `bidId==''` + matching `order_requests/{orderId}` (request path), with the bid/request flipped to ACCEPTED **in the same write**, and with `soldQuantity`/`availableStock` arithmetic matching exactly (`firestore.rules` :199-248).
6. **Stock update.** `recordSale()` (`HarvestListing.kt` :132) decrements `availableQuantity` and increments `soldQuantity` **once**, inside the transaction, guarded by the PENDING status check → repeated taps / concurrent accepts cannot double-decrement (the second attempt re-reads status≠PENDING and aborts). Verified by `OrderRequestFlowTest.repeatedTapsOrDuplicateAcceptanceCannotDeductStockMoreThanOnce` (against a fake that mirrors the guards).
7. **Fulfilment.** `FarmerOrderDetailScreen` / `SupplierOrderDetailScreen` → `updateOrderStatus()` (:172). Non-cancel transitions are a Firestore read-then-write; **CANCELLED** runs a transaction that calls `releaseSale()` to return stock and is idempotent (already-cancelled returns early). Rules constrain transitions to AWAITING_PICKUP→{IN_TRANSIT,CANCELLED}, IN_TRANSIT→{DELIVERED,CANCELLED}, DELIVERED→{CANCELLED} and allow **either participant** to make them (:250-266). Room is updated after each successful Firestore write; `syncOrders()` refreshes from Firestore.

**Net:** For **READY** listings the journey is coherent and transactionally safe in code. For **EXPECTED/future-harvest** listings the journey is **broken at step 1** under the committed rules.

---

## D. Data-Integrity & Security Risks (ranked)

### 🔴 HIGH
1. **Future-harvest rules/app divergence (committed).** The app implements the full EXPECTED-harvest lifecycle but `HEAD:firestore.rules` has **zero** support for it (no `harvestStatus`/`estimatedQuantity`/`isPreHarvest` handling; create requires `availableStock>0`; update `affectedKeys` excludes harvest fields; farmer pre-harvest-cancel clause removed). If deployed, every future-harvest write fails `PERMISSION_DENIED`, and the feature is unreachable. **Why it slipped through:** JVM unit tests use fakes (never touch rules), and the emulator rules tests that covered harvest lifecycle were **deleted** in the same commit. *Files:* `firestore.rules` :73-97,:145-192,:235; `HarvestListingRepositoryImpl` :218-291; `CreateListingScreen.kt` :252; `OrderRequestRepositoryImpl` :77-91.
2. **Deployed-vs-local rules unknown.** It cannot be confirmed (without authenticated, potentially mutating remote access) whether production rules match the local file. Given risk #1, the production behaviour of future-harvest is indeterminate. **UNVERIFIED.**
3. **No integration test exercises the real Firestore transactions or the app's actual write payloads.** `OrderRepositoryImpl`/`OrderRequestRepositoryImpl`/`BidRepositoryImpl` transactions are validated only by **fakes that re-implement the same guards** (`OrderRequestFlowTest` `FakeOrderRepository` :705-769). The emulator rules tests use **simplified payloads** (e.g. the edit test writes only `cropName`, :76-82, whereas `updateListing` writes a 14-field map incl. harvest fields, `HarvestListingRepositoryImpl` :135-155). Real client-write-vs-rules drift is therefore uncaught.

### 🟠 MEDIUM
4. **Fulfilment authority is not role-restricted.** Either party can set IN_TRANSIT/DELIVERED (`firestore.rules` :250-266; both detail screens expose the same buttons). A supplier could self-mark an order DELIVERED. Business-rule overlap, not an ownership breach.
5. **Pre-harvest request lifecycle gaps.** `markHarvestReady(actual)` and `updateHarvestEstimate()` do **not** re-validate or adjust existing PENDING pre-harvest requests. If the estimate is lowered, or actual ready-quantity < requested, the request stays PENDING but can never be accepted (`AcceptOrderRequestUseCase` :63 rejects on insufficient stock). Only `markHarvestFailed` cancels pending pre-harvest requests (:243-291). Suppliers can be left with stale, un-acceptable requests. (Moot until risk #1 is fixed, but latent.)
6. **`fallbackToDestructiveMigration()` + `exportSchema=false` + no migration tests** (`DatabaseModule.kt` :41, `AppDatabase.kt` :25). A missing/incorrect migration silently **wipes local Room data**. Migrations 1→5 exist but are unverified by any test.
7. **Marketplace/offline staleness.** Supplier listings come from the Room cache, refreshed only by `syncListings()` on connectivity changes (`MarketplaceViewModel` :34-38) — there is **no live Firestore snapshot listener** for the marketplace. Stock/status shown can be stale between syncs; two suppliers could act on outdated availability (server-side transaction still prevents oversell, but UX shows stale data).
8. **`updateOrderStatus` non-cancel path is non-transactional** (`getOnce` then `set`, :202-210). Low impact (status-only), but a concurrent cancel could race the read.

### 🟡 LOW
9. **Password reset not wired** — `sendPasswordResetEmail` is dead code (no UI caller).
10. **Supplier cannot view/withdraw bids** — backend exists, no screen; bids are fire-and-forget for suppliers.
11. **Dead/placeholder UI** — FarmerTopBar "Settings" (:351) and "Help & Support" (:361) only close the menu; the notification centre is hard-coded mock data (:127-157); `features/auth/WelcomeScreen.kt` is an unused duplicate; empty error-clear lambdas in `PlaceBidScreen` :94-95 / `PlaceOrderRequestScreen` :96-97.
12. **`PlaceOrderRequest` route missing from `supplierRoutes`** (`SupplierTopBar.kt` :154-158) → no supplier scaffold/top-bar and **no NavGraph role-guard redirect** for that route; mitigated by data-layer role checks in `PlaceOrderRequestViewModel`.
13. **`firestore-debug.log` (0-byte emulator artifact) is committed/tracked** — repo hygiene; should be git-ignored.
14. **`storage.rules` governs an effectively unused path.** New uploads go to Cloudinary; the Firebase Storage `listing_images/...` rules only matter for legacy display. Not harmful, but the rule set no longer matches the active upload path.
15. **Documentation overstates implementation** (see "Prior claims" below) — risk of false confidence from README/spec.

### Prior claims vs current workspace
- Root `README.md`: claims **Driver role** (code has only FARMER/SUPPLIER, `Enums.kt` :8-11), order status **"through to paid/delivered"** (no PAID status), bids **"with payment terms"** (no such field), **cold-chain / GPS transport pooling / cold-storage booking** (not implemented), and a **`core/service/` layer replacing per-action UseCases** (no `core/service/` dir exists; the code uses per-action UseCases). `google-services.json` is committed (normal for Firebase, but note it is tracked).
- `supplier_module_documentation.md`: describes a **5-tab supplier bottom nav incl. "Bids"** (actual: 4 tabs, no Bids — `SupplierBottomBar.kt` :55-60), a dedicated **Item Details screen** (actual: card → directly to PlaceBid/PlaceOrderRequest), a **My Bids screen with Withdraw** (not implemented), **Supplier Home pending-bids counter** (not found), and **profile editing / photo upload / address fields** (SupplierProfileScreen is read-only except logout).

---

## E. Remaining Manual Steps (NOT completed by this audit)

These require decisions/permissions beyond a read-only audit and were **deliberately not performed**:

1. **Reconcile the future-harvest divergence (pick one):**
   - (a) **Restore rules support** — re-add `isExpectedHarvest()`/`estimatedQty()` helpers, allow EXPECTED create with `estimatedQuantity>0`, add `harvestStatus`/`estimatedQuantity`/`harvestFailureReason`/`harvestUpdatedAt` to the listing-update `affectedKeys`, re-add the farmer pre-harvest-cancel clause on `order_requests`, and re-add the `harvestStatus=='READY'` guard on order creation; **or**
   - (b) **Remove the future-harvest feature** from the app (models, repository, use cases, screens, enums, migration columns).
   - Then **re-add the deleted emulator rules tests** for the harvest lifecycle. *(The prior committed history `414a0d2`/HEAD~ contained these rules+tests; they were stripped in `899b893`.)*
2. **Verify which rules are deployed** to the `agroroute` project and, if intended, deploy the reconciled `firestore.rules`/`storage.rules` (`firebase deploy --only firestore,storage`). **Not done — mutating + requires permission.**
3. **Confirm Cloudinary** unsigned preset exists and is scoped to the listing-photos folder; perform a **real device upload** to validate end-to-end (current tests only fake it). **Not done.**
4. **Confirm Firebase project config** — `app/google-services.json` points to the intended project; Email/Password and Google providers enabled in the console; run a **real sign-in** smoke test. **Not verified live.**
5. **Wire or remove dead backend** — password reset UI, supplier "My Bids"/withdraw screen, or delete the unused methods. **Not done.**
6. **Repo hygiene** — add `firestore-debug.log` (and `*.log`) to `.gitignore` and untrack it. **Not done.**
7. **Add instrumented/migration tests** (`androidTest`) and Room schema export to protect migrations. **Not done.**
8. **Decide fulfilment authority** — restrict IN_TRANSIT/DELIVERED transitions to the appropriate role in rules + UI if that is the intended business rule. **Not done.**

---

## F. Commands Run & Actual Results

All commands were run from `D:\Projects\AgroRoute`. Nothing was deployed, committed, pushed, or mutated remotely.

| # | Command | Result |
|---|---|---|
| 1 | `git status` / `git branch -vv` / `git log --oneline -20` | Branch `v3`; initially 5 modified files + untracked `firestore-debug.log`; recent commits `414a0d2 maybe`, `143cdcb with errors`, `21f5906 Add supplier order request flow`. |
| 2 | `git diff` (rules, tests, Kotlin) | Showed future-harvest logic being **removed** from `firestore.rules` & `rules-tests`, `combine()` added to `FarmerOrderRequestsViewModel`, stock guard added to `AcceptOrderRequestUseCase`, 3 new tests added to `OrderRequestFlowTest`. |
| 3 | `git status` (re-check, later) | **Clean.** `git log` showed a **new commit `899b893 "beffore editing"`** (2026-10-03 20:14) created externally mid-audit; `v3` ahead of `origin/v3` by 1 (not pushed). `firestore-debug.log` now tracked. |
| 4 | `git show HEAD:firestore.rules \| grep -cE "harvestStatus\|isExpectedHarvest\|estimatedQty\|isPreHarvest"` | **0** — committed rules have no future-harvest support. |
| 5 | `git show HEAD:.../HarvestListingRepositoryImpl.kt \| grep -cE "markHarvestReady\|markHarvestFailed"` | **6** — committed app code implements the harvest lifecycle. (Confirms divergence #1.) |
| 6 | `./gradlew testDebugUnitTest --offline` (first run) | `BUILD SUCCESSFUL in 1s` — but all tasks **UP-TO-DATE** (cached; tests did not execute). |
| 7 | `./gradlew testDebugUnitTest --offline --rerun-tasks` | **`BUILD SUCCESSFUL in 57s`**, 31 tasks executed. Debug Kotlin compiled (deprecation warnings only). |
| 8 | Parse `app/build/test-results/**.xml` | **7 suites, 83 tests, 0 failures, 0 errors, 0 skipped**: `AuthRoleRoutingTest` 3, `FarmerNavigationTest` 2, `CloudinaryUploadTest` 13, `HarvestListingRulesTest` 30, `UpdateListingUseCaseTest` 5, `OrderRequestFlowTest` 25, `MarketplaceEligibilityTest` 5. |
| 9 | `cd rules-tests && npm test` (no emulator) | **FAILED** — every test errored: *"The host and port of the firestore emulator must be specified."* (The `npm test` script does not start the emulator; the earlier "exit 0" was masked by the `\| tail` pipe.) |
| 10 | `firebase emulators:exec --only firestore --project demo-agroroute-rules "cd rules-tests && npm test"` | **FAILED to start** — *"Port 8081 is not open … Could not start Firestore Emulator, port taken."* (A stale process holds 8081; not killed.) |
| 11 | `firebase emulators:exec --only firestore --config firebase.rules-test.json --project demo-agroroute-rules "cd rules-tests && npm test"` | **`tests 22, pass 22, fail 0`** (emulator on port 8082, demo project, no login). Rules tests pass against the **current (stripped)** rules — note they **no longer include any future-harvest cases**. |
| 12 | `./gradlew assembleDebug --offline` | **`BUILD SUCCESSFUL in 4s`** → `app/build/outputs/apk/debug/app-debug.apk` (≈25 MB). |

**Commands NOT run (and why):**
- `firebase deploy` / any rules publish — **mutating remote state; not permitted.**
- Reading the **deployed** production rules — would require authenticated access to the live `agroroute` project; not attempted to avoid touching remote state. **Deployed rules remain UNVERIFIED.**
- Any **live** Firebase Auth/Firestore/Storage call or **real Cloudinary upload** — requires credentials/network and would mutate remote state; not permitted. All backend verdicts are code-level + local-emulator only.
- Killing the process on port 8081 — avoided (could be the user's work); used the alternate 8082 config instead.

---

## G. Prioritized Recommended Fixes (do NOT implement during this audit)

1. **[Critical] Resolve the future-harvest rules/app divergence** (Risk #1). Decide restore-rules vs remove-feature, then make `firestore.rules`, the Kotlin lifecycle code, and the emulator rules tests consistent. Until then, treat EXPECTED listings as non-functional.
2. **[Critical] Re-add emulator rules tests for the harvest lifecycle** (create EXPECTED, mark ready/failed, estimate update, pre-harvest request create/cancel, accept-blocked-while-EXPECTED) so this class of regression is caught automatically.
3. **[High] Add at least one integration test that drives the real repository transactions** (e.g. via the Firestore emulator) using the **app's actual write payloads**, so client writes are validated against rules rather than against fakes.
4. **[High] Confirm and align deployed rules** with the reconciled local rules; document the deploy step. Verify the `agroroute` project state before any production use.
5. **[Medium] Restrict fulfilment transitions by role** in both rules and UI (e.g. supplier advances IN_TRANSIT/DELIVERED; farmer may cancel), if that is the intended policy.
6. **[Medium] Handle pre-harvest requests on estimate change / mark-ready** — re-validate or auto-cancel/notify pending requests when the estimate drops or actual quantity is insufficient.
7. **[Medium] Protect Room data** — export schemas, add migration tests, and reconsider `fallbackToDestructiveMigration()` for production.
8. **[Medium] Reduce marketplace staleness** — add a Firestore snapshot listener (or pull-to-refresh sync) for supplier listings/stock.
9. **[Low] Wire or remove dead backend/UI** — password reset, supplier My-Bids/withdraw, Settings/Help items, mock notifications, unused `auth/WelcomeScreen.kt`, empty error-clear lambdas; add `PlaceOrderRequest` to `supplierRoutes`.
10. **[Low] Verify a real Cloudinary upload** on-device and confirm the unsigned preset scope; keep the API secret out of the client (currently correct).
11. **[Low] Repo hygiene** — git-ignore/untrack `firestore-debug.log`; correct README/`supplier_module_documentation.md` to match implemented scope (remove Driver role, "paid" status, cold-chain/GPS/cold-storage, `core/service/`, 5-tab supplier nav, My-Bids/Item-Details/profile-edit claims).

---

### Verification-status key (per the audit brief)
1. **Implemented in source** — yes for all rows marked ✅ in "Code".
2. **Reachable via UI** — yes except: future-harvest actions (unreachable because EXPECTED listings can't be created under current rules), supplier bid view/withdraw (no screen), password reset (no UI).
3. **Persisted locally/remotely** — Room + Firestore for all core entities; drafts are Room-only by design.
4. **Covered by tests** — 83 JVM fake-based unit tests + 22 emulator rules tests pass; **no** instrumented, migration, real-transaction, or real-upload tests.
5. **Configured in local project** — `google-services.json`, `firebase.json`, `firebase.rules-test.json`, `.firebaserc` (project `agroroute`), Cloudinary config present.
6. **Deployed to Firebase** — **UNVERIFIED** (not checked; would require remote access).
7. **Verified against a live service** — **NO** (no live Auth/Firestore/Storage/Cloudinary calls performed).

*End of audit. No app files, tests, Gradle files, Firebase/Cloudinary configuration, schemas, or security rules were modified by this audit; the only file created is `business-logic-audit/README.md`.*
