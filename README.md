# AgroRoute

**Smart Agro Cold-Chain & Harvest Pooling App** — Mobile Application Development (MAD) coursework project.

AgroRoute is a harvest-pooling platform for small-scale farmers. Farmers list harvest with photos and sell directly to buyers/suppliers through fixed pricing or an open bidding system. Several features mentioned in older descriptions (cold-storage booking, full GPS transport pooling, a Driver role, or a payment gateway) are not implemented in the current codebase — see "Status" below for details.

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose (Kotlin) |
| Architecture | MVVM + Repository + UseCase-style features |
| Local Database | Room (SQLite) — offline-first |
| Remote Backend | Firebase (Firestore, Authentication, Storage) |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |
| Hardware Access | Camera (photo capture), GPS/Location |

No custom backend server — Firebase serves as the complete remote data source (Firestore for data, Auth for login, Storage for images), satisfying the MAD syllabus's "Remote Data Source" and "Authentication" requirements.

---

## Architecture

```
UI (Composable Screen)
    ↓ observes state / calls functions
ViewModel (holds UI state, triggers actions)
    ↓ calls
UseCases / Feature logic (validation, coordination per action)
    ↓ calls
Repository Interface → Repository Implementation
    ↓ uses
Room (local cache) + Firestore/Firebase (remote source)
```

- **ViewModels** never call Firebase/Room directly — always through a Repository or UseCase.
- **UseCases** and small feature-coordination classes hold business logic per action (the codebase favors per-action UseCases over a single monolithic `Service` layer).
- **Repositories** are the single source of truth: read from Room (offline-first), write to Firestore then mirror to Room.

---

## Folder Structure

```
app/src/main/java/com/agroroute/
├── AgroRouteApp.kt          # Application class (@HiltAndroidApp)
├── MainActivity.kt          # Single activity, hosts the NavHost
│
├── core/                    # Shared across Farmer & Supplier
│   ├── database/            # Room: AppDatabase + DAOs (Bid, HarvestListing, Order, UserProfile)
│   ├── di/                  # Hilt modules (Database, Firebase, Repository wiring)
│   ├── model/                # Data classes: Bid, HarvestListing, Order, UserProfile, Enums
│   ├── navigation/           # NavGraph + Screen routes
│   ├── remote/                # Firebase SDK wrappers (Firestore, Auth, Storage)
│   ├── repository/            # Repository interfaces + implementations
│   ├── service/                # Business logic per feature (Auth, Listing, Bidding, Order)
│   ├── theme/                   # Colors, typography, app theme
│   ├── util/                     # Result wrapper, Constants, Extensions
│   └── widgets/                   # Reusable UI components (buttons, cards, fields, etc.)
│
└── features/                 # Role-based & screen-based UI + logic
    ├── auth/                  # Login, Register, Welcome
    ├── farmer/
    │   ├── bids/               # View & respond to supplier bids
    │   ├── home/               # Farmer dashboard
    │   ├── listing/            # Create/manage harvest listings, bidding toggle
    │   ├── orders/              # Farmer order tracking
    │   ├── profile/              # Farmer profile
    │   └── shell/                 # Bottom nav, scaffold, top bar
    ├── supplier/
    │   ├── bidding/             # Place bids on listings
    │   ├── home/                # Supplier dashboard
    │   ├── marketplace/          # Browse open listings
    │   ├── orders/                # Supplier order tracking
    │   ├── profile/                # Supplier profile
    │   └── shell/                   # Bottom nav, scaffold, top bar
    └── welcome/                 # App entry screen
```

---

## Core Features

- **Harvest Listing** — quantity, category (Organic/Inorganic), price, quality photo; price editable after listing.
- **Bidding System** — farmer can open a listing for bidding if it can't sell at the fixed price; suppliers submit offers with payment terms; farmer accepts/rejects.
-- **Order Tracking** — status flow from confirmed through to delivered (AWAITING_PICKUP, IN_TRANSIT, DELIVERED, CANCELLED). There is no `PAID` status and no built-in payment gateway; creating or delivering an order does not imply that a payment was processed.
-- **Role-based Auth** — Firebase Authentication with Farmer / Supplier roles (the Driver role referenced in older docs is not implemented).
- **Offline-first** — Room caches data locally; syncs with Firestore when connectivity resumes.

---

## Setup

1. Clone the project and open in Android Studio.
2. Add your `google-services.json` to the `app/` module (from Firebase Console).
3. Enable **Firestore Database**, **Authentication (Email/Password)**, and **Storage** in the Firebase Console.
4. Sync Gradle and run on an emulator or device (`minSdk` / `compileSdk` as configured in `app/build.gradle.kts`).

---

## Status

Actively in development — base architecture, Firebase integration, and core Farmer/Supplier screens are present. Known limitations and differences from older documentation:

- Driver role: NOT IMPLEMENTED. The app currently recognizes only `FARMER` and `SUPPLIER` roles in code.
- Payments: NOT IMPLEMENTED. There is no payment gateway integration and no `PAID` order status; orders do not record payment transactions.
- Cold-chain / GPS transport pooling / cold-storage booking: NOT IMPLEMENTED.
- Future Harvest (EXPECTED) flows: client-side code supports expected harvest listings and pre-harvest requests. The Firestore rules enforce per-request limits, and availability is enforced at acceptance time; see `firestore.rules` and `rules-tests` for details.
