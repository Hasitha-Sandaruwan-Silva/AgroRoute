# AgroRoute

**Smart Agro Cold-Chain & Harvest Pooling App** — Mobile Application Development (MAD) coursework project.

AgroRoute is a harvest-pooling and cold-chain visibility platform for small-scale farmers: farmers list surplus harvest with quality photos, pool transport with nearby farmers via GPS matching, book cold-storage slots, and sell directly to buyers/suppliers through fixed pricing or an open bidding system.

---

## Tech Stack

| Layer | Technology |
|---|---|
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
│   └── components/                   # Reusable UI components (buttons, cards, fields, etc.)
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
2. Add your `google-services.json` to the `app/` module (from Firebase Console).
3. Enable **Firestore Database**, **Authentication (Email/Password)**, and **Storage** in the Firebase Console.
4. Sync Gradle and run on an emulator or device (`minSdk` / `compileSdk` as configured in `app/build.gradle.kts`).

---

## Status

Actively in development — base architecture, Firebase integration, and core Farmer/Supplier screens in progress.

---

## AgroRoute Source Guide

මෙම guide එක දැනට ඇති source files අනුව ලියා ඇත. ඉහත project overview එකේ සඳහන් GPS transport pooling, cold-storage booking සහ Driver flow සඳහා මෙහි source tree එකේ පැහැදිලි implementation එකක් හමු නොවීය; ඒවා ක්‍රියාත්මක බව මෙයින් තහවුරු නොවේ. දැනට order එකක් සහ මිලක් පෙන්වුවත් app එක තුළ payment සැකසීමක් නැත.

### ප්‍රධාන ෆෝල්ඩර්

- `app/src/main/java/com/agroroute/core/` — roles දෙකටම පොදු models, Room, Firebase, repositories, navigation, theme සහ widgets.
- `app/src/main/java/com/agroroute/features/auth/` — sign in, registration සහ account role තේරීම.
- `app/src/main/java/com/agroroute/features/farmer/` — ගොවි dashboard, listings, bids, order requests, orders, profile සහ navigation.
- `app/src/main/java/com/agroroute/features/supplier/` — සැපයුම්කරු dashboard, marketplace, bids, order requests, orders, profile සහ navigation.
- `app/src/main/java/com/agroroute/features/leaderboard/` සහ `app/src/main/java/com/agroroute/features/reviews/` — points අනුව වෙන වෙනම leaderboards සහ order review.
- `app/src/main/res/` — Android manifest, නාම/තේමා resources, app icons සහ images.
- `app/src/test/` — Kotlin/JVM unit tests.
- `rules-tests/` — Firestore rules සඳහා Node.js emulator tests.
- `gradle/` — Gradle wrapper, version catalog සහ build runtime settings.

### Farmer සහ Supplier ගලායන ආකාරය

1. `WelcomeScreen` වෙතින් user `LoginScreen` හෝ `RegisterScreen` වෙත යයි. Email/password හෝ Google sign-in පසු saved role එක අනුව Farmer හෝ Supplier home එක විවෘත වේ.
2. Farmer listing එකක් draft ලෙස device එකේ තබාගත හැකිය, නැතහොත් harvest, ප්‍රමාණය, මිල, category සහ අවශ්‍ය නම් photo එක සමඟ publish කරයි. Listing එකේ bidding සක්‍රීය නම් Supplier bid කරයි; අක්‍රිය නම් fixed-price order request එකක් යවයි. Expected harvest එකක් නම් request එක ඇස්තමේන්තුවක් පමණි.
3. Farmer incoming bids හෝ order requests පරීක්ෂා කර accept/reject කරයි. Accept කිරීමේදී order එක සෑදී listing stock යාවත්කාලීන වේ. Supplier තම bids සහ requests බලමින් order පිළිතුරු බලා සිටී.
4. Order එකේ අදියර `AWAITING_PICKUP` → `IN_TRANSIT` → `DELIVERED` වේ; අවසර ලත් අවස්ථාවල එය `CANCELLED` කළ හැකිය. Supplier order එක delivered ලෙස තහවුරු කළ විට order එකකට එක් වරක් Farmerටත් Supplierටත් point එක බැගින් ලැබේ.
5. `DELIVERED` order එකකට Farmerට Supplierටත්, Supplierට Farmerටත් 1–5-star rating එකක් සහ ලිඛිත review එකක් එක් කළ හැකිය. Review එක points වෙනස් නොකරයි. `FarmerLeaderboard` සහ `SupplierLeaderboard` වෙන වෙනම role අනුව points පෙන්වයි.

### Build, Firebase සහ ලේඛන ගොනු

- `build.gradle.kts` — project-wide Gradle plugin alias-ууд සකසයි.
- `settings.gradle.kts` — Gradle repositories සහ `:app` module එක සකසයි.
- `gradle.properties` — Gradle, AndroidX සහ Kotlin build flags සකසයි.
- `gradle/gradle-daemon-jvm.properties` — Gradle daemon සඳහා JDK toolchain තේරීම සකසයි.
- `gradle/libs.versions.toml` — plugin සහ library versions එක තැනක තබයි.
- `gradle/wrapper/gradle-wrapper.properties` — භාවිත කරන Gradle distribution එක සඳහන් කරයි.
- `gradle/wrapper/gradle-wrapper.jar` — Gradle Wrapper ආරම්භ කරන binary එකයි.
- `gradlew` සහ `gradlew.bat` — macOS/Linux සහ Windows සඳහා Gradle Wrapper scripts වේ.
- `app/build.gradle.kts` — Android app settings, dependencies, Compose සහ test options සකසයි.
- `local.properties` — මෙම පරිගණකයේ Android SDK ස්ථානය තබයි; එහි අගය මෙහි පෙන්වා නැත.
- `.firebaserc` — Firebase CLI project mapping තබයි; project value මෙහි පෙන්වා නැත.
- `app/google-services.json` — Android app එකට අදාල Firebase client configuration එකයි; configuration values මෙහි ඇතුළත් කර නැත.
- `firebase.json` — Firebase rules ගොනු සහ emulator configuration සම්බන්ධ කරයි.
- `firebase.rules-test.json` — Firestore rules tests සඳහා වෙනම emulator configuration එකක් සපයයි.
- `firestore.rules` — Firestore collections සඳහා role, ownership සහ ලිවීමේ අවසර සීමා කරයි.
- `storage.rules` — Firebase Storage listing image path සඳහා අවසර සීමා කරයි.
- `rules-tests/package.json` — Firestore rules test command සහ Node dependencies සකසයි.
- `rules-tests/package-lock.json` — rules test dependencies වල නියමිත versions සටහන් කරයි.
- `.gitignore` — build outputs, local config සහ log වැනි දේ Git වලින් ඉවත් කරයි.
- `README.md` — project overview එක සහ මෙම code guide එක අඩංගු කරයි.
- `LOGICS.md` — app logic පිළිබඳ Sinhala audit ලේඛනයකි.
- `supplier_module_documentation.md` — Supplier module සඳහා ලියූ functional specification එකකි; එය source implementation එකට වඩා වෙනස් විය හැකිය.
- `business-logic-audit/README.md` — දින සහිත audit වාර්තාවකි; එහි සොයාගැනීම් වත්මන් source සමඟ නැවත පරීක්ෂා කළ යුතුය.

### Android ආරම්භය සහ resources

- `app/src/main/AndroidManifest.xml` — application, launcher activity, permissions සහ harvest reminder receiver ලියාපදිංචි කරයි.
- `app/src/main/java/com/agroroute/AgroRouteApp.kt` — Hilt dependency injection සඳහා Application entry point එකයි.
- `app/src/main/java/com/agroroute/MainActivity.kt` — splash screen, theme සහ Compose navigation host එක ආරම්භ කරයි.
- `app/src/main/res/values/colors.xml` — Android resource colors නිර්වචනය කරයි.
- `app/src/main/res/values/strings.xml` — app නම සහ order/payment caption string සපයයි.
- `app/src/main/res/values/themes.xml` — Android window සහ splash themes සකසයි.
- `app/src/main/res/drawable/agroroute.png` — app drawable image asset එකකි; Kotlin source එකෙන් භාවිතය පැහැදිලි නොවේ.
- `app/src/main/res/drawable/agroroute1.png` — welcome header සහ animated logo සඳහා image එකයි.
- `app/src/main/res/drawable/logo.png` — login සහ app header වල පෙන්වන logo image එකයි.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — launcher icon foreground drawable එකයි.
- `app/src/main/res/drawable/splash_icon.xml` — splash screen icon drawable එකයි.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — adaptive launcher icon එකයි.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` — round adaptive launcher icon එකයි.

### `core/database` සහ `core/model`

- `app/src/main/java/com/agroroute/core/database/AppDatabase.kt` — Room entities සහ DAOs එකතු කරයි.
- `app/src/main/java/com/agroroute/core/database/BidDao.kt` — bids කියවීම, සුරැකීම සහ මැකීමේ queries සපයයි.
- `app/src/main/java/com/agroroute/core/database/Converters.kt` — enums සහ string lists SQLite අගයන් බවට පරිවර්තනය කරයි.
- `app/src/main/java/com/agroroute/core/database/DatabaseMigrations.kt` — Room database versions අතර migration සකසයි.
- `app/src/main/java/com/agroroute/core/database/HarvestListingDao.kt` — harvest listings සඳහා local queries සහ writes සපයයි.
- `app/src/main/java/com/agroroute/core/database/OrderCompletionRecordDao.kt` — order completion සහ point-award records ගබඩා කරයි.
- `app/src/main/java/com/agroroute/core/database/OrderDao.kt` — orders සඳහා local queries සහ writes සපයයි.
- `app/src/main/java/com/agroroute/core/database/OrderRequestDao.kt` — order requests සඳහා local queries සහ writes සපයයි.
- `app/src/main/java/com/agroroute/core/database/ReviewDao.kt` — reviews කියවීම සහ සුරැකීමේ queries සපයයි.
- `app/src/main/java/com/agroroute/core/database/UserProfileDao.kt` — profiles සහ role අනුව points rankings කියවයි.
- `app/src/main/java/com/agroroute/core/model/Bid.kt` — Supplier offer එකේ fields සහ status තබයි.
- `app/src/main/java/com/agroroute/core/model/Enums.kt` — user role, listing, harvest, bid, order සහ quantity statuses නිර්වචනය කරයි.
- `app/src/main/java/com/agroroute/core/model/HarvestListing.kt` — listing fields සහ stock/harvest/bidding වෙනස් කරන domain functions තබයි.
- `app/src/main/java/com/agroroute/core/model/Order.kt` — accepted offer/request එකෙන් සාදන order data එකයි.
- `app/src/main/java/com/agroroute/core/model/OrderCompletionRecord.kt` — order එකකට point award එක නැවත නොකිරීමට record එකක් තබයි.
- `app/src/main/java/com/agroroute/core/model/OrderRequest.kt` — Supplier fixed-price order request එකේ data එකයි.
- `app/src/main/java/com/agroroute/core/model/Review.kt` — order participant review එකේ rating සහ comment තබයි.
- `app/src/main/java/com/agroroute/core/model/UserProfile.kt` — Farmer/Supplier account සහ points data එකයි.

### `core/di`, `core/navigation`, `core/notification` සහ `core/remote`

- `app/src/main/java/com/agroroute/core/di/DatabaseModule.kt` — Room database සහ DAOs Hilt හරහා ලබාදෙයි.
- `app/src/main/java/com/agroroute/core/di/FirebaseModule.kt` — Firebase Auth, Firestore සහ Storage instances ලබාදෙයි.
- `app/src/main/java/com/agroroute/core/di/RepositoryModule.kt` — repository interfaces ඒවායේ implementations සමඟ සම්බන්ධ කරයි.
- `app/src/main/java/com/agroroute/core/di/ThemeModule.kt` — theme persistence implementation එක bind කරයි.
- `app/src/main/java/com/agroroute/core/navigation/NavGraph.kt` — screens සම්බන්ධ කර login role අනුව routes සීමා කරයි.
- `app/src/main/java/com/agroroute/core/navigation/Screen.kt` — named navigation routes සහ route arguments නිර්වචනය කරයි.
- `app/src/main/java/com/agroroute/core/notification/HarvestReminderReceiver.kt` — නියමිත දිනට harvest reminder notification එක පෙන්වයි.
- `app/src/main/java/com/agroroute/core/notification/HarvestReminderScheduler.kt` — expected harvest දිනය සඳහා Android alarm එක සකසයි හෝ අවලංගු කරයි.
- `app/src/main/java/com/agroroute/core/remote/AuthService.kt` — Firebase email/password, Google sign-in සහ session actions සපයයි.
- `app/src/main/java/com/agroroute/core/remote/CloudinaryPhotoStorage.kt` — listing photo එක පරීක්ෂා කර Cloudinary වෙත upload කර URL එක ලබාගනී.
- `app/src/main/java/com/agroroute/core/remote/FirestoreService.kt` — Firestore reads, writes, flows සහ transactions සඳහා wrapper එකයි.
- `app/src/main/java/com/agroroute/core/remote/NetworkConnectivityMonitor.kt` — network online තත්ත්වය Flow එකක් ලෙස නිරීක්ෂණය කරයි.
- `app/src/main/java/com/agroroute/core/remote/StorageService.kt` — Firebase Storage image upload/download URL ක්‍රියා සපයයි.

### `core/repository` සහ `core/usecase`

- `app/src/main/java/com/agroroute/core/repository/AuthRepository.kt` — authentication සහ profile මෙහෙයුම් සඳහා contract එකයි.
- `app/src/main/java/com/agroroute/core/repository/AuthRepositoryImpl.kt` — Firebase Auth, Firestore profile සහ Room cache එක සම්බන්ධ කරයි.
- `app/src/main/java/com/agroroute/core/repository/BidRepository.kt` — bid operations සඳහා contract එකයි.
- `app/src/main/java/com/agroroute/core/repository/BidRepositoryImpl.kt` — bid queries, writes, status සහ sync ක්‍රියාත්මක කරයි.
- `app/src/main/java/com/agroroute/core/repository/HarvestListingRepository.kt` — listing සහ harvest status operations සඳහා contract එකයි.
- `app/src/main/java/com/agroroute/core/repository/HarvestListingRepositoryImpl.kt` — listing Firestore/Room writes, ownership checks, stock සහ harvest updates පාලනය කරයි.
- `app/src/main/java/com/agroroute/core/repository/LeaderboardRepository.kt` — role අනුව leaderboard සහ rank operations සඳහා contract එකයි.
- `app/src/main/java/com/agroroute/core/repository/LeaderboardRepositoryImpl.kt` — Farmers සහ Suppliers points ranking වෙන වෙනම ලබාදෙයි.
- `app/src/main/java/com/agroroute/core/repository/ListingPhotoStorage.kt` — photo storage contract, errors සහ storage path helper සපයයි.
- `app/src/main/java/com/agroroute/core/repository/OrderRepository.kt` — orders, acceptance සහ status changes සඳහා contract එකයි.
- `app/src/main/java/com/agroroute/core/repository/OrderRepositoryImpl.kt` — bid/request accept transactions, stock, order status සහ once-only points awards කළමනාකරණය කරයි.
- `app/src/main/java/com/agroroute/core/repository/OrderRequestRepository.kt` — order request operations සඳහා contract එකයි.
- `app/src/main/java/com/agroroute/core/repository/OrderRequestRepositoryImpl.kt` — Supplier request writes, Farmer response සහ sync ක්‍රියාත්මක කරයි.
- `app/src/main/java/com/agroroute/core/repository/ReviewRepository.kt` — review queries සහ submission සඳහා contract එකයි.
- `app/src/main/java/com/agroroute/core/repository/ReviewRepositoryImpl.kt` — completed order, participant, rating සහ duplicate-review checks කර reviews සුරකියි.
- `app/src/main/java/com/agroroute/core/usecase/GetBidsBySupplierUseCase.kt` — Supplier bids ලබාගැනීම repository වෙත යොමු කරයි.
- `app/src/main/java/com/agroroute/core/usecase/GetListingsUseCase.kt` — listings කියවීම සහ sync කිරීම සඳහා actions සපයයි.
- `app/src/main/java/com/agroroute/core/usecase/GetOrdersUseCase.kt` — Farmer/Supplier order queries සහ single-order query සපයයි.
- `app/src/main/java/com/agroroute/core/usecase/WithdrawBidUseCase.kt` — Supplier bid withdrawal repository වෙත යොමු කරයි.

### `core/theme`, `core/util`, `core/components`

- `app/src/main/java/com/agroroute/core/theme/Color.kt` — light/dark theme colors නිර්වචනය කරයි.
- `app/src/main/java/com/agroroute/core/theme/ExtendedColors.kt` — screens සඳහා අමතර light/dark colors සපයයි.
- `app/src/main/java/com/agroroute/core/theme/Theme.kt` — Material theme, status bar colors සහ shapes සකසයි.
- `app/src/main/java/com/agroroute/core/theme/ThemePreference.kt` — system/light/dark තේරීම device එකේ සුරකියි.
- `app/src/main/java/com/agroroute/core/theme/ThemeViewModel.kt` — theme state UI එකට ලබාදෙයි.
- `app/src/main/java/com/agroroute/core/theme/Type.kt` — Compose typography styles සකසයි.
- `app/src/main/java/com/agroroute/core/util/CloudinaryConfig.kt` — Cloudinary upload endpoint settings සහ photo limits තබයි; secret එකක් නොවිය යුතුය.
- `app/src/main/java/com/agroroute/core/util/Constants.kt` — Firestore collection, Room database සහ navigation key names තබයි.
- `app/src/main/java/com/agroroute/core/util/Extensions.kt` — date, currency, string සහ `Result` helpers සපයයි.
- `app/src/main/java/com/agroroute/core/util/HarvestAvailabilityDate.kt` — harvest date එක පෙන්වීමට format කරයි.
- `app/src/main/java/com/agroroute/core/util/Result.kt` — Loading, Success සහ Error result states නිර්වචනය කරයි.
- `app/src/main/java/com/agroroute/core/components/AgroTextField.kt` — app එකේ භාවිත කරන පොදු text field එකයි.
- `app/src/main/java/com/agroroute/core/components/AnimatedBottomBar.kt` — animated bottom navigation composable එකයි.
- `app/src/main/java/com/agroroute/core/components/AppCard.kt` — click සහ non-click card composable එකයි.
- `app/src/main/java/com/agroroute/core/components/EmptyState.kt` — හිස් ලැයිස්තු සඳහා message view එකයි.
- `app/src/main/java/com/agroroute/core/components/GradientHeader.kt` — gradient title header එකයි.
- `app/src/main/java/com/agroroute/core/components/ListingImage.kt` — listing photo, loading සහ fallback icon පෙන්වයි.
- `app/src/main/java/com/agroroute/core/components/LoadingButton.kt` — loading state සහිත action button එකයි.
- `app/src/main/java/com/agroroute/core/components/OrderTimeline.kt` — order status timeline සහ progress strip පෙන්වයි.
- `app/src/main/java/com/agroroute/core/components/PhotoPicker.kt` — system photo picker එකෙන් image තෝරයි.
- `app/src/main/java/com/agroroute/core/components/PriceInputField.kt` — decimal price input එක සීමා කරයි.
- `app/src/main/java/com/agroroute/core/components/RoleBadge.kt` — Farmer/Supplier role badge පෙන්වයි.
- `app/src/main/java/com/agroroute/core/components/RoleSelectCard.kt` — account role තේරීමේ card එකයි.
- `app/src/main/java/com/agroroute/core/components/SectionTitle.kt` — section title සහ optional action පෙන්වයි.
- `app/src/main/java/com/agroroute/core/components/SettingsSection.kt` — theme සහ profile settings actions පෙන්වයි.
- `app/src/main/java/com/agroroute/core/components/Shimmer.kt` — loading skeleton composables සපයයි.
- `app/src/main/java/com/agroroute/core/components/SnackbarHost.kt` — snackbar messages සහ error effects පෙන්වයි.
- `app/src/main/java/com/agroroute/core/components/StatCard.kt` — dashboard count/value card පෙන්වයි.
- `app/src/main/java/com/agroroute/core/components/StatusChip.kt` — listing, bid, order සහ harvest statuses සඳහා chips සපයයි.
- `app/src/main/java/com/agroroute/core/components/WidgetPreviews.kt` — shared widgets සඳහා Compose previews අඩංගු කරයි.
- `app/src/main/java/com/github/thelacspace/icons/Lucide.kt` — code එකේ icon names Material icons වෙත map කරයි.

### `features/auth`, `features/welcome`, `features/settings`

- `app/src/main/java/com/agroroute/features/auth/AuthViewModel.kt` — login, registration, Google sign-in සහ session UI state කළමනාකරණය කරයි.
- `app/src/main/java/com/agroroute/features/auth/GoogleRoleSelectionScreen.kt` — අලුත් Google account එකකට Farmer/Supplier role තෝරයි.
- `app/src/main/java/com/agroroute/features/auth/LoginScreen.kt` — email/password සහ Google sign-in UI පෙන්වයි.
- `app/src/main/java/com/agroroute/features/auth/LoginUseCase.kt` — login input පරීක්ෂා කර repository එක අමතයි.
- `app/src/main/java/com/agroroute/features/auth/RegisterScreen.kt` — account creation සහ role selection UI පෙන්වයි.
- `app/src/main/java/com/agroroute/features/auth/RegisterUseCase.kt` — registration input පරීක්ෂා කර repository එක අමතයි.
- `app/src/main/java/com/agroroute/features/auth/SendPasswordResetEmailUseCase.kt` — email පරීක්ෂා කර reset email එක යවයි.
- `app/src/main/java/com/agroroute/features/welcome/AnimatedLogoHeader.kt` — animated logo සහ app title header එක පෙන්වයි.
- `app/src/main/java/com/agroroute/features/welcome/WelcomeScreen.kt` — onboarding pages සහ register/login වෙත යාම පෙන්වයි.
- `app/src/main/java/com/agroroute/features/settings/SettingsScreens.kt` — role අනුව Help, About සහ Privacy screens පෙන්වයි.

### `features/farmer`

- `app/src/main/java/com/agroroute/features/farmer/bids/AcceptBidUseCase.kt` — Farmer bid පිළිගැනීම/ප්‍රතික්ෂේපය validate කරයි.
- `app/src/main/java/com/agroroute/features/farmer/bids/BidsViewModel.kt` — Farmer bid lists, sync සහ bid actions කළමනාකරණය කරයි.
- `app/src/main/java/com/agroroute/features/farmer/bids/FarmerBidsScreen.kt` — incoming bids filter කර accept/reject කිරීමට පෙන්වයි.
- `app/src/main/java/com/agroroute/features/farmer/bids/ViewBidsScreen.kt` — listing එකකට bids පෙන්වා accept/reject actions සපයයි.
- `app/src/main/java/com/agroroute/features/farmer/calendar/HarvestCalendarScreen.kt` — ඉදිරියේ ඇති harvest dates ලැයිස්තුගත කර reminders සකසයි.
- `app/src/main/java/com/agroroute/features/farmer/home/FarmerHomeScreen.kt` — listing/order totals සහ Farmer shortcuts පෙන්වයි.
- `app/src/main/java/com/agroroute/features/farmer/listing/CloseListingUseCase.kt` — open listing එකක් close කිරීම පරීක්ෂා කරයි.
- `app/src/main/java/com/agroroute/features/farmer/listing/CreateListingScreen.kt` — listing සෑදීම, edit කිරීම, draft සහ harvest details UI එකයි.
- `app/src/main/java/com/agroroute/features/farmer/listing/CreateListingUseCase.kt` — listing fields පරීක්ෂා කර photo upload පසු publish කරයි.
- `app/src/main/java/com/agroroute/features/farmer/listing/ListingViewModel.kt` — Farmer listings/drafts load කර actions repository/use cases වෙත යොමු කරයි.
- `app/src/main/java/com/agroroute/features/farmer/listing/MyListingsScreen.kt` — Farmer listings සොයයි, sort/filter කර manage කරයි.
- `app/src/main/java/com/agroroute/features/farmer/listing/ScheduleHarvestReminderUseCase.kt` — listing එකකට reminder එක schedule කරයි.
- `app/src/main/java/com/agroroute/features/farmer/listing/ToggleBiddingModeUseCase.kt` — listing එකක bidding on/off කිරීම validate කරයි.
- `app/src/main/java/com/agroroute/features/farmer/listing/UpdateListingUseCase.kt` — listing edit එක validate කර pending bids/orders සහ photo upload පරීක්ෂා කරයි.
- `app/src/main/java/com/agroroute/features/farmer/orderrequests/FarmerOrderRequestsScreen.kt` — incoming direct requests filter කර accept/reject කරයි.
- `app/src/main/java/com/agroroute/features/farmer/orderrequests/FarmerOrderRequestsViewModel.kt` — Farmer requests සහ listing data load කර actions සකසයි.
- `app/src/main/java/com/agroroute/features/farmer/orderrequests/OrderRequestUseCases.kt` — direct request accept/reject සඳහා Farmer-side checks කරයි.
- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderDetailScreen.kt` — order detail, status timeline සහ Supplierට review action පෙන්වයි.
- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrderViewModel.kt` — Farmer orders load කර status actions හසුරුවයි.
- `app/src/main/java/com/agroroute/features/farmer/orders/FarmerOrdersScreen.kt` — Farmer order history filter කර summary පෙන්වයි.
- `app/src/main/java/com/agroroute/features/farmer/profile/FarmerProfileScreen.kt` — Farmer profile, points සහ Farmer leaderboard link පෙන්වයි.
- `app/src/main/java/com/agroroute/features/farmer/shell/FarmerBottomBar.kt` — Farmer tab navigation සපයයි.
- `app/src/main/java/com/agroroute/features/farmer/shell/FarmerScaffold.kt` — Farmer top/bottom bars සහ screen content එකට එක් කරයි.
- `app/src/main/java/com/agroroute/features/farmer/shell/FarmerTopBar.kt` — Farmer route title, profile menu සහ sign-out controls පෙන්වයි.

### `features/supplier`, `features/leaderboard`, `features/reviews`

- `app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidScreen.kt` — listing එකකට Supplier offer එකක් සකසා යවයි.
- `app/src/main/java/com/agroroute/features/supplier/bidding/PlaceBidUseCase.kt` — Supplier role, offer සහ listing eligibility පරීක්ෂා කරයි.
- `app/src/main/java/com/agroroute/features/supplier/bids/SupplierBidsScreen.kt` — Supplierගේ bids සහ pending bid withdrawal action පෙන්වයි.
- `app/src/main/java/com/agroroute/features/supplier/bids/SupplierBidsViewModel.kt` — Supplier bid history load කර withdraw action හසුරුවයි.
- `app/src/main/java/com/agroroute/features/supplier/home/SupplierHomeScreen.kt` — marketplace, bids සහ orders සඳහා Supplier dashboard පෙන්වයි.
- `app/src/main/java/com/agroroute/features/supplier/marketplace/BrowseListingsScreen.kt` — eligible listings සොයමින් bid හෝ fixed-price request flow තෝරයි.
- `app/src/main/java/com/agroroute/features/supplier/marketplace/MarketplaceViewModel.kt` — listings sync කර marketplace state සපයයි.
- `app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestScreen.kt` — fixed-price හෝ pre-harvest request quantity යවයි.
- `app/src/main/java/com/agroroute/features/supplier/orderrequest/PlaceOrderRequestUseCase.kt` — Supplier request, quantity සහ listing eligibility validate කරයි.
- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderDetailScreen.kt` — Supplier order detail, delivery confirmation සහ Farmer review action පෙන්වයි.
- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrderViewModel.kt` — Supplier orders/requests load කර status/cancel actions හසුරුවයි.
- `app/src/main/java/com/agroroute/features/supplier/orders/SupplierOrdersScreen.kt` — Supplierගේ requests සහ orders පෙන්වයි.
- `app/src/main/java/com/agroroute/features/supplier/profile/SupplierProfileScreen.kt` — Supplier profile, points සහ Supplier leaderboard link පෙන්වයි.
- `app/src/main/java/com/agroroute/features/supplier/shell/SupplierBottomBar.kt` — Supplier tab navigation සපයයි.
- `app/src/main/java/com/agroroute/features/supplier/shell/SupplierScaffold.kt` — Supplier top/bottom bars සහ screen content එකට එක් කරයි.
- `app/src/main/java/com/agroroute/features/supplier/shell/SupplierTopBar.kt` — Supplier route titles, profile menu සහ route lists සපයයි.
- `app/src/main/java/com/agroroute/features/leaderboard/LeaderboardScreen.kt` — role එක අනුව leaderboard සහ review score පෙන්වයි.
- `app/src/main/java/com/agroroute/features/leaderboard/LeaderboardViewModel.kt` — role-specific points rankings සහ review metrics load කරයි.
- `app/src/main/java/com/agroroute/features/reviews/ReviewViewModel.kt` — order participant review submission state කළමනාකරණය කරයි.
- `app/src/main/java/com/agroroute/features/reviews/SubmitReviewScreen.kt` — 1–5-star rating සහ ලිඛිත review ලබාගනී.

### Unit tests සහ Firestore rules tests

- `app/src/test/java/com/agroroute/core/navigation/AuthRoleRoutingTest.kt` — saved role සහ Google sign-in outcome අනුව home route පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/core/theme/ThemePreferenceTest.kt` — theme mode load/persist වීම පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/core/usecase/GetBidsBySupplierUseCaseTest.kt` — Supplier bids query forwarding සහ error result පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/core/usecase/WithdrawBidUseCaseTest.kt` — bid withdrawal delegation සහ error propagation පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/auth/SendPasswordResetEmailUseCaseTest.kt` — email validation සහ reset request පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/farmer/home/FarmerNavigationTest.kt` — Farmer home shortcuts සහ role routes පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/farmer/listing/CloudinaryUploadTest.kt` — photo upload success/failure සහ returned URL handling පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/farmer/listing/HarvestListingRulesTest.kt` — listing, stock, bid, harvest සහ photo rules සඳහා unit checks අඩංගු කරයි.
- `app/src/test/java/com/agroroute/features/farmer/listing/UpdateListingUseCaseTest.kt` — edit ownership, pending bids/orders සහ photo replacement පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/orderrequests/OrderRequestFlowTest.kt` — request placement, acceptance, stock, harvest සහ ownership flows පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/orders/OrderStatusTransitionTest.kt` — Farmer/Supplier order status transition rules පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/points/PointAwardingTest.kt` — order completion point award සහ duplicate protection පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/reviews/ReviewConstraintsTest.kt` — review participant, completion, uniqueness සහ rating constraints පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/supplier/home/SupplierNavigationTest.kt` — Supplier routes සහ shared settings navigation පරීක්ෂා කරයි.
- `app/src/test/java/com/agroroute/features/supplier/marketplace/MarketplaceEligibilityTest.kt` — marketplace listing eligibility සහ bid action පරීක්ෂා කරයි.
- `rules-tests/firestore.rules.test.mjs` — Firestore rules වල access, ownership සහ transaction-related writes emulator එකෙන් පරීක්ෂා කරයි.

### ඉවත් කළ ගොනු

Generated outputs සහ dependency folders — `build/`, `app/build/`, `.gradle/`, `.kotlin/errors/`, `node_modules/` — මෙම guide එකේ file list එකට ඇතුළත් කර නැත. IDE workspace metadata සහ local-only metadata ද source structure ලෙස විස්තර කර නැත. Firebase සහ SDK local configuration files වල values මෙහි නැවත පෙන්වා නැත.
