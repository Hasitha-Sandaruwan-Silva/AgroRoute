# Current business logic and UI audit — 11 October 2026

This matrix was derived before UI edits from current models, use cases, repositories,
Firestore rules, callable functions and navigation. It supersedes the historical audit
where that audit describes an earlier workflow. Backend transactions remain authoritative.

| State | Role and presentation | Allowed actions | Blocked actions / next state |
|---|---|---|---|
| Signed out / incomplete profile | Both: sign in, register, reset password; Google account completes role/profile | Existing authentication and profile completion | Role-gated routes until account is ready; persisted role cannot be changed by profile editing |
| Draft listing | Farmer: private draft, images and pickup location | Edit/publish/delete through existing listing flow | Supplier marketplace/request/bid entry |
| OPEN EXPECTED harvest | Farmer: estimate and expected date; Supplier: pre-harvest availability | Supplier request or bid according to listing mode; Farmer update estimate, mark ready/failed | Acceptance until READY; request quantity cannot exceed estimate |
| OPEN READY harvest | Both: actual remaining stock | Supplier request in fixed-price mode or bid in bidding mode; Farmer accept pending matching request/bid | Acceptance when stock is insufficient, listing missing/draft/closed/failed, wrong mode or missing required saved locations |
| CLOSED / FULFILLED / FAILED | Both: unavailable state | Existing owner edit/close operations where authorized; reject/withdraw pending submissions where allowed | New request/bid and acceptance; sold-out listings are fulfilled |
| Expiry metadata | Both: existing date presentation | Existing workflow | No expiry-based restriction: expiresAt is not enforced by current transaction/rules |
| PENDING request | Farmer: supplier, quantity, price, harvest readiness; Supplier: pending | Farmer accept eligible READY stock or reject; Supplier cancel own request | Request ACCEPTED creates order with request ID; REJECTED/CANCELLED terminal; no request-creation points or cancellation penalty |
| PENDING bid | Farmer: offer, supplier and listing readiness; Supplier: pending | Farmer accept eligible READY stock or reject; Supplier withdraw own pending bid | ACCEPTED creates order with bid ID; competing bids remain pending; no invented auto-rejection |
| ACCEPTED request/bid | Both: accepted and resulting order entry | Open existing role-specific order details | No repeated accept/reject/withdraw |
| AWAITING_PICKUP | Farmer: accepted order, supplier PIN entry; Supplier: authenticated six-digit code and saved Farmer pickup | Farmer verify valid code (+3 once); Supplier confirm collection (+4 once); either participant cancel (-2 actor only, floor zero); Supplier edit delivery destination | Farmer cannot move order to IN_TRANSIT/DELIVERED; cancellation requires confirmation; Supplier collection moves to IN_TRANSIT |
| Handover verified | Farmer: confirmed state, no PIN resubmission; Supplier: Farmer confirmed | Supplier collection if awaiting | Handover alone does not change order status; reward cannot be repeated |
| IN_TRANSIT | Farmer: tracking; Supplier: delivery destination and delivery confirmation | Supplier confirm delivery; Supplier edit saved delivery destination; Farmer may still verify valid enrolled code if not confirmed | Cancellation blocked; Supplier delivery moves to DELIVERED and awards +6 each once |
| DELIVERED | Both: final status and delivered reward policy, eligible counterpart review | Review once per participant; Farmer late handover verification remains allowed for a valid enrolled code | No collection/delivery/cancel action; historical orders do not receive retroactive rewards |
| CANCELLED | Both: cancelled state | View summary and saved locations | No verification, collection, delivery or review; atomic cancellation restores stock once |
| Pickup code loading/error/expiry | Supplier: loading, refresh and readable failure; Farmer: code input/error | Existing authenticated callable refresh; verify six digits | Five wrong attempts trigger existing 30-minute restriction; code expires after 24 hours; no direct verification-storage access |
| Legacy pickup order | Both: no enrolled verification/no new pickup reward | Existing backend-supported view/transition | No fake code or retroactive reward; completion receipts protect historical awards |
| Review checking / already reviewed | Both: eligibility loading or reviewed state | Delivered participant reviews other participant, rating 1–5 and nonblank comment | Duplicate review, nonparticipant, wrong counterpart and undelivered order |
| Locations | Farmer: order delivery; Supplier: pickup before collection and delivery during transport | Existing external maps navigation, GPS and authorized destination edit | No automatic profile-location overwrite; missing legacy coordinates show address fallback; no Directions API |
| Notifications | Both: current event text and role-specific destination | Existing request/bid/order notification routes | No second notification architecture; no separate handover event currently implemented |
| Points / leaderboard | Both: trusted users.points, role-specific leaderboard/rank | Read existing stored totals | No client awards: Farmer acceptance +1/handover +3/delivery +6; Supplier pickup +4/delivery +6; cancellation actor -2 floor 0 |

## UI gaps identified before editing

- Order details use oversized gradient summaries and vertical/nested progress cards;
  primary pickup actions occur after secondary maps/contact/payment information.
- Cancellation is a one-tap destructive action without the existing -2 consequence.
- Supplier transport details continue to navigate to pickup rather than delivery.
- Delivered review buttons do not check eligibility or outgoing reviews from another device.
- Pending request/bid acceptance controls do not represent all current readiness/stock/mode restrictions.
- Supplier pending bids appear twice and withdrawal does not reflect action loading.
- Accepted submissions do not consistently lead into their resulting order.
- Pickup copy lacks contextual +3/+4 policy and delivery +6 explanation; loading,
  expiry and confirmed states need a clearer hierarchy without claiming retroactive points.

## Preservation constraints

No change to reward amounts, atomic transactions, order transitions, stock, role permissions,
pickup security, notification routing or maps providers. Preserve LocationPickerDialog,
LocationPickerLayout, LocationMap, inset/IME handling, map instance preservation and
Marketplace previews. Use existing callbacks and role-specific order routes.

Validation and implementation findings will be appended after the changes are tested.

## Implementation completed

- Farmer and Supplier order details now use shared, compact themed summary/progress,
  current-action, cancellation and review sections. Primary verification/delivery actions
  precede secondary maps, contact, audit metadata and the payment disclaimer.
- Farmer has no transport/delivery control. Supplier collection still calls the existing
  IN_TRANSIT flow; Confirm Delivery still calls the existing DELIVERED flow. Cancellation
  is shown only while awaiting pickup and requires confirmation explaining actor-only
  two-point deduction, zero floor and stock release.
- Pickup retains authenticated callable state, six-digit validation, invalid/expired/rate-limit
  feedback, confirmed-state duplicate protection, legacy handling and valid late Farmer
  verification. Supplier collection is disabled for an expired code unless Farmer already
  confirmed. Reward text explains +3/+4 eligibility rather than awarding points locally.
- Summary and leaderboard copy explain acceptance +1, handover +3, pickup +4 and delivery
  +6; existing users.points and ranking queries are unchanged. Confirmed flags retain their
  existing historical reward wording, without pretending old orders received new amounts.
- Both order lists include human-readable next-step hints and agreed totals. Supplier cards
  have a smaller image; Farmer names now resolve for fixed-price request orders as well as
  bid orders. Counterparty name loads through the existing contact-profile read.
- Farmer request and both bid inbox presentations disable acceptance for current readiness,
  stock, mode, unavailable listing and required-location restrictions and show an explanation.
  Accepted requests/bids link to the existing order ID and role-specific details.
- Cancelled pre-harvest request copy no longer invents a Farmer cancellation reason. Harvest-confirmation waiting copy applies only while the listing remains EXPECTED.
- Supplier request/bid forms validate positive finite input and current listing eligibility
  before enabling submit. Pending bids appear once; withdrawal and order/request submissions
  use loading guards. Cancelling an unaccepted request remains distinct from scored order cancellation.
- Delivered review actions wait for a fresh order/participant/counterpart check and the existing
  review check. Already-reviewed state replaces the action; failed checks have retry. Submission
  is disabled while eligibility is unresolved and repeated taps are guarded.
- Supplier location presentation selects the order pickup snapshot before collection and the
  saved delivery snapshot during transport. Existing destination editing remains authorized
  before delivery. Map height, styling, native instance handling and external Navigate behavior
  are retained. LocationPickerDialog/Layout/Map and Marketplace previews are unchanged.
- NavHost consumes the shell's applied padding so child IME padding does not add the same
  system-bar inset twice. Provider diagnostics are mapped to readable presentation errors;
  meaningful business/PIN feedback is retained.
- Notifications already map requests, acceptance/rejection, transport, completion and cancellation
  to the correct role-specific destinations. Route/event unit coverage is retained. There is no
  separately implemented handover notification to expose; no notification policy was invented.
- Auth/role/profile completion, reset password, listing creation/edit/images/drafts, stock,
  leaderboard calculations, rating rules and the existing empty/error/retry flows were inspected
  and retained. Listing expiry metadata does not enforce expiry. Competing bids remain pending.

## Minimal data-read correction

`ReviewRepositoryImpl.hasUserReviewedOrder` previously checked only Room. The existing sync
loads incoming reviews (revieweeId), so an outgoing review created on another device was absent
and the UI offered a duplicate review. When no local review exists, the repository now reads
this authenticated user's outgoing reviews from the server, caches them, and checks orderId.
No rating calculation, review eligibility rule, write format or duplicate-security rule changed.
A focused regression test proves the remote outgoing review is recognized and cached.

## Source inspection

The current matrix follows model helpers in HarvestListing/OrderLocations/Enums; auth and
profile use cases; create/edit listing validation and repositories; request/bid placement,
accept/reject/withdraw use cases; OrderRepositoryImpl and OrderRequestRepositoryImpl; review
repository and Firestore rules; NotificationEvent/NotificationRoute/inbox presentation; and
trusted functions orderPickup/orderCompletion/orderScoring. The October 3 audit was not used
as authority where it contradicts the current implementation.

## Validation

- Initial complete Android unit run: 169 passed / 171 executed. Two existing
  HarvestReminderSchedulerTest cases fail because their fixed 2026-10-10 expected date is
  now in the past. No tests were removed or weakened and reminder logic was not changed.
- Focused regression run: 133 passed, covering order transitions, stock/request flows,
  callable pickup/points refresh, review constraints/eligibility/outgoing cache, locations,
  listing rules/images/edit and notification routes/events.
- Connected Pixel 7 / Android 17: first run 76 passed, including all 24 unchanged
  LocationPicker cases, 8 existing pickup cases and 44 new order-flow presentation cases.
- Complete connected run including Supplier pending-request fixture: 80 passed (48 new order-flow + 8 existing pickup + 24 unchanged LocationPicker cases). After extending its cancelled-state assertion, the final run passed 79/80; one loading/review fixture lost its Android host activity (no Compose hierarchy). Its entire unchanged 48-case class passed on retry (48/48). All 24 location cases passed in the final full run.
- Backend functions/rules were not touched; affected backend/rules tests are therefore not required.
- Fixtures render production components and exercise real callbacks without creating fake
  production orders. A live two-account Marketplace -> acceptance -> collection -> delivery
  -> review walkthrough remains to be performed with suitable real test accounts/orders.

## Files changed

- `app/src/androidTest/java/com/agroroute/OrderFlowPresentationTest.kt`
- `app/src/main/java/com/agroroute/core/components/ListingActionPresentation.kt`
- `app/src/main/java/com/agroroute/core/components/OrderFlowSections.kt`
- `app/src/main/java/com/agroroute/core/components/OrderLocationSection.kt`
- `app/src/main/java/com/agroroute/core/components/OrderPickupSection.kt`
- `app/src/main/java/com/agroroute/core/components/OrderTimeline.kt`
- `app/src/main/java/com/agroroute/core/components/ProfilePhoneContact.kt`
- `app/src/main/java/com/agroroute/core/components/SnackbarHost.kt`
- `app/src/main/java/com/agroroute/core/navigation/NavGraph.kt`
- `app/src/main/java/com/agroroute/core/repository/ReviewRepositoryImpl.kt`
- `app/src/main/java/com/agroroute/features/farmer/bids/BidsViewModel.kt`
- `app/src/main/java/com/agroroute/features/farmer/bids/FarmerBidsScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/bids/ViewBidsScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/listing/MyListingsScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/orderrequests/FarmerOrderRequestsScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/orderrequests/FarmerOrderRequestsViewModel.kt`
- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderDetailScreen.kt`
- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderViewModel.kt`
- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrdersScreen.kt`
- `app/src/main/java/com/agroroute/features/leaderboard/HomeLeaderboard.kt`
- `app/src/main/java/com/agroroute/features/leaderboard/LeaderboardScreen.kt`
- `app/src/main/java/com/agroroute/features/reviews/ReviewViewModel.kt`
- `app/src/main/java/com/agroroute/features/reviews/SubmitReviewScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/bids/SupplierBidsScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/bids/SupplierBidsViewModel.kt`
- `app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderDetailScreen.kt`
- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderViewModel.kt`
- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrdersScreen.kt`
- `app/src/test/java/com/agroroute/features/orders/ListingActionPresentationTest.kt`
- `app/src/test/java/com/agroroute/features/reviews/ReviewEligibilityTest.kt`
- `business-logic-audit/UI_FLOW_AUDIT-2026-10-11.md`

### Validation follow-up

The added cancelled-request assertion initially caught an unapplied text replacement
in all four theme/viewport variants. The production text was corrected directly; the
assertion remains unchanged. The final connected results above supersede that intermediate
failure. The final provider-error test also checks that meaningful legacy/PIN messages
are retained. Review submission now clears loading and disables submission if the account
signs out between eligibility checking and submission; a focused regression covers it.

Final standalone validation: debug APK build successful; 133 focused unit tests passed; 48/48 order-flow phone cases passed on retry. Together with the final full run, all 80 distinct presentation cases have passed, including all 24 unchanged location regressions.
