AgroRoute split delivery

Full project assembly
1. Extract others.zip; the extracted others/ folder is the Android/Firebase project root.
2. Extract src.zip. Copy its src/ directory to others/app/src/ (replace the old app/src/ directory).
3. The profile editor saves each user's phone to Firestore at users/{uid}.phone. Signed-in Farmers and Suppliers can view the other party's saved number in the marketplace, incoming bids, and order details. A number must first be entered in that user's Profile; the app cannot supply a number that the user has not entered.
4. Build from the others/ project root with ./gradlew :app:assembleDebug.

Firebase files
- firestore.rules is the rules file supplied by the project owner; it permits signed-in profile reads and lets each user update their own phone/profile fields without changing role or points.
- storage.rules includes owner-only profile avatar upload/update/delete rules.
- app/google-services.json is the Android plugin configuration path. Check that it belongs to the intended Firebase project/application before building against a live backend.
- firestore.rules.test.mjs includes a test that a user can update their own phone field. Run the Firebase Emulator rules tests before deploying rules.

Manual change bundle
The separate others-changed-files.zip contains only the changed/new project configuration and Firebase files, preserving their relative paths so they can be copied into an existing project root. Keep a backup before replacing files. Do not copy the directory name others/ itself as a nested project folder.
