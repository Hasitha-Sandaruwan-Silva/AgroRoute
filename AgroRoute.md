# AgroRoute

**Smart Agro Cold-Chain & Harvest Pooling App** — Mobile Application Development (MAD) coursework project.

AgroRoute is a harvest-pooling and cold-chain visibility platform for small-scale farmers: farmers list surplus harvest with quality photos, pool transport with nearby farmers via GPS matching, book cold-storage slots, and sell directly to buyers/suppliers through fixed pricing or an open bidding system.

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| UI | Jetpack Compose (Kotlin) |
| Architecture | MVVM + Repository + Service layer |
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
Service (business logic per feature — validation, rules, coordination)
    ↓ calls
Repository Interface → Repository Implementation
    ↓ uses
Room (local cache) + Firestore/Firebase (remote source)
```

- **ViewModels** never call Firebase/Room directly — always through a Repository or Service.

- **Services** hold business logic per feature (e.g. `ListingService`, `BiddingService`), replacing a one-class-per-action UseCase pattern.

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

- **Order Tracking** — status flow from confirmed through to paid/delivered, separately for Farmer and Supplier views.

- **Role-based Auth** — Firebase Authentication with Farmer / Supplier / Driver roles.

- **Offline-first** — Room caches data locally; syncs with Firestore when connectivity resumes.

---

## Setup

1. Clone the project and open in Android Studio.

1. Add your `google-services.json` to the `app/` module (from Firebase Console).

1. Enable **Firestore Database**, **Authentication (Email/Password)**, and **Storage** in the Firebase Console.

1. Sync Gradle and run on an emulator or device (`minSdk` / `compileSdk` as configured in `app/build.gradle.kts`).

---

## Status

Actively in development — base architecture, Firebase integration, and core Farmer/Supplier screens in progress.

---

## AgroRoute Source Guide

This guide is based on the source files currently available. No clear implementation for GPS-based transport pooling, cold-storage booking, or the Driver flow mentioned in the project overview above was found in the source tree; this does not confirm that those features are implemented. Although an order and a price are currently displayed, the app does not process payments.

### Main Folders

- `app/src/main/java/com/agroroute/core/` — Models, Room, Firebase, repositories, navigation, theme, and widgets shared by both roles.

- `app/src/main/java/com/agroroute/features/auth/` — Sign-in, registration, and account-role selection.

- `app/src/main/java/com/agroroute/features/farmer/` — Farmer dashboard, listings, bids, order requests, orders, profile, and navigation.

- `app/src/main/java/com/agroroute/features/supplier/` — Supplier dashboard, marketplace, bids, order requests, orders, profile, and navigation.

- `app/src/main/java/com/agroroute/features/leaderboard/` and `app/src/main/java/com/agroroute/features/reviews/` — Separate leaderboards based on points and order reviews.

- `app/src/main/res/` — Android manifest, name/theme resources, app icons, and images.

- `app/src/test/` — Kotlin/JVM unit tests.

- `rules-tests/` — Node.js emulator tests for Firestore rules.

- `gradle/` — Gradle wrapper, version catalog, and build runtime settings.

### Farmer and Supplier Workflow

1. From `WelcomeScreen`, the user goes to `LoginScreen` or `RegisterScreen`. After signing in with email/password or Google, the Farmer or Supplier home screen opens according to the saved role.

1. A Farmer can save a listing as a draft on the device or publish it with the harvest, quantity, price, category, and (optionally) a photo. If bidding is enabled for the listing, a Supplier can place a bid; otherwise, the Supplier can submit a fixed-price order request. For an expected harvest, the request is only an estimate.

1. The Farmer reviews incoming bids or order requests and accepts or rejects them. When one is accepted, an order is created and the listing stock is updated. The Supplier can view their bids and requests while waiting for a response to the order request.

1. The order progresses through `AWAITING_PICKUP` → `IN_TRANSIT` → `DELIVERED`; it can be changed to `CANCELLED` in permitted cases. When the Supplier confirms an order as delivered, both the Farmer and Supplier receive one point each, once per order.

1. For a `DELIVERED` order, the Farmer can leave the Supplier a 1–5-star rating and written review, and the Supplier can do the same for the Farmer. Reviews do not change points. `FarmerLeaderboard` and `SupplierLeaderboard` display points separately by role.

### Build, Firebase, and Documentation Files

- `build.gradle.kts` — Configures project-wide Gradle plugin aliases.

- `settings.gradle.kts` — Configures Gradle repositories and the `:app` module.

- `gradle.properties` — Configures Gradle, AndroidX, and Kotlin build flags.

- `gradle/gradle-daemon-jvm.properties` — Selects the JDK toolchain for the Gradle daemon.

- `gradle/libs.versions.toml` — Centralizes plugin and library versions.

- `gradle/wrapper/gradle-wrapper.properties` — Specifies the Gradle distribution in use.

- `gradle/wrapper/gradle-wrapper.jar` — The binary that starts the Gradle Wrapper.

- `gradlew` and `gradlew.bat` — Gradle Wrapper scripts for macOS/Linux and Windows.

- `app/build.gradle.kts` — Configures Android app settings, dependencies, Compose, and test options.

- `local.properties` — Stores the Android SDK location on this computer; its value is not shown here.

- `.firebaserc` — Stores the Firebase CLI project mapping; the project value is not shown here.

- `app/google-services.json` — Firebase client configuration for the Android app; configuration values are not included here.

- `firebase.json` — Connects Firebase rules files and emulator configuration.

- `firebase.rules-test.json` — Provides a separate emulator configuration for Firestore rules tests.

- `firestore.rules` — Restricts roles, ownership, and write permissions for Firestore collections.

- `storage.rules` — Restricts permissions for Firebase Storage listing-image paths.

- `rules-tests/package.json` — Configures the Firestore rules test command and Node dependencies.

- `rules-tests/package-lock.json` — Records the exact versions of the rules-test dependencies.

- `.gitignore` — Excludes items such as build outputs, local configuration, and logs from Git.

- `README.md` — Contains the project overview and this code guide.

- `LOGICS.md` — A Sinhala-language audit document about the app logic.

- `supplier_module_documentation.md` — A functional specification written for the Supplier module; it may differ from the source implementation.

- `business-logic-audit/README.md` — A dated audit report; its findings should be rechecked against the current source.

### Android Entry Point and Resources

- `app/src/main/AndroidManifest.xml` — Registers the application, launcher activity, permissions, and harvest reminder receiver.

- `app/src/main/java/com/agroroute/AgroRouteApp.kt` — The Application entry point for Hilt dependency injection.

- `app/src/main/java/com/agroroute/MainActivity.kt` — Starts the splash screen, theme, and Compose navigation host.

- `app/src/main/res/values/colors.xml` — Defines Android resource colors.

- `app/src/main/res/values/strings.xml` — Provides the app name and order/payment caption strings.

- `app/src/main/res/values/themes.xml` — Configures Android window and splash themes.

- `app/src/main/res/drawable/agroroute.png` — An app drawable image asset; its use is not clear from the Kotlin source.

- `app/src/main/res/drawable/agroroute1.png` — The image used for the welcome header and animated logo.

- `app/src/main/res/drawable/logo.png` — The logo image displayed on the login screen and app header.

- `app/src/main/res/drawable/ic_launcher_foreground.xml` — The launcher icon foreground drawable.

- `app/src/main/res/drawable/splash_icon.xml` — The splash screen icon drawable.

- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — The adaptive launcher icon.

- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — The round adaptive launcher icon.

### `core/database` and `core/model`

- `app/src/main/java/com/agroroute/core/database/AppDatabase.kt` — Brings together Room entities and DAOs.

- `app/src/main/java/com/agroroute/core/database/BidDao.kt` — Provides queries to read, save, and delete bids.

- `app/src/main/java/com/agroroute/core/database/Converters.kt` — Converts enums and string lists into SQLite values.

- `app/src/main/java/com/agroroute/core/database/DatabaseMigrations.kt` — Configures migrations between Room database versions.

- `app/src/main/java/com/agroroute/core/database/HarvestListingDao.kt` — Provides local queries and writes for harvest listings.

- `app/src/main/java/com/agroroute/core/database/OrderCompletionRecordDao.kt` — Stores order-completion and point-award records.

- `app/src/main/java/com/agroroute/core/database/OrderDao.kt` — Provides local queries and writes for orders.

- `app/src/main/java/com/agroroute/core/database/OrderRequestDao.kt` — Provides local queries and writes for order requests.

- `app/src/main/java/com/agroroute/core/database/ReviewDao.kt` — Provides queries to read and save reviews.

- `app/src/main/java/com/agroroute/core/database/UserProfileDao.kt` — Reads profiles and points rankings by role.

- `app/src/main/java/com/agroroute/core/model/Bid.kt` — Holds the fields and status of a Supplier offer.

- `app/src/main/java/com/agroroute/core/model/Enums.kt` — Defines user roles and listing, harvest, bid, order, and quantity statuses.

- `app/src/main/java/com/agroroute/core/model/HarvestListing.kt` — Holds listing fields and domain functions for changing stock, harvest, and bidding.

- `app/src/main/java/com/agroroute/core/model/Order.kt` — Order data created from an accepted offer or request.

- `app/src/main/java/com/agroroute/core/model/OrderCompletionRecord.kt` — Records an order so that its points are not awarded more than once.

- `app/src/main/java/com/agroroute/core/model/OrderRequest.kt` — Data for a Supplier fixed-price order request.

- `app/src/main/java/com/agroroute/core/model/Review.kt` — Holds the rating and comment for an order participant review.

- `app/src/main/java/com/agroroute/core/model/UserProfile.kt` — Farmer/Supplier account and points data.

### `core/di`, `core/navigation`, `core/notification`, and `core/remote`

- `app/src/main/java/com/agroroute/core/di/DatabaseModule.kt` — Provides the Room database and DAOs through Hilt.

- `app/src/main/java/com/agroroute/core/di/FirebaseModule.kt` — Provides Firebase Auth, Firestore, and Storage instances.

- `app/src/main/java/com/agroroute/core/di/RepositoryModule.kt` — Binds repository interfaces to their implementations.

- `app/src/main/java/com/agroroute/core/di/ThemeModule.kt` — Binds the theme-preference implementation.

- `app/src/main/java/com/agroroute/core/navigation/NavGraph.kt` — Connects screens and restricts routes according to the logged-in role.

- `app/src/main/java/com/agroroute/core/navigation/Screen.kt` — Defines named navigation routes and route arguments.

- `app/src/main/java/com/agroroute/core/notification/HarvestReminderReceiver.kt` — Displays a harvest reminder notification on the scheduled date.

- `app/src/main/java/com/agroroute/core/notification/HarvestReminderScheduler.kt` — Schedules or cancels an Android alarm for the expected harvest date.

- `app/src/main/java/com/agroroute/core/remote/AuthService.kt` — Provides Firebase email/password, Google sign-in, and session actions.

- `app/src/main/java/com/agroroute/core/remote/CloudinaryPhotoStorage.kt` — Validates a listing photo, uploads it to Cloudinary, and returns its URL.

- `app/src/main/java/com/agroroute/core/remote/FirestoreService.kt` — A wrapper for Firestore reads, writes, flows, and transactions.

- `app/src/main/java/com/agroroute/core/remote/NetworkConnectivityMonitor.kt` — Monitors network connectivity as a Flow.

- `app/src/main/java/com/agroroute/core/remote/StorageService.kt` — Provides Firebase Storage image upload and download-URL operations.

### `core/repository` and `core/usecase`

- `app/src/main/java/com/agroroute/core/repository/AuthRepository.kt` — The contract for authentication and profile operations.

- `app/src/main/java/com/agroroute/core/repository/AuthRepositoryImpl.kt` — Connects Firebase Auth, Firestore profiles, and the Room cache.

- `app/src/main/java/com/agroroute/core/repository/BidRepository.kt` — The contract for bid operations.

- `app/src/main/java/com/agroroute/core/repository/BidRepositoryImpl.kt` — Implements bid queries, writes, status updates, and synchronization.

- `app/src/main/java/com/agroroute/core/repository/HarvestListingRepository.kt` — The contract for listing and harvest-status operations.

- `app/src/main/java/com/agroroute/core/repository/HarvestListingRepositoryImpl.kt` — Manages listing writes to Firestore/Room, ownership checks, stock, and harvest updates.

- `app/src/main/java/com/agroroute/core/repository/LeaderboardRepository.kt` — The contract for role-based leaderboard and ranking operations.

- `app/src/main/java/com/agroroute/core/repository/LeaderboardRepositoryImpl.kt` — Provides separate points rankings for Farmers and Suppliers.

- `app/src/main/java/com/agroroute/core/repository/ListingPhotoStorage.kt` — Provides the photo-storage contract, errors, and storage-path helper.

- `app/src/main/java/com/agroroute/core/repository/OrderRepository.kt` — The contract for orders, acceptance, and status changes.

- `app/src/main/java/com/agroroute/core/repository/OrderRepositoryImpl.kt` — Manages bid/request acceptance transactions, stock, order status, and once-only points awards.

- `app/src/main/java/com/agroroute/core/repository/OrderRequestRepository.kt` — The contract for order-request operations.

- `app/src/main/java/com/agroroute/core/repository/OrderRequestRepositoryImpl.kt` — Implements Supplier request writes, Farmer responses, and synchronization.

- `app/src/main/java/com/agroroute/core/repository/ReviewRepository.kt` — The contract for review queries and submissions.

- `app/src/main/java/com/agroroute/core/repository/ReviewRepositoryImpl.kt` — Saves reviews after checking order completion, participants, rating, and duplicate reviews.

- `app/src/main/java/com/agroroute/core/usecase/GetBidsBySupplierUseCase.kt` — Delegates retrieval of Supplier bids to the repository.

- `app/src/main/java/com/agroroute/core/usecase/GetListingsUseCase.kt` — Provides actions for reading and synchronizing listings.

- `app/src/main/java/com/agroroute/core/usecase/GetOrdersUseCase.kt` — Provides Farmer/Supplier order queries and a single-order query.

- `app/src/main/java/com/agroroute/core/usecase/WithdrawBidUseCase.kt` — Delegates Supplier bid withdrawal to the repository.

### `core/theme`, `core/util`, and `core/widgets`

- `app/src/main/java/com/agroroute/core/theme/Color.kt` — Defines light- and dark-theme colors.

- `app/src/main/java/com/agroroute/core/theme/ExtendedColors.kt` — Provides additional light- and dark-theme colors for screens.

- `app/src/main/java/com/agroroute/core/theme/Theme.kt` — Configures the Material theme, status-bar colors, and shapes.

- `app/src/main/java/com/agroroute/core/theme/ThemePreference.kt` — Stores the system/light/dark selection on the device.

- `app/src/main/java/com/agroroute/core/theme/ThemeViewModel.kt` — Exposes theme state to the UI.

- `app/src/main/java/com/agroroute/core/theme/Type.kt` — Configures Compose typography styles.

- `app/src/main/java/com/agroroute/core/util/CloudinaryConfig.kt` — Stores Cloudinary upload endpoint settings and photo limits; it must not contain a secret.

- `app/src/main/java/com/agroroute/core/util/Constants.kt` — Stores Firestore collection, Room database, and navigation key names.

- `app/src/main/java/com/agroroute/core/util/Extensions.kt` — Provides date, currency, string, and `Result` helpers.

- `app/src/main/java/com/agroroute/core/util/HarvestAvailabilityDate.kt` — Formats a harvest date for display.

- `app/src/main/java/com/agroroute/core/util/Result.kt` — Defines Loading, Success, and Error result states.

- `app/src/main/java/com/agroroute/core/widgets/AgroTextField.kt` — A shared text field used in the app.

- `app/src/main/java/com/agroroute/core/widgets/AnimatedBottomBar.kt` — An animated bottom-navigation composable.

- `app/src/main/java/com/agroroute/core/widgets/AppCard.kt` — Clickable and non-clickable card composables.

- `app/src/main/java/com/agroroute/core/widgets/EmptyState.kt` — A message view for empty lists.

- `app/src/main/java/com/agroroute/core/widgets/GradientHeader.kt` — A gradient title header.

- `app/src/main/java/com/agroroute/core/widgets/ListingImage.kt` — Displays a listing photo, loading state, and fallback icon.

- `app/src/main/java/com/agroroute/core/widgets/LoadingButton.kt` — An action button with a loading state.

- `app/src/main/java/com/agroroute/core/widgets/OrderTimeline.kt` — Displays the order-status timeline and progress strip.

- `app/src/main/java/com/agroroute/core/widgets/PhotoPicker.kt` — Selects an image using the system photo picker.

- `app/src/main/java/com/agroroute/core/widgets/PriceInputField.kt` — Restricts decimal price input.

- `app/src/main/java/com/agroroute/core/widgets/RoleBadge.kt` — Displays a Farmer/Supplier role badge.

- `app/src/main/java/com/agroroute/core/widgets/RoleSelectCard.kt` — A card for selecting an account role.

- `app/src/main/java/com/agroroute/core/widgets/SectionTitle.kt` — Displays a section title and an optional action.

- `app/src/main/java/com/agroroute/core/widgets/SettingsSection.kt` — Displays theme and profile settings actions.

- `app/src/main/java/com/agroroute/core/widgets/Shimmer.kt` — Provides loading-skeleton composables.

- `app/src/main/java/com/agroroute/core/widgets/SnackbarHost.kt` — Displays snackbar messages and error effects.

- `app/src/main/java/com/agroroute/core/widgets/StatCard.kt` — Displays a dashboard count/value card.

- `app/src/main/java/com/agroroute/core/widgets/StatusChip.kt` — Provides chips for listing, bid, order, and harvest statuses.

- `app/src/main/java/com/agroroute/core/widgets/WidgetPreviews.kt` — Contains Compose previews for shared widgets.

- `app/src/main/java/com/github/thelacspace/icons/Lucide.kt` — Maps icon names used in the code to Material icons.

### `features/auth`, `features/welcome`, and `features/settings`

- `app/src/main/java/com/agroroute/features/auth/AuthViewModel.kt` — Manages login, registration, Google sign-in, and session UI state.

- `app/src/main/java/com/agroroute/features/auth/GoogleRoleSelectionScreen.kt` — Lets a new Google account choose the Farmer/Supplier role.

- `app/src/main/java/com/agroroute/features/auth/LoginScreen.kt` — Displays the email/password and Google sign-in UI.

- `app/src/main/java/com/agroroute/features/auth/LoginUseCase.kt` — Validates login input and calls the repository.

- `app/src/main/java/com/agroroute/features/auth/RegisterScreen.kt` — Displays account creation and role-selection UI.

- `app/src/main/java/com/agroroute/features/auth/RegisterUseCase.kt` — Validates registration input and calls the repository.

- `app/src/main/java/com/agroroute/features/auth/SendPasswordResetEmailUseCase.kt` — Validates the email address and sends a reset email.

- `app/src/main/java/com/agroroute/features/welcome/AnimatedLogoHeader.kt` — Displays an animated logo and app-title header.

- `app/src/main/java/com/agroroute/features/welcome/WelcomeScreen.kt` — Displays onboarding pages and navigation to registration/login.

- `app/src/main/java/com/agroroute/features/settings/SettingsScreens.kt` — Displays role-based Help, About, and Privacy screens.

### `features/farmer`

- `app/src/main/java/com/agroroute/features/farmer/bids/AcceptBidUseCase.kt` — Validates Farmer acceptance/rejection of bids.

- `app/src/main/java/com/agroroute/features/farmer/bids/BidsViewModel.kt` — Manages Farmer bid lists, synchronization, and bid actions.

- `app/src/main/java/com/agroroute/features/farmer/bids/FarmerBidsScreen.kt` — Displays filtered incoming bids and allows acceptance/rejection.

- `app/src/main/java/com/agroroute/features/farmer/bids/ViewBidsScreen.kt` — Displays bids for a listing and provides accept/reject actions.

- `app/src/main/java/com/agroroute/features/farmer/calendar/HarvestCalendarScreen.kt` — Lists upcoming harvest dates and schedules reminders.

- `app/src/main/java/com/agroroute/features/farmer/home/FarmerHomeScreen.kt` — Displays listing/order totals and Farmer shortcuts.

- `app/src/main/java/com/agroroute/features/farmer/listing/CloseListingUseCase.kt` — Checks whether an open listing can be closed.

- `app/src/main/java/com/agroroute/features/farmer/listing/CreateListingScreen.kt` — UI for creating and editing listings, drafts, and harvest details.

- `app/src/main/java/com/agroroute/features/farmer/listing/CreateListingUseCase.kt` — Validates listing fields and publishes after photo upload.

- `app/src/main/java/com/agroroute/features/farmer/listing/ListingViewModel.kt` — Loads Farmer listings/drafts and delegates actions to repositories/use cases.

- `app/src/main/java/com/agroroute/features/farmer/listing/MyListingsScreen.kt` — Searches, sorts, filters, and manages Farmer listings.

- `app/src/main/java/com/agroroute/features/farmer/listing/ScheduleHarvestReminderUseCase.kt` — Schedules a reminder for a listing.

- `app/src/main/java/com/agroroute/features/farmer/listing/ToggleBiddingModeUseCase.kt` — Validates enabling/disabling bidding for a listing.

- `app/src/main/java/com/agroroute/features/farmer/listing/UpdateListingUseCase.kt` — Validates listing edits, checks pending bids/orders, and handles photo uploads.

- `app/src/main/java/com/agroroute/features/farmer/orderrequests/FarmerOrderRequestsScreen.kt` — Filters incoming direct requests and allows acceptance/rejection.

- `app/src/main/java/com/agroroute/features/farmer/orderrequests/FarmerOrderRequestsViewModel.kt` — Loads Farmer requests and listing data and coordinates actions.

- `app/src/main/java/com/agroroute/features/farmer/orderrequests/OrderRequestUseCases.kt` — Performs Farmer-side checks for accepting/rejecting direct requests.

- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderDetailScreen.kt` — Displays order details, the status timeline, and the action to review the Supplier.

- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderViewModel.kt` — Loads Farmer orders and handles status actions.

- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrdersScreen.kt` — Filters Farmer order history and displays a summary.

- `app/src/main/java/com/agroroute/features/farmer/profile/FarmerProfileScreen.kt` — Displays the Farmer profile, points, and a link to the Farmer leaderboard.

- `app/src/main/java/com/agroroute/features/farmer/shell/FarmerBottomBar.kt` — Provides Farmer tab navigation.

- `app/src/main/java/com/agroroute/features/farmer/shell/FarmerScaffold.kt` — Combines Farmer top/bottom bars with screen content.

- `app/src/main/java/com/agroroute/features/farmer/shell/FarmerTopBar.kt` — Displays the Farmer route title, profile menu, and sign-out controls.

### `features/supplier`, `features/leaderboard`, and `features/reviews`

- `app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidScreen.kt` — Creates and submits a Supplier offer for a listing.

- `app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidUseCase.kt` — Checks the Supplier role, offer, and listing eligibility.

- `app/src/main/java/com/agroroute/features/supplier/bids/SupplierBidsScreen.kt` — Displays the Supplier’s bids and the option to withdraw pending bids.

- `app/src/main/java/com/agroroute/features/supplier/bids/SupplierBidsViewModel.kt` — Loads Supplier bid history and handles bid withdrawal.

- `app/src/main/java/com/agroroute/features/supplier/home/SupplierHomeScreen.kt` — Displays the Supplier dashboard for the marketplace, bids, and orders.

- `app/src/main/java/com/agroroute/features/supplier/marketplace/BrowseListingsScreen.kt` — Browses eligible listings and selects the bid or fixed-price request flow.

- `app/src/main/java/com/agroroute/features/supplier/marketplace/MarketplaceViewModel.kt` — Synchronizes listings and provides marketplace state.

- `app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestScreen.kt` — Submits a fixed-price or pre-harvest request quantity.

- `app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestUseCase.kt` — Validates the Supplier request, quantity, and listing eligibility.

- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderDetailScreen.kt` — Displays Supplier order details, delivery confirmation, and the action to review the Farmer.

- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderViewModel.kt` — Loads Supplier orders/requests and handles status/cancellation actions.

- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrdersScreen.kt` — Displays the Supplier’s requests and orders.

- `app/src/main/java/com/agroroute/features/supplier/profile/SupplierProfileScreen.kt` — Displays the Supplier profile, points, and a link to the Supplier leaderboard.

- `app/src/main/java/com/agroroute/features/supplier/shell/SupplierBottomBar.kt` — Provides Supplier tab navigation.

- `app/src/main/java/com/agroroute/features/supplier/shell/SupplierScaffold.kt` — Combines Supplier top/bottom bars with screen content.

- `app/src/main/java/com/agroroute/features/supplier/shell/SupplierTopBar.kt` — Provides Supplier route titles, profile menu, and route lists.

- `app/src/main/java/com/agroroute/features/leaderboard/LeaderboardScreen.kt` — Displays the leaderboard and review score according to role.

- `app/src/main/java/com/agroroute/features/leaderboard/LeaderboardViewModel.kt` — Loads role-specific points rankings and review metrics.

- `app/src/main/java/com/agroroute/features/reviews/ReviewViewModel.kt` — Manages the state for submitting an order-participant review.

- `app/src/main/java/com/agroroute/features/reviews/SubmitReviewScreen.kt` — Collects a 1–5-star rating and written review.

### Unit Tests and Firestore Rules Tests

- `app/src/test/java/com/agroroute/core/navigation/AuthRoleRoutingTest.kt` — Tests the home route based on the saved role and Google sign-in outcome.

- `app/src/test/java/com/agroroute/core/theme/ThemePreferenceTest.kt` — Tests loading and persisting the theme mode.

- `app/src/test/java/com/agroroute/core/usecase/GetBidsBySupplierUseCaseTest.kt` — Tests Supplier bid-query forwarding and error results.

- `app/src/test/java/com/agroroute/core/usecase/WithdrawBidUseCaseTest.kt` — Tests bid-withdrawal delegation and error propagation.

- `app/src/test/java/com/agroroute/features/auth/SendPasswordResetEmailUseCaseTest.kt` — Tests email validation and password-reset requests.

- `app/src/test/java/com/agroroute/features/farmer/home/FarmerNavigationTest.kt` — Tests Farmer home shortcuts and role routes.

- `app/src/test/java/com/agroroute/features/farmer/listing/CloudinaryUploadTest.kt` — Tests photo-upload success/failure and handling of the returned URL.

- `app/src/test/java/com/agroroute/features/farmer/listing/HarvestListingRulesTest.kt` — Contains unit checks for listing, stock, bid, harvest, and photo rules.

- `app/src/test/java/com/agroroute/features/farmer/listing/UpdateListingUseCaseTest.kt` — Tests edit ownership, pending bids/orders, and photo replacement.

- `app/src/test/java/com/agroroute/features/orderrequests/OrderRequestFlowTest.kt` — Tests request placement, acceptance, stock, harvest, and ownership flows.

- `app/src/test/java/com/agroroute/features/orders/OrderStatusTransitionTest.kt` — Tests Farmer/Supplier order-status transition rules.

- `app/src/test/java/com/agroroute/features/points/PointAwardingTest.kt` — Tests order-completion point awards and duplicate protection.

- `app/src/test/java/com/agroroute/features/reviews/ReviewConstraintsTest.kt` — Tests review participant, completion, uniqueness, and rating constraints.

- `app/src/test/java/com/agroroute/features/supplier/home/SupplierNavigationTest.kt` — Tests Supplier routes and shared settings navigation.

- `app/src/test/java/com/agroroute/features/supplier/marketplace/MarketplaceEligibilityTest.kt` — Tests marketplace listing eligibility and bid actions.

- `rules-tests/firestore.rules.test.mjs` — Uses the emulator to test access, ownership, and transaction-related writes in Firestore rules.

### Excluded Files

Generated outputs and dependency folders — `build/`, `app/build/`, `.gradle/`, `.kotlin/errors/`, and `node_modules/` — are not included in this guide’s file list. IDE workspace metadata and local-only metadata are also not described as part of the source structure. Values from local Firebase and SDK configuration files are not reproduced here.

meeke kisima deyak wens krnne nathuwa meeka ohomma english walin hdala denna