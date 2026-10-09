# Dummy sample data — test project / emulator සඳහා පමණයි

`firestore.json` හි `documents` array එකේ සෑම item එකකම `path` යනු නිවැරදි collection/document path එකයි; `data` යනු එම document එකේ fields වේ. `documents`, `path`, `data` යන wrapper fields Firestore document එකට දාන්න එපා.

මෙහි real users, passwords හෝ credentials නැහැ. `.invalid` emails සහ `+9400000000x` numbers dummy values වේ; ඒවාට calls කරන්න එපා. Photo URLs හිස්ව තිබෙන්නේ fake upload URLs හිතෙන් නිර්මාණය නොකිරීමටයි.

## Local emulator

1. Node dependencies: `cd rules-tests` සහ `npm ci`.
2. Project root එකේ `firebase emulators:start --only firestore --project demo-agroroute-sample --config firebase.rules-test.json` ධාවනය කරන්න.
3. වෙනම PowerShell window එකක project root තුළ:

```powershell
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8082'
node rules-tests/seed-sample.mjs
```

Script එක loopback emulator host එකක් පමණක් පිළිගෙන `demo-agroroute-sample` project එකට documents 8ක් seed කර profile/phone/location reads සහ points/status write protection validate කරයි. Emulator එකේ අනෙක් documents delete කරන්නේ නැහැ. නැවත ධාවනය කිරීමෙන් මේ dummy IDs පමණක් replace වේ.

Firestore fixture users සෑදීමෙන් Firebase Auth accounts සෑදෙන්නේ නැහැ. Automated validation එක emulator test identities භාවිත කරයි. App login පරීක්ෂාවට වෙනම test Firebase Auth accounts හෝ Auth Emulator users සෑදිය යුතුයි; එවිට `userId`, farmer/supplier IDs සහ document paths එම Auth UIDs වලට එකට ගැළපෙන්න වෙනස් කරන්න. App එක දැනට live config එක භාවිත කරයි; මේ seed script එක එයට සම්බන්ධ නැහැ.

## Firebase Console හරහා වෙනම test project එකකට දමන ක්‍රමය

1. ඔබගේ **වෙනම test project එක** තෝරන්න. Production project එකට මෙම fixtures දාන්න එපා.
2. `users/demo-farmer` සහ `users/demo-supplier` සාදන්න.
3. `listings/demo-fixed`, `listings/demo-bidding` සාදන්න.
4. `bids/demo-bid` සාදන්න.
5. `order_requests/demo-order`, `order_requests/demo-request` සාදන්න.
6. `orders/demo-order` සාදන්න.
7. JSON `data` යටතේ ඇති fields එකින් එක දාන්න: numbers → number, booleans → boolean, null → null, imageUrls → array, අනෙක් text → string. Time fields app එකේ **epoch milliseconds numbers** වේ; Firestore Timestamp බවට වෙනස් කරන්න එපා.

`demo-order` යනු accepted direct request එකක් සඳහා `AWAITING_PICKUP` order එකකි. Listing stock: quantity 30, sold 10, available 20. Pending request quantity 5කි. Bidding listing එකේ pending bid එකක් පවතී. Initial user points 0 වේ. Completion receipts/reviews හි fake data දාන්නේ නැහැ; delivered workflow එකෙන් valid receipt සහ points සෑදේ.

Console JSON wrapper file එක සෘජුව import කළ නොහැක; ඉහත path/type mapping භාවිත කර fields අතින් දාන්න, නැතහොත් local emulator script එක භාවිත කරන්න.
