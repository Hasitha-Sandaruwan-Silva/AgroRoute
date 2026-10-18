# AgroRoute

**Smart Farmer–Supplier Marketplace & Delivery Platform**

## Overview

AgroRoute connects farmers and suppliers through an Android marketplace for harvest listings, requests and bids, orders, pickup, delivery, maps, chat, and notifications. Room caches synchronized data locally for use when connectivity is limited.

## Main Features

| Role | Features |
| --- | --- |
| Farmer | Create/manage produce listings; CameraX/gallery photos; quantity, pricing and expiry; pickup location; receive requests/bids; accept orders; pickup-code verification; order tracking; chat; notifications; profiles and leaderboards. |
| Supplier | Browse produce; submit requests/bids; select delivery location; accepted-order dashboard; active-order Map tab; pickup/delivery navigation; tracking; chat; notifications; participant profiles. |
| Shared | Authentication, light/dark themes, local caching, maps and location, profile images, ratings/reviews, and responsive Compose UI. |

## Order Lifecycle

```text
Supplier Request/Bid → Farmer Accepts → AWAITING_PICKUP
→ Farmer verifies pickup code → Supplier confirms pickup
→ IN_TRANSIT → Supplier confirms delivery → DELIVERED
```

The current lifecycle uses Firestore transactions protected by Security Rules and is Spark-compatible. Cloud Functions are not required at runtime. Older trusted-backend implementations remain in `functions/` for reference; review their compatibility before deploying.

## Architecture

MVVM separates screens, state and data access. Hilt supplies dependencies, feature use cases coordinate actions, and shared services support business logic.

```text
Jetpack Compose UI → ViewModel → UseCase / Service
→ Repository → Firebase / Room (SQLite)
```

## Technology Stack

| Area | Technologies |
| --- | --- |
| Android | Kotlin, Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM, Hilt, repositories, use cases |
| Data | Room / SQLite, Cloud Firestore |
| Security | Firebase Authentication, Firebase Security Rules |
| Photos | Cloudinary unsigned uploads, CameraX, gallery picker, Coil |
| Maps | MapLibre, MapTiler, Android location services |
| Concurrency | Coroutines / Flow |
| Notifications | Firestore inbox, Android local notifications, Firebase Messaging client integration |

## Maps & Location

Embedded maps use **MapLibre + MapTiler**. Orders preserve pickup/delivery location snapshots, and the Supplier active-order Map tab shows current orders. External navigation may open a Maps app or browser. Embedded road routing and live ETA calculations are not provided.

## Notifications

The Firestore-backed in-app inbox maintains unread state. Android local notifications are available while the app process is available and notification permission is granted. Exact-chat suppression avoids local alerts for the conversation currently being viewed while maintaining read state.

Process-independent push needs a configured trusted FCM sender/backend. Client integration alone does not provide force-stopped delivery; Android may require a force-stopped app to be reopened before notifications resume.

## Security

Firebase Authentication identifies users. Firestore Rules enforce participant-specific access and lifecycle constraints. The current Spark pickup flow stores a salted **PBKDF2-HMAC-SHA256** verifier, not a plaintext pickup PIN.

Preserve and deploy `firestore.rules` and `storage.rules`. Keep local credentials, service-account keys and signing material outside version control.

## Project Structure

```text
app/
├── build.gradle.kts
├── google-services.json
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/agroroute/
    │   │   ├── AgroRouteApp.kt
    │   │   ├── MainActivity.kt
    │   │   ├── core/
    │   │   │   ├── database/
    │   │   │   ├── di/
    │   │   │   ├── location/
    │   │   │   ├── navigation/
    │   │   │   ├── notification/
    │   │   │   ├── remote/
    │   │   │   ├── repository/
    │   │   │   └── usecase/
    │   │   └── features/
    │   └── res/
    ├── debug/                   # Debug runtime and demo experience
    └── release/                 # Release runtime implementations
functions/                       # Trusted-backend/reference source
gradle/                         # Wrapper and version catalog
firebase.json                    # Deployment configuration
firestore.rules
storage.rules
local.properties.example         # Placeholder-only setup
```

`core/` holds shared models, persistence, migrations, dependency wiring, navigation, maps, notifications and repositories. `features/` contains authentication, farmer/supplier screens, messaging, reviews, leaderboards and participant profiles. `res/` contains application images, launcher/splash artwork, themes, strings and XML configuration.

## Setup

1. Clone this repository and open its root in Android Studio.
2. Select Android Studio's bundled JDK as the Gradle JDK, compatible with this project's Gradle 8.7 wrapper (Java 17 is suitable). Install Android SDK Platform 35. Devices require Android 8.0 / API 26 or later.
3. Register `com.agroroute` in your Firebase project and place its `google-services.json` in `app/`. Enable the authentication providers you use and create Firestore. Configure signing fingerprints for Google sign-in.
4. Review and deploy the Firestore and Storage Rules to your intended Firebase project. Check `.firebaserc` before CLI deployment. Deploying legacy Functions is not required for the Spark lifecycle.
5. Add your MapTiler key to untracked `local.properties`, preserving Android Studio's `sdk.dir`. See `local.properties.example`:

   ```properties
   MAPTILER_API_KEY=YOUR_MAPTILER_KEY
   ```

   Never commit `local.properties` or real credentials.
6. For your own photo-upload environment, configure the Cloudinary account and restricted unsigned preset matching `core/util/CloudinaryConfig.kt`. Never put an API secret in the Android client.
7. Sync Gradle, select a device/emulator and run `app`. Grant camera, location and notification permissions as needed.

## Build

Windows, from the project root with the Gradle JDK configured:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
.\gradlew.bat lint
```

On macOS/Linux, use `./gradlew` with the same tasks. Outputs are generated under `app/build/`. Release distribution requires your own signing setup; private signing keys are not supplied. Android test sources are intentionally excluded from this final project version.

## Screens / UI

Run the app to review welcome/login, role dashboards, listings, marketplace, orders, maps, chat, notifications and profiles. Temporary preview exports are not maintained as README screenshots.

## Notes / Limitations

- Process-independent notifications require a trusted FCM backend; force-stopped delivery is subject to Android restrictions.
- External navigation requires an available app or browser.
- MapLibre + MapTiler maps need a valid key and connectivity for remote tiles.
- Local caching does not make authentication, photo uploads or every write available offline.
- Older Functions flows require compatibility review before deployment with current rules.

## Authors / Academic Project

AgroRoute is a Mobile Application Development (MAD) coursework project. The repository does not provide a verified team roster.
