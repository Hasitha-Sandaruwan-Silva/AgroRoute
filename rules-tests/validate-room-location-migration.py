"""Run the actual 6->7 SQL against SQLite and compare with Room's generated fresh schema.
Requires :app:assembleDebug first. Uses in-memory databases, never user app data.
"""
import pathlib
import re
import sqlite3

root = pathlib.Path(__file__).resolve().parent.parent
source = (root / "app/src/main/java/com/agroroute/core/database/DatabaseMigrations.kt").read_text(encoding="utf-8-sig")
block = source.split("val MIGRATION_6_7", 1)[1].split("val MIGRATION_1_2", 1)[0]
migration_sql = re.findall(r'database\.execSQL\("([^"]+)"\)', block)
assert len(migration_sql) == 2, "Expected two additive migration statements"
generated = root / "app/build/generated/ksp/debug/java/com/agroroute/core/database/AppDatabase_Impl.java"
room_text = generated.read_text(encoding="utf-8")
creates = re.findall(r'db\.execSQL\("(CREATE (?:TABLE|(?:UNIQUE )?INDEX)[^"\n]+)"\)', room_text)
creates = [sql for sql in creates if "room_master_table" not in sql]
assert len([sql for sql in creates if sql.startswith("CREATE TABLE")]) == 7

fresh = sqlite3.connect(":memory:")
legacy = sqlite3.connect(":memory:")
for sql in creates:
    fresh.execute(sql)
    old_sql = sql
    if sql.startswith("CREATE TABLE IF NOT EXISTS `bids`") or sql.startswith("CREATE TABLE IF NOT EXISTS `order_requests`"):
        old_sql = sql.replace(", `deliveryLocation` TEXT NOT NULL DEFAULT ''", "")
    legacy.execute(old_sql)

names = [row[0] for row in legacy.execute("SELECT name FROM sqlite_master WHERE type='table'")]
for name in names:
    columns = list(legacy.execute(f"PRAGMA table_info(`{name}`)"))
    values = []
    for _, col, sql_type, not_null, default, primary_key in columns:
        value = 5 if sql_type in ("INTEGER", "REAL") else f"legacy-{col}"
        if col == "pickupLocation": value = "Legacy farm address"
        if col == "deliveryLocation": value = "Existing warehouse\n[6.900000, 79.900000]"
        if not not_null and not primary_key: value = None
        values.append(value)
    legacy.execute(f"INSERT INTO `{name}` VALUES ({','.join('?' for _ in columns)})", values)

before = {name: list(legacy.execute(f"SELECT * FROM `{name}`")) for name in names}
legacy.execute("PRAGMA user_version=6")
with legacy:
    for sql in migration_sql: legacy.execute(sql)
    legacy.execute("PRAGMA user_version=7")
assert legacy.execute("PRAGMA user_version").fetchone()[0] == 7
for name in names:
    fresh_schema = list(fresh.execute(f"PRAGMA table_info(`{name}`)"))
    migrated_schema = list(legacy.execute(f"PRAGMA table_info(`{name}`)"))
    assert fresh_schema == migrated_schema, f"Room schema mismatch for {name}"
    after = list(legacy.execute(f"SELECT * FROM `{name}`"))
    if name in ("bids", "order_requests"):
        assert [row[:-1] for row in after] == before[name], f"Legacy values changed in {name}"
        assert all(row[-1] == "" for row in after), f"Missing legacy default for {name}"
        legacy.execute(f"UPDATE `{name}` SET deliveryLocation=?", ("New selected pin\n[7.000000, 80.000000]",))
    else:
        assert after == before[name], f"Rows changed in {name}"
    assert list(fresh.execute(f"PRAGMA index_list(`{name}`)")) == list(legacy.execute(f"PRAGMA index_list(`{name}`)"))
assert legacy.execute("PRAGMA integrity_check").fetchone()[0] == "ok"
print("PASS: actual 6->7 migration preserves rows in all 7 tables; adds only 2 delivery columns with empty legacy defaults")
print("PASS: migrated columns/defaults/indexes match Room-generated v7 schema; old order locations untouched; new delivery values writable")
