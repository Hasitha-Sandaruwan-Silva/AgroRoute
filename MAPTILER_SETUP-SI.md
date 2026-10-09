# MapLibre + MapTiler — AgroRoute setup සහ verification

## Configuration

මේ project එක දැන් MapLibre Android SDK `11.8.0` භාවිත කරයි. Kotlin **2.0.21**, Gradle **8.7**, AGP **8.6.0** වෙනස් කර නැහැ. Google Maps SDK/Compose dependency සහ Google Maps manifest API-key metadata ඉවත් කර ඇත. Firebase/Google sign-in සඳහා අවශ්‍ය වෙනත් Google libraries ඉවත් කර නැහැ.

`app/build.gradle.kts` විසින් project root හි `local.properties` කියවා `BuildConfig.MAPTILER_API_KEY` generate කරයි. Actual key එක source code, example file, report හෝ logs තුළ දමා නැහැ. Key එක හිස් නම් map SDK initialise නොකර setup message එක පෙන්වයි; search/current-location fallback තවම තිබෙනවා.

පරීක්ෂා කළ මොහොතේ සැබෑ `local.properties` තුළ `MAPTILER_API_KEY` නොතිබුණා. `local.properties.example` තුළ තිබූ key වැනි value එක ඉවත් කර හිස් template එකක් තබා ඇත. එම පැරණි value එක සැබෑ key එකක් නම් MapTiler Console එකෙන් එය revoke/rotate කර නව key එක local file එකට පමණක් දාන්න.

## ඔබ කළ යුතු පියවර

1. MapTiler Cloud → API keys තුළ ඔබගේ app එක සඳහා key එකක් තෝරන්න හෝ සාදන්න.
2. Native app restriction සඳහා **Allowed User-Agent header** value එක `com.agroroute/` ලෙස සකසන්න. මේ SDK version එකේ default User-Agent එක application package/version එක ඇතුළත් කරයි. Android app එක සඳහා website-only Allowed HTTP origins restriction එකක් යෙදීමෙන් requests deny විය හැක. [MapTiler key instructions](https://docs.maptiler.com/guides/credentials/api-key/), [MapLibre 11.8.0 User-Agent identifier](https://github.com/maplibre/maplibre-native/blob/android-v11.8.0/platform/android/MapLibreAndroid/src/main/java/org/maplibre/android/http/HttpIdentifier.java).
3. Android Studio තුළ project root හි **local.properties** විවෘත කරන්න. `local.properties.example` වෙනස් කිරීමෙන් app එකට key එක නොලැබේ.
4. දැනට ඇති `sdk.dir=...` line එක තබාගෙන පහත line එකට ඔබගේ key එක දාන්න. Quotes, `YOUR_NEW_KEY=` හෝ තවත් `=` prefix එකක් එක් කරන්න එපා.

   ```properties
   MAPTILER_API_KEY=YOUR_OWN_KEY
   ```

5. Android Studio → Settings → Build Tools → Gradle → Gradle JDK compatible **17/21** ලෙස තෝරන්න. මෙහි build එක Android Studio JBR21 භාවිත කළා.
6. **Sync Project with Gradle Files** කරන්න. ඉන්පසු **Build / Run** කරන්න. Key වෙනස් කළ පසු rebuild අවශ්‍යයි; runtime එක local.properties කියවන්නේ නැහැ.
7. Internet සහ device location enable කර Profile/Edit profile, Create/Edit listing හෝ Supplier order delivery location picker එක විවෘත කරන්න.
8. Map tiles load වී ඇති බව බලන්න. Pan/zoom කරන්න; map එක tap කර green pin එක එම ස්ථානයට මාරු වෙනවාද සහ coordinates/address update වෙනවාද බලන්න.
9. Floating current-location button එක ඔබා permission ලබා දෙන්න. Camera එක GPS/network fix එකට focus වී blue dot එක පෙනෙනවාද බලන්න. Approximate permission එකත් supported; device/provider අනුව accuracy වෙනස් වේ.
10. Permission deny කර නැවත picker එක භාවිත කරන්න. Map tap/search/manual address සහ confirm වැඩ කළ යුතුයි. GPS position එක ලබාගැනීමට අවශ්‍ය විට පමණක් permission ඉල්ලයි; foreground location permissions පමණක් භාවිත කරයි.
11. Search result එකක් තෝරන්න. Geocoder result නැතිවුණොත් map pin එක තෝරා selected-address field එක manually පුරවන්න. **Use this location** එබීමෙන් existing `Address\n[latitude, longitude]` string එක form callback එකට යයි. Profile/listing සඳහා පසුව existing Save/Publish action එකත් කරන්න.
12. Back button / Android Back භාවිතයෙන් dialog එක cancel කර existing form location එක නොවෙනස්ව තිබෙනවාද බලන්න. Confirm කර නැවත picker එක විවෘත කළ විට saved pin/address නිවැරදිද බලන්න.

## UI සහ architecture

Existing `LocationPickerDialog` එකම AndroidView තුළ native MapView එක host කරයි; duplicate screens/classes නැහැ. Map texture rendering භාවිත කරන නිසා Compose floating controls සහ rounded panel නිවැරදිව map එක උඩ පෙනෙයි. Lifecycle start/resume/pause/stop/destroy සහ memory callbacks handle කර native resources release කරයි. Green selected marker සහ blue current-position dot native map layers වන නිසා map pan/zoom සමඟ coordinates නිවැරදිව පවත්වා ගනී. Pan කිරීමෙන් pin එක වෙනස් නොවේ; tap, search result හෝ current-location action එකෙන් location select කරයි.

MapTiler Streets style/tiles HTTPS හරහා ලබාගනී. INTERNET, ACCESS_NETWORK_STATE, ACCESS_COARSE_LOCATION සහ ACCESS_FINE_LOCATION permissions Manifest එකේ ඇත. Map attribution controls පෙනෙන ලෙස තබා ඇත. Cleartext traffic enable කර නැහැ.

Existing `LocationServices` Android Geocoder/search/reverse-geocoding සහ permission/provider/timeouts පවත්වා ඇත. Existing `SelectedLocation` codec, callers, repositories සහ Room schema වෙනස් කර නැහැ. Selected coordinates/address configuration recreation සඳහා rememberSaveable භාවිත කරයි. Coordinate එකක් තෝරා නොමැති විට confirm disabled වේ.

## Key ආරක්ෂාව

- `local.properties` .gitignore තුළ ඇත; Git tracked files තුළ නොමැත. Generated BuildConfig/APK build outputs ද ignored වේ.
- MapLibre SDK logs සහ HTTP request URL logging අක්‍රිය කර ඇත. UI errors raw exceptions/URLs පෙන්වන්නේ නැහැ.
- Client API key එක APK තුළ භාවිත වන නිසා එය server secret එකක් ලෙස සැඟවිය නොහැක. MapTiler native User-Agent restriction සහ account usage limits/monitoring භාවිත කරන්න. User-Agent restriction එක spoof කළ හැකි නිසා එය user authentication එකක් නොවේ.
- Google Maps key/SHA fingerprint/Maps SDK enable කිරීම දැන් අවශ්‍ය නැහැ. Local file එකේ පැරණි MAPS_API_KEY line තිබේ නම් එය ඉවත් කළ හැක; build එක එය කියවන්නේ නැහැ.

## පරීක්ෂණ සහ සීමාව

Gradle dependency resolution, Kotlin/Java compile, debug APK assembly සහ existing Android unit tests ධාවනය කර ඇත. අවසන් ප්‍රතිඵල `delivery/maplibre-final-build.log` සහ `delivery/maplibre-verification.json` තුළ ඇත. Room/schema/backend/location-saving tests ඒ suite එක තුළ ඇතුළත්ය.

Actual local MapTiler key නොමැති නිසා MapTiler authorization, tiles rendering සහ GPS/permission/tap UI flow device එකේ end-to-end තහවුරු කර නැහැ. ඉහත steps අනුව key දමා device verification කරන්න. Google Map/පැරණි OpenStreetMap screenshots මේ implementation එකේ evidence ලෙස භාවිත කර නැහැ.

## මේ migration එකේ වෙනස් කළ project files

| File | කළ වෙනස |
| --- | --- |
| `app/build.gradle.kts` | Google Maps Compose dependency ඉවත් කර MapLibre Android 11.8.0 එක් කළා; local.properties key එක escaped BuildConfig String එකක් ලෙස generate කරයි. |
| `app/src/main/AndroidManifest.xml` | Google Maps API-key metadata ඉවත් කළා; තිබූ Internet/network/fine/coarse permissions පවත්වා ඇත. |
| `app/src/main/java/com/agroroute/core/components/LocationPickerDialog.kt` | Shared picker එක native MapLibre වෙත මාරු කළා; tap-selected green pin, blue fix dot, pan/zoom, floating buttons, rounded panel, fixed confirm button, search/manual fallback සහ lifecycle/resource cleanup එක් කළා. |
| `local.properties.example` | Google Maps instructions සහ key වැනි value ඉවත් කර blank MAPTILER_API_KEY template එකක් කළා. |
| `MAPTILER_SETUP-SI.md` | නව setup, security, changed-file list සහ exact device-verification steps. |
| `GOOGLE_MAPS_SETUP-SI.md` | දැන් භාවිත නොවන Google Maps setup document එක ඉවත් කළා. |
| `DELIVERY_REPORT-SI.md` | Current provider, setup links සහ validation ප්‍රතිඵල MapLibre වෙත update කළා. |
| `INTEGRATION_README.txt` | Provider/setup/delivery instructions MapTiler වෙත update කළා. |

`.gitignore` වෙනස් කිරීමට අවශ්‍ය නොවුණා: local.properties දැනටමත් ignored වේ. LocationServices.kt, SelectedLocation.kt, profile/listing/order callers, repositories සහ Room schema මේ migration එකෙන් වෙනස් නොකළා. Gradle versions catalog/root build settings ද වෙනස් කර නැහැ.

Generated build APK: `app/build/outputs/apk/debug/app-debug.apk`. Migration verification logs/JSON සහ පැරණි picker/config backup `delivery/` යටතේ ඇත; ඒවා Git ignored වේ. Delivery ZIP තුන updated source/config වලින් නැවත generate කර entry paths සහ byte hashes verify කර ඇත.
