# AgroRoute — වෙනස්කම්, පරීක්ෂණ සහ run කරන පියවර

නව order pickup/delivery snapshot implementation සහ අවසන් tests/migration විස්තර [ORDER_LOCATION_FLOW-SI.md](ORDER_LOCATION_FLOW-SI.md) තුළ ඇත. පහත කොටස් පෙර profile/points/provider audit එකේ විස්තර ද පවත්වා ගනී.

## Source සහ backup

2026-10-09 දින workspace තුළ තිබූ project files source of truth ලෙස භාවිත කළා. ඔබගේ පසුකාලීන තේරීම අනුව අවසන් location picker එක **MapLibre + MapTiler** භාවිත කරයි. නව migration විස්තර සහ key setup සඳහා [MAPTILER_SETUP-SI.md](MAPTILER_SETUP-SI.md) බලන්න.

Existing `allDone` branch/commit `70c9bfe` වෙනස් නොකළා. මුල් tracked files `delivery/baseline/allDone-70c9bfe.zip` සහ එවකට තිබූ files `delivery/baseline/working-files/` තුළ සුරකිමින්, මුල් Git status එකත් සුරැකුවා. `app/src.zip` මුල සිට delete වී තිබුණා; එම deletion එක මේ වැඩෙන් නොකළ එකක්. වැඩ branch එක `location-profile-audit-20261009` වේ. Remote එක වෙනස් කර නැහැ; push/deploy කර නැහැ.

## Location picker

- Farmer සහ Supplier දෙපාර්ශ්වයේ **Profile → Edit profile → Choose location on map**.
- Farmer **Create/Edit listing → Pickup location → Choose pickup location on map**.
- Supplier order details තුළ awaiting-pickup/in-transit orders සඳහා **Choose / edit delivery location**.
- MapLibre map එක pan/zoom කර map එක tap කිරීමෙන් green pin එකේ ස්ථානය තෝරාගත හැක. Blue current-location dot සහ floating back/recenter controls තිබේ. Address/place search, current location, address label editing, latitude/longitude display, confirm/cancel ක්‍රියා තිබෙනවා.
- Current location button එක ඔබ එබූ විට පමණක් fine/coarse runtime permission ඉල්ලයි. Approximate location එකටත් network provider භාවිත කළ හැක. Permission ප්‍රතික්ෂේප කළත් configured map/search භාවිත කළ හැක.
- Recent position එකක් නැති විට fresh location request එකට තත්පර 15ක සීමාවක් තිබෙනවා. Dialog එක වැසීමෙන් location listener ඉවත් කරයි. Geocoder calls UI thread එකෙන් පිටත ධාවනය කරයි.
- Confirm කිරීම form එක පුරවයි; profile/listing සඳහා එම form එකේ **Save/Publish** button එකත් එබිය යුතුයි. Delivery location එක order screen එකේ confirm කළ විට backend සහ Room වෙත save වේ.
- Existing string fields තුළ `Address\n[latitude, longitude]` ලෙස coordinates ද රඳවා ගනී. Old address-only values rewrite නොකරයි. Listing location එකෙන් order pickup location එකට යන existing flow එක පවත්වා ගෙන තිබෙනවා. පෙර provider phase එකේ Room version 6 පවත්වා ඇත; නව order flow එකේ v7 migration bids/requests සඳහා delivery columns දෙකක් එක් කරයි.

**MapTiler key setup සඳහා [MAPTILER_SETUP-SI.md](MAPTILER_SETUP-SI.md) බලන්න.** Build configuration එක local.properties හි MAPTILER_API_KEY කියවයි. Search/reverse geocoding සඳහා Android Geocoder පවත්වා ඇත.

## Profile, phone සහ photos

`AuthRepositoryImpl.updateProfile` දැනටමත් `users/{uid}.phone` සුරකිමින් editable fields පමණක් update කර Room cache එක refresh කරයි. එම phone save logic එක rewrite නොකළා. Farmer/Supplier profile persistence tests දෙකෙන් phone/location/photo URL සුරැකීම, role/points ආරක්ෂාව සහ වෙනත් user profile update ප්‍රතික්ෂේප කිරීම පරීක්ෂා කළා. Existing `ProfilePhoneContact` components marketplace/bid/order screens තුළ නිවැරදි counterparty ID එක භාවිත කරනවා. Backend එකේ phone එක නොදා තිබේ නම් number එක app එකෙන් නිර්මාණය කර පෙන්වන්නේ නැහැ.

Profile editor එක save request එකට පෙර වැසී නොයයි. Save fail වුණොත් input සහ error එක dialog එකේම තිබෙනවා. Simultaneous profile/photo save ක්‍රියා guard කර තිබෙනවා.

Listing photos දැනටමත් Cloudinary unsigned upload භාවිත කළා. `CloudinaryPhotoStorage.kt` හි size limit එකට පෙර මුළු file එකම memory වෙත කියවීම bounded read එකකින් වෙනස් කළා. Replacement listing photos සඳහා unique public IDs දමා response close කිරීම නිවැරදි කළා. Unsigned uploads එකම ID overwrite නොකරන නිසා unique IDs අවශ්‍යයි. [Cloudinary Upload API](https://cloudinary.com/documentation/image_upload_api_reference)

`CloudinaryConfig.kt` හි existing cloud name සහ listing preset අගයන් වෙනස් නොකළා. Console account/preset settings මෙතැනින් verify කළ නොහැකි වුණා. **Profile preset නම හිතෙන් හදා නැහැ**: `PROFILE_UNSIGNED_PRESET` හිස්ව තිබෙන විට පැරණි Firebase Storage avatar upload එක භාවිත කරයි. Firebase Storage ඔබගේ project එකේ නොවැඩ කරන්නේ නම්:

1. Cloudinary Console → Settings → Upload → Upload presets තුළ profile uploads සඳහා **Unsigned** preset එකක් සකසන්න.
2. JPEG/PNG/WebP formats සහ අවශ්‍ය profile folder/dimension restrictions යොදන්න. මේ client එක unique `public_id` යවන නිසා preset එක ඒ parameter එක පිළිගත යුතුයි.
3. එහි නිවැරදි **non-secret preset name** එක `app/src/main/java/com/agroroute/core/util/CloudinaryConfig.kt` හි `PROFILE_UNSIGNED_PRESET` වෙත දාන්න. Cloud name වැරදි නම් එම file එකේ `CLOUD_NAME` පමණක් නිවැරදි කරන්න.
4. Listing preset එක listing folder/restrictions වලට සීමා නම් profile සඳහා වෙනම preset එක භාවිත කරන්න. එකම preset එක දෙකටම ගැළපේද කියා Console එකෙන් තහවුරු නොකළ නිසා reuse එක අනුමාන කර නැහැ.

Cloudinary API Secret, Admin API credentials හෝ passwords app එකට එක් කර නැහැ. Unsigned presets සහ public image URLs authentication/private access controls නොවේ. Preset settings ගැන [Cloudinary documentation](https://cloudinary.com/documentation/upload_presets) බලන්න. Photo-upload success actual Cloudinary/Firebase project එකක පරීක්ෂා කළේ නැහැ; profile preset අවශ්‍ය නම් ඔබ එය config කළ යුතුයි.

## Points සහ leaderboard

ඔබගේ අනුමැතිය අනුව අසුරක්ෂිත client `+1` points write නවත්වා තිබෙනවා. දැන් Supplier **Mark delivered** request එක `completeOrder` trusted callable එකට යයි. Backend එක Auth UID, order participant roles, `IN_TRANSIT` status සහ completion receipt එක පරීක්ෂා කර එක transaction එකකින්:

1. Order එක `DELIVERED` කරයි.
2. `order_completion_records/{orderId}` receipt එකක් සෑදේ.
3. Farmer සහ Supplier points +1 බැගින් update කරයි.

Concurrent/retry request එකකින් points දෙවරක් නොලැබේ. Initial profile points 0 පමණක් client create කළ හැක; direct client points/receipt writes deny කර ඇත. Delivered transition එකත් callable එකෙන් පමණක් සිදුවේ. Profile fields backend transaction එකෙන් overwrite නොකරයි. Android එකට trusted order/profile values නැවත cache වෙයි. Leaderboard එක දැනටමත් backend profiles sync කර Room `user_profiles` points අනුව sort කරයි; එය rewrite නොකළා.

**නව function එක ඔබගේ backend එකට තවම deploy කර නැහැ.** Updated app එකේ delivery completion පරීක්ෂා කිරීමට පළමුව වෙනම test backend එකක Functions/rules යොදන්න. Production deploy කිරීම මෙහි සිදු කළේ නැහැ. පැරණි app clients direct delivered writes කරන නිසා updated rules සමඟ app/backend rollout එක සම්බන්ධීකරණය කළ යුතුයි.

## Notifications audit

Existing FCM token registration/logout cleanup, authenticated notification callable, counterparty resolution, inbox storage/readAt, unread filter, role-allowed routes සහ Android notification permission UI පරීක්ෂා කළා. Server inbox documents `user_notifications/{uid}/items/{id}` යටතේ Admin backend එකෙන් ලියයි; client read/readAt operations callable endpoints භාවිත කරයි. Local harvest inbox SharedPreferences තුළ account අනුව රඳවා ගනී. Registered service එක `FirebaseNotificationService` ය; අනෙක් legacy messaging class එක අලුතින් register කළේ නැහැ.

දැනට තිබූ `BootCompletedReceiver` Manifest එකේ නොතිබුණු නිසා permission/receiver එක් කළා. Reminder restore වෙනත් account එකේ listing එක current user එකට පවරන අවදානම වැළැක්වීමට reminder owner metadata එක SharedPreferences තුළ සුරකිනවා. Ownerless legacy reminders farmer listing sync පසු නැවත සකස් වේ. Privacy wording එක supplied rules හි signed-in profile reads වලට ගැළපෙන ලෙස නිවැරදි කළා.

FCM delivery සහ live inbox navigation device දෙකක real/test accounts සමඟ end-to-end පරීක්ෂා කර නැහැ. Cloud Functions deployment, FCM token availability, device settings සහ network මත ඒවා රඳා පවතී. Notification requests existing business write එකට පසු client callable ලෙස යයි; network failure එකක delivery retry guarantee එකක් අලුතින් එක් කර නැහැ.

## Local database සහ backend භාවිතය

| Local Room table / DAO | භාවිතය |
| --- | --- |
| `user_profiles` / UserProfileDao | Current profile සහ leaderboard cache; Firestore `users` source |
| `harvest_listings` / HarvestListingDao | Listings cache සහ device-only drafts; published listings Firestore `listings` |
| `bids` / BidDao | Supplier/farmer bid lists cache; Firestore `bids` |
| `orders` / OrderDao | Participant order lists/details cache; Firestore `orders` |
| `order_requests` / OrderRequestDao | Direct/pre-harvest request cache; Firestore `order_requests` |
| `reviews` / ReviewDao | Review cache; Firestore `reviews` |
| `order_completion_records` / OrderCompletionRecordDao | Existing local schema පවත්වා ඇත; නව awarding/idempotency සඳහා authoritative receipt එක backend තුළයි |

Repositories සාමාන්‍යයෙන් backend write සාර්ථක වූ පසු Room mirror එක update කරයි. UI Room flows භාවිත කරයි; explicit sync backend rows cache කරයි. Room migrations 1→7 තිබෙනවා. නව order flow එකේ destructive fallback ඉවත් කර ඇත; supported upgrade එක additive migration එකෙන් සිදුවේ. Orders/listings/profile tables rewrite නොවේ. Notification inbox/reminders සහ theme preference Room entities නොවෙයි.

## Validation

Build JVM: Android Studio JBR **21.0.10**, Gradle **8.7**, AGP **8.6.0**; app Java/Kotlin target **17**. Default Java 26 භාවිත නොකළා.

- Android unit tests: 149 සමත්, failures/errors 0 (MapLibre final validation සඳහා delivery/maplibre-final-build.log බලන්න).
- Kotlin/Java compile සහ `:app:assembleDebug`: නව MapLibre build එක සාර්ථකයි (28s); ප්‍රතිඵල සඳහා `delivery/maplibre-final-build.log` සහ `MAPTILER_SETUP-SI.md` බලන්න.
- Firestore Emulator rules tests: **91 සමත්**; delivery edit authorization, completed/cancelled edit denial, direct delivered/points writes සහ initial points protection ඇතුළත්.
- Functions TypeScript compile + notification policy tests: **3 සමත්**.
- Backend completion transaction emulator tests: **5 සමත්**, concurrent retry requests 4ක් සමඟ one-time award තහවුරු කළා.
- Sample fixture: **dummy documents 8ක්** `demo-agroroute-sample` local emulator එකේ seed/validate කළා.
- `git diff --check` පරීක්ෂා කර ඇත. Existing deprecation warnings තිබිය හැක; warnings සියල්ල ඉවත් කරන UI rewrite එකක් කළේ නැහැ.

පෙර audit එකේ compile/test issues නිවැරදි කර නැවත tests ධාවනය කළා. අවසන් provider MapLibre + MapTiler ය. Actual MapTiler key නොමැති නිසා map authorization/rendering සහ GPS/permission UI flow device එකේ end-to-end certify කර නැහැ; MAPTILER_SETUP-SI.md හි device checks කරන්න.

## Android Studio එකට files දමා run කිරීම

1. ඔබගේ project එකේ backup එකක් සුරකින්න.
2. `src.zip` extract කර එහි **src/** folder එක existing project එකේ **app/src/** වෙත copy/replace කරන්න.
3. Existing project එකක් update කරනවා නම් `others-changed-files.zip` extract කර එහි files project root එකට relative paths පවත්වා copy කරන්න. මෙහි source Manifest එක නොමැත; එය `src.zip` තුළයි.
4. සම්පූර්ණ project assembly සඳහා `others.zip` extract කරන්න. එහි **others/** folder එක project root එකයි. `src.zip` හි **src/** එම root එකේ **app/src/** යටතට දාන්න. `others/` නම තවත් nested directory එකක් ලෙස copy කරන්න එපා.
5. ඔබගේ machine SDK path එක `local.properties` හි සකසා [MapTiler setup](MAPTILER_SETUP-SI.md) අනුව `MAPTILER_API_KEY` එක් කරන්න. Firebase Android client config `app/google-services.json` ඔබගේ intended test project/application එකට ගැළපෙන බව බලන්න.
6. අවශ්‍ය නම් Cloudinary profile preset config කරන්න; ඉහත photos කොටස බලන්න.
7. Android Studio Gradle JDK compatible 17/21 ලෙස තෝරා Sync/Rebuild කරන්න. මෙහි tested JVM එක JBR21 ය.
8. PowerShell project root එකේ `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` ධාවනය කරන්න. Android Studio Run භාවිත කරන්න.
9. Updated Functions සහ Firestore rules **වෙනම test Firebase project එකකට** සකස් කළ පසු profile/contact/order/points/notification flows පරීක්ෂා කරන්න. App එක supplied Firebase config භාවිත කරන නිසා test project එක තෝරා ගැනීම වැදගත්ය. Production වෙනස් කිරීමට ඔබගේ වෙනම අනුමැතිය අවශ්‍ය වේ.
10. Sample data සඳහා [sample-data/README-SI.md](sample-data/README-SI.md) භාවිත කරන්න. Auth users සෑදීම සහ Firestore sample profiles සෑදීම වෙනම ක්‍රියා දෙකකි.

Files සහ ZIP SHA-256 verification සඳහා `delivery/zip-verification.json`, ZIP contents සඳහා `delivery/*-contents.txt`, නිවැරදි changed files සඳහා `delivery/changed-files.txt` බලන්න. Local keys, private credentials, build/cache/node_modules/.git සහ original backups delivery ZIP වලට දමා නැහැ.
