# Spark lifecycle migration (design before implementation)

The deployed student project cannot run Functions. Android currently calls `orderPickup`, `completeOrder`, `cancelOrder`, and notification/token/inbox callables. Acceptance writes stock/order/proposal on Android; `rewardOrderAcceptance` is a backend trigger. Room mirrors acknowledged writes; the leaderboard reads `users.points`. Existing private Admin receipts must remain private. Functions source remains the stronger future backend, not the active Spark implementation.

| Current function | Responsibility | Spark replacement | Retained guarantee | Weaker guarantee |
|---|---|---|---|---|
| rewardOrderAcceptance | Farmer +1 after validated acceptance | Acceptance transaction: proposal, stock, order, deterministic event and constrained total | Atomic stock reservation; owner/READY/mode/terms; +1 once | Farmer declares available harvest stock; colluding accounts can manufacture business activity |
| orderPickup/view | Secret creation and expiry | Supplier SecureRandom PIN, salted PBKDF2 verifier, participant-only session, server-issued timestamp | No plaintext Firestore PIN; latest verifier only; Supplier-only rotation | Farmer can read verifier and attempt offline guesses; PIN entropy remains six digits |
| orderPickup/verify | Trusted PIN comparison and attempt limits; Farmer +3 | Farmer checks PBKDF2 locally; transaction records attempt or handover plus event/total | Farmer-only transition; immutable timestamp; +3 once; persisted attempt window cannot be reset by rotation/deletion | Rules cannot execute PBKDF2 or prove correct comparison. Modified Farmer can assert success or omit failed attempts. This is participant attestation, not server-proven possession |
| orderPickup/collect | Independent Supplier collection; +4 | Supplier transaction: session, order, event/total | Supplier-only AWAITING_PICKUP → IN_TRANSIT once; separate handover | Physical collection is a Supplier assertion |
| completeOrder | Delivered, +6 each and receipt | Supplier transaction: order, two immutable events, two constrained totals | IN_TRANSIT → DELIVERED once; exact amounts; reviews remain once | Physical delivery is a Supplier assertion |
| cancelOrder | Eligible cancellation, actor −2 floor zero, stock restoration | Participant transaction: order, listing, event/total | Only AWAITING_PICKUP; restore once; exact penalty | Legacy malformed/deleted listing cannot be safely invented/repaired |
| notification callables | Authorized in-app inbox plus FCM | Existing inbox collection, deterministic resource/state-bound notifications; direct owner reads/read acknowledgement | No unrelated recipient or arbitrary payload; event at most once | No background FCM delivery without trusted sender; notifications are best effort after lifecycle commit |

## Protocol and trust decisions

- New `spark_pickup_sessions/{orderId}` contains participant IDs, server `createdAt`/`issuedAt`, salt, PBKDF2-HMAC-SHA256 verifier (120,000 iterations), revision, attempt window/count, and independent confirmation timestamps. Expiry is **derived** from issuedAt +24h in Rules (avoids trusting a phone-supplied expiresAt). No PIN is in Firestore, Room, disk, or logs.
- Plaintext is Supplier process memory only, keyed by authenticated UID/order/revision. After process restart/second device, opening the order explicitly establishes a new code when necessary; refresh creates a new code and invalidates the old one. Rotation preserves attempt restrictions and confirmation flags.
- Security time comes from request.time/serverTimestamp. The device clock only affects presentation and friendly prechecks; Rules make the final decision. Transactions require server acknowledgement before Room/state/points success.
- Immutable `scoreEvents` and `users.lastScoreEventId` constrain each total change to one new exact event in the same atomic write. Existing totals remain the baseline; no historical backfill. Event rules require the corresponding **before → after** transition, not merely an already reached status.
- Stock writes carry an order pointer, checked against a newly created order or a newly cancelled order using getAfter. Ordinary Farmer quantity edits preserve sold stock and adjust available stock by the quantity difference. Harvest quantity itself remains Farmer-declared, as it was before.
- Old AWAITING_PICKUP may enroll if no existing Admin completed verification/receipt conflicts. No historical acceptance reward. Old IN_TRANSIT can be delivered without any new rewards; historical DELIVERED never gains retroactive events. Admin-only old secrets stay inaccessible.
- Existing Supplier delivery-location edit is an intentional authorized exception to snapshot immutability; commercial terms and pickup location stay immutable. Maps, navigation, listing cards, expiry/competing bids and UI insets remain outside this migration.

The PIN-comparison trust boundary is fundamentally impossible to retain on Spark with this flow. The safest available alternative here is explicit Farmer attestation with strict ownership and once-only bounded rewards. A trusted backend is required for server-enforced PIN correctness and all-attempt throttling. Tests must demonstrate and document this limitation rather than claim to block it.

## Implemented behavior

`RepositoryModule` binds `OrderLifecycle` to `SparkOrderLifecycle`. The old Android `OrderCompletionService` implements the same interface but is unbound. Normal lifecycle and inbox/token APIs call no Functions. `functions/src/**` is unchanged. Switching back requires coordinated Rules/receipt changes, not only changing the binding: never run the old acceptance trigger alongside Spark acceptance awards.

| Operation | Atomic writes / eligibility |
|---|---|
| Accept request / bid | Read fresh proposal/listing/profile and missing order; READY/OPEN/mode/locations/owner/stock; update proposal and reserve stock; create order with `sparkVersion:1`, Farmer acceptance event and exact +1 total. Bid consumes all remaining stock, as before. Cache acceptance points only after acknowledgement. |
| Supplier view / generate | Own AWAITING_PICKUP may enroll; enrolled unverified post-pickup sessions may rotate for late Farmer handover. Six-digit SecureRandom code, random 16-byte salt, PBKDF2-HMAC-SHA256 120,000 iterations/256-bit output. New code differs from previous digits. |
| Farmer verify | Transaction compares latest verifier in constant time. Incorrect submission commits its attempt counter before reporting failure. Correct submission sets Farmer confirmation and immutable timestamp plus exact +3 event/total. Order status stays unchanged. Duplicate verification is a read-only success. |
| Supplier collect | Own AWAITING_PICKUP, enrolled session, unexpired issuance or existing Farmer confirmation; separate confirmation/timestamp, IN_TRANSIT/pickedUpAt, exact +4 event/total. No Farmer-first prerequisite. Retry is read-only once already confirmed. |
| Supplier deliver | Own IN_TRANSIT → DELIVERED/deliveredAt with exact +6 events/totals for both participants in one transaction. Repeated DELIVERED completion awards nothing. Both existing review actions become eligible. |
| Either participant cancel | Only AWAITING_PICKUP; cancel actor/timestamp, restore original reserved stock, deterministic actor −2 event and floor-zero total in one transaction. Repeated CANCELLED cancellation changes nothing. IN_TRANSIT/DELIVERED cancellation fails. |

Room schema is unchanged. Sessions, salts, verifiers and events never enter Room. Existing leaderboard role queries/ranking stay unchanged; users.points remains the constrained aggregate, with historical values as its baseline. Other lifecycle profile refreshes mirror acknowledged totals. No offline optimistic order transition or reward is implemented; transaction failure stays an error.

Supplier UI shows code and expiry, explains that **Generate new code invalidates the previous code**, and keeps independent Confirm Pickup. Farmer retains manual six-digit entry and separate Confirm Handover. No verifier/salt is displayed or logged. Friendly error presentation now includes NOT_FOUND; existing permission/network/Firebase mappings remain. Maps, location picker, listing cards, navigation, expiry/competing bids and safe-area/IME behavior are preserved.

## Persisted data

| Collection / fields | Purpose |
|---|---|
| `orders` + sparkVersion / pickedUpAt / deliveredAt / cancelledAt / cancelledBy | Lifecycle eligibility and immutable server-time transition records; existing commercial snapshots unchanged |
| `spark_pickup_sessions` | orderId, farmerId, supplierId, immutable createdAt; issuedAt + revision; salt, verifier, iterations; attemptCount, attemptWindowAt, lastAttemptAt; independent farmerConfirmed/supplierConfirmed and immutable confirmation timestamps |
| `scoreEvents` | Deterministic ID; orderId, userId, role, eventType, exact pointsDelta and server createdAt; create only |
| `users` + lastScoreEventId | Existing points aggregate; exact event-linked delta/floor/cap; ordinary profile editing cannot change points/role/event marker |
| `listings` + stockOrderId | Atomic reservation/restoration pointer; ledger protected against standalone sold/available quantity rewrites |
| Existing `user_notifications/{recipient}/items` | Deterministic event/entity ID, entityId, eventType, actorId, recipientId, server createdAt, owner-only readAt; no client-supplied title/body/route |

**No new Spark Firestore write stores plaintext PIN.** The plaintext exists only in authenticated Supplier process memory. Process death or another device loses that copy; the next eligible Supplier view creates a fresh verifier and invalidates the previous digits. Same-process reopens reuse the current code. Explicit generation rotates it; counters and confirmations survive. Concurrent rotations retain only the latest committed revision. Old Admin-only verification records may contain plaintext from the previous implementation; this migration neither exposes nor deletes production data, and does not claim those records were purged.

Expiry is derived from issuedAt +24h in Rules rather than a phone-supplied expiresAt. Presentation exposes its derived milliseconds only to Supplier. Rules use request.time for expiry, 30-minute attempt-window rollover and all security timestamps. Local time provides countdown/friendly prechecks; a mis-set clock may cause conservative client errors but cannot authorize an otherwise forbidden write.

## Rules guarantees and remaining limitations

- Authentication, immutable profile roles/IDs, correct order participants and actor roles; unrelated users cannot access sessions or change orders/events/totals.
- New order requires its existing pending source, exact saved locations/product/unit/price/quantity, READY listing/mode, atomic accepted proposal, stock pointer/reservation and acceptance event. A batch cannot flip EXPECTED to READY and accept simultaneously.
- Commercial/order identities and pickup snapshots cannot be rewritten. The existing Supplier destination-edit action remains allowed only while AWAITING_PICKUP/IN_TRANSIT. Terminal destinations cannot be edited.
- Exact event ID/type/beneficiary/role/delta/time and corresponding before/after transition. No standalone, duplicate, updated or deleted event; total changes must be coupled to that new event. Delivery requires both rewards atomically. Cancellation floors zero.
- Supplier alone enrolls/rotates/collects; Farmer alone attests handover. Wrong-attempt increments cannot skip counts, reset early, backdate, delete/recreate, or be erased by Supplier rotation. Both confirmations/timestamps become immutable.
- Stock cannot be negative, reserved twice from stale state, restored twice, or silently reset. Farmer edits preserve sold stock and adjust available stock with total quantity; reserved listing units and deletion are blocked. A deleted/malformed legacy stock record cannot be invented for cancellation; safe administrative reconciliation is required.
- Reviews remain delivered-participant-only, correct counterparty, deterministic once-only ID, nonblank feedback, rating 1–5, no update/delete.

**Weaker than Functions:** Rules cannot prove a local PBKDF2 match, prevent offline guessing against the six-digit verifier, or require a modified Farmer to report every wrong attempt. A modified authorized Farmer can submit the exact handover/event/total attestation without possessing the PIN; an explicit emulator test demonstrates this. Persisted five-attempt restrictions are enforceable once recorded, but all-attempt throttling is not. Physical stock/handover/collection/delivery and noncollusion remain participant declarations. This is suitable only for the stated student/demo trust model; server-proven PIN possession requires trusted execution.

Notifications use the existing inbox and render content/routes from the persisted entity. Rules constrain actor, real counterparty, resource state, deterministic ID and allowed fields; arbitrary spam/text/recipients are blocked. Writes are best effort after lifecycle commit with a bounded wait. One notice per event/entity means repeated HARVEST_UPDATE for the same request is deduplicated. Historical inbox items remain readable, including serverTimestamp read acknowledgement. Existing process-alive realtime local notifications and harvest reminders remain; closed-app FCM delivery has no Spark trusted sender. The inbox uses one profile read plus up to one entity read per visible reference item (limit 50 default), appropriate for the small demo dataset.

## Legacy behavior

- Old AWAITING_PICKUP can enroll only with correct participants and no conflicting Admin confirmation/completion receipt. Enrollment adds no past acceptance reward. Future handover/pickup/delivery events can then be earned normally.
- Old AWAITING_PICKUP with existing completed Admin verification/receipt is blocked from conflicting enrollment. It needs safe administrative reconciliation; the client does not overwrite or reinterpret those private receipts.
- Old IN_TRANSIT can be completed without awarding retroactive delivery points; it cannot enroll a new verification. Existing DELIVERED/CANCELLED stays historical with no new scoring. Existing delivered reviews remain available under the unchanged eligibility rules.

## Files changed for this migration

This list excludes unrelated, already-uncommitted changes from the prior UI audit.

- Android remote: `OrderLifecycle.kt` (new), `SparkOrderLifecycle.kt` (new), `SparkScore.kt` (new), `PickupPin.kt` (new), `OrderCompletionService.kt` (future adapter).
- Repositories/DI: `OrderRepository.kt`, `OrderRepositoryImpl.kt`, `RepositoryModule.kt`.
- Notifications: `NotificationService.kt`, `SparkNotificationInbox.kt` (new), `NotificationInboxItem.kt`.
- Presentation/model: `OrderPickupSection.kt`, `SnackbarHost.kt`, `OrderPickupVerification.kt`, `SupplierOrderViewModel.kt`, `SupplierOrderDetailScreen.kt`.
- New unit tests: `core/remote/PickupPinTest.kt`, `core/remote/SparkOrderLifecycleTest.kt`, `core/notification/SparkNotificationInboxTest.kt`.
- Existing test fixtures/adapters: `OrderLocationFlowTest.kt`, `HarvestListingRulesTest.kt`, `OrderRequestFlowTest.kt`, `OrderPickupRepositoryTest.kt`, `PointAwardingTest.kt`; connected `OrderFlowPresentationTest.kt` new-code label assertion.
- Security/tests: `firestore.rules`, `rules-tests/firestore.rules.test.mjs`, `rules-tests/spark.lifecycle.test.mjs` (new), `rules-tests/package.json`, `functions/test/runEmulatorTests.cjs`.
- This report. No Functions implementation, Firebase initialization/project config, Room schema, map implementation or new dependency changed.

## Validation

| Check | Final result |
|---|---|
| Debug APK / Kotlin / Hilt | BUILD SUCCESSFUL; `app/build/outputs/apk/debug/app-debug.apk` |
| Focused Android unit regression | **151 passed**, zero failures/errors/skips; includes 12 real Kotlin Spark protocol transaction tests, four crypto/event tests and two inbox compatibility tests, plus existing lifecycle/stock/scoring/reviews suites |
| Firestore Rules | **150 passed** (98 existing +52 new); correct and malicious request/bid/stock/points/PIN-state/notification/review cases |
| Retained Functions emulator | **26 passed**, zero failures; stronger backend source untouched |
| Retained Functions TypeScript/unit | Build passed; **4 passed** |
| Connected Pixel 7, Android 17 | **80 passed**: 48 OrderFlow +8 pickup +24 LocationPicker, light/dark and compact/normal/layout/IME configurations |

The first overlapping Gradle invocation hit a Windows classes.jar lock; the final sequential run passed. Rules negative-write PERMISSION_DENIED logging is expected. Retained backend concurrency tests logged emulator lock retries but all passed. Real Kotlin tests use acknowledged transaction doubles, and authenticated JS emulator clients validate the actual Rules; these are not a claim of a completed production two-account test. The phone run validates presentation. The final full unfiltered unit suite was not rerun; the broader focused regression above is the validated scope. Earlier audit documented unrelated date-dependent harvest-reminder fixture failures.

Reproduce Android: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` with test filters for `core.remote.*`, `features.orders.*`, `features.orderrequests.*`, `features.points.*`, `core.repository.Order*`, `features.reviews.*`, `features.farmer.listing.*`, `core.notification.NotificationRouteTest`, `core.notification.NotificationEventTest`, `core.notification.SparkNotificationInboxTest`, `core.usecase.WithdrawBidUseCaseTest`.

Reproduce all backend/emulator checks: `npm.cmd --prefix functions test`, then `firebase.cmd emulators:exec --only firestore --config firebase.rules-test.json --project demo-agroroute-rules "node functions/test/runEmulatorTests.cjs"` with JAVA_HOME set to the installed Java and Node on PATH. Emulator tests require localhost and use only the demo project.

## Deployment and manual two-account test

**Nothing deployed.** Production will continue denying the new Spark writes until these reviewed Rules are deployed. Later, explicitly deploy **only** `firestore:rules` to `agroroute`: `firebase deploy --only firestore:rules --project agroroute`. Do not deploy Functions. No new composite index is needed: lifecycle uses document reads; inbox uses its ordinary single-field createdAt index; existing leaderboard queries remain unchanged. No role/points/data backfill is needed.

Live two-account testing remains pending that Rules deployment. Use consenting Farmer/Supplier test accounts and real, clearly identified demo listings; do not fabricate production receipts or points. Two devices are preferable; sequential sign-in works but switching devices/processes may rotate the Supplier PIN.

1. Record both initial totals (F0/S0) and listing available/sold stock. Farmer publishes an OPEN READY fixed-price listing with a bounded pickup map pin; Supplier creates a request with quantity within stock and a delivery pin. Request creation changes no points.
2. Farmer accepts once. Check request ACCEPTED, order AWAITING_PICKUP, exact reserved quantity, immutable commercial/location snapshots and Farmer F0+1. Retry acceptance: no second stock reservation or event.
3. Supplier opens that order. Observe six digits and expiry; Firestore session has no plaintext code. Generate a new code, check prior code is invalid. Restart Supplier app or open on a second device and check a new revision/code; Farmer does not automatically receive it.
4. Farmer opens the same order and manually submits one wrong code: readable error, persisted failed-attempt count, no points/status change. Submit the current correct code: Handover confirmed, Farmer F0+4, no PIN input, status still AWAITING_PICKUP. Repeat/reopen: no additional +3.
5. Supplier confirms pickup once: IN_TRANSIT, Supplier S0+4, immutable pickup timestamp; retry/reopen: no additional +4. On a separate order confirm Supplier pickup **before** Farmer verification to verify independence, then verify Farmer late without duplicating either reward.
6. Supplier confirms delivery: DELIVERED, Farmer F0+10 and Supplier S0+10 for the fully verified order, both exact +6 events and deliveredAt. Retry: unchanged totals. Farmer cannot confirm transport/delivery. Refresh leaderboard/profile to check the same persisted totals and role ranking.
7. Both accounts submit one review of the counterpart. Check eligible only after delivery and duplicate/update/delete rejected. Order destinations and commercial terms remain unchanged after listing/profile edits; existing authorized Supplier destination editing before delivery still works.
8. Repeat steps 1–7 on a READY bidding-enabled listing using a Supplier bid. Check the accepted bid consumes all remaining stock, correct offered price/saved pins, and the same ten-point successful lifecycle. Pending creation grants zero points.
9. Create separate AWAITING_PICKUP orders for Farmer and Supplier cancellation. Check only the cancelling actor loses two with floor zero; the other keeps its earned points. Stock restores exactly once; repeated cancellation does nothing. For Farmer cancellation after fresh acceptance, total is max(0,F0+1−2); for Supplier cancellation it is max(0,S0−2), with Farmer retaining F0+1. IN_TRANSIT/DELIVERED cancellation fails. Sell-out reopening retains the existing releaseSale behavior.
10. Check persisted five wrong attempts reject further verification for 30 minutes; rotation does not reset that window. Emulator tests cover expiry/server timestamp forgery. Disable network before a lifecycle action: show connectivity failure/retry, no queued optimistic reward. Reconnect and retry once.
11. Verify inbox counterpart notifications, safe routes and owner-only read acknowledgement. No guarantee of closed-app FCM push. Review the explicit modified-client PIN limitation separately; UI-disabled controls are not security guarantees.
