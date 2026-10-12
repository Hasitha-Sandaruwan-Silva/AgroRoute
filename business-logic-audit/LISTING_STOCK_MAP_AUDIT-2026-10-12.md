# Listing, stock and maps audit — 2026-10-12

## Inspection before implementation

Existing expiry is nullable `HarvestListing.expiresAt`, Unix milliseconds, persisted by Firestore and Room. Creation currently defaults to no expiry; editing preserves the value. No second expiry/status field is needed. Bid is currently a price-per-unit offer for all remaining stock; unlike OrderRequest it has no quantity. The requested partial-bid scenario requires extending this existing Bid with an optional requested quantity. Legacy absent/null quantity retains all-remaining-stock semantics. No parallel proposal/order architecture will be introduced.

## Map-flow matrix (recorded before code changes)

| Screen | Location source | Editable? | Storage | Map display | Current GPS? | Destination | Failure states |
|---|---|---|---|---|---|---|---|
| Farmer/Supplier profile | UserProfile.location | Own profile | Firestore + Room text | Shared picker | Permission-driven | Selected profile pin | Denial, disabled GPS, no fix, geocoder/map failure |
| Create/edit listing | HarvestListing.location; form/draft | Farmer; commercial history protected | Firestore + Room; draft Room only | Shared picker | Permission-driven | Farmer pickup | Legacy address without pin, failed save/upload |
| Place bid | Bid.deliveryLocation | Supplier before submit | Firestore + Room | Shared delivery field/picker | Permission-driven | Supplier delivery | Missing pickup/delivery, unavailable GPS/geocoder |
| Place request | OrderRequest.deliveryLocation | Supplier before submit | Firestore + Room | Shared delivery field/picker | Permission-driven | Supplier delivery | Missing pickup/delivery, unavailable GPS/geocoder |
| Marketplace | Listing pickup | No | Existing listing text + bitmap LRU | 120dp snapshot; serialized renderer, no card MapView | No | Farmer pickup | Missing coordinates, absent key, network/style timeout |
| Supplier order details, awaiting pickup | Order.pickupLocation snapshot | No pickup edit | Accepted order, Firestore + Room | Shared native MapLibre MapView | Optional | Farmer pickup | Legacy snapshot missing pin, GPS/map unavailable |
| Supplier order details, in transit | Order.deliveryLocation snapshot | Existing explicit supplier destination edit | Order, Firestore + Room | Shared native MapLibre MapView | Optional | Supplier delivery | Missing destination, denial/no fix/map failure |
| Farmer order details | Order pickup/delivery snapshots | No destination controls | Order, Firestore + Room | Saved delivery preview + both addresses | No | Saved delivery | Legacy coordinates absent, map unavailable |

Coordinate format is address followed by `[latitude, longitude]`; finite bounds are already checked. Accepted snapshots do not observe profile/listing edits. The existing explicit supplier delivery correction in AWAITING_PICKUP/IN_TRANSIT is a domain exception to literal immutability and will be preserved; profile edits never rewrite it.

Implementation and validation results will be recorded below after completion. Nothing will be deployed automatically.
