AgroRoute split delivery — final provider: MapLibre + MapTiler

See ORDER_LOCATION_FLOW-SI.md for order-specific pickup/delivery snapshots, Room 6->7 migration, updated rules and manual checks.
See MAPTILER_SETUP-SI.md for the current map migration, key configuration and device checks.
See DELIVERY_REPORT-SI.md for the earlier profile/points/notification audit and delivery instructions.

src.zip contains src/...; copy that directory into app/src/.
others.zip contains others/...; others/ is the project root for full assembly.
others-changed-files.zip contains changed/new non-Android-source files with project-relative paths.
local.properties and actual MapTiler keys are excluded from the archives.

The existing completeOrder callable and updated firestore.rules still require your test backend setup. This map migration does not change or deploy backend logic.
Profile photos keep Firebase Storage unless CloudinaryConfig.PROFILE_UNSIGNED_PRESET is configured from your own Console.
Sample data and emulator-only seed instructions are in sample-data/README-SI.md.
