package com.agroroute.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN category TEXT")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN expectedHarvestAt INTEGER")
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN qualityGrade TEXT")
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN availableQuantity REAL")
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN soldQuantity REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS order_requests (
                    requestId TEXT PRIMARY KEY NOT NULL,
                    listingId TEXT NOT NULL,
                    farmerId TEXT NOT NULL,
                    farmerName TEXT NOT NULL,
                    supplierId TEXT NOT NULL,
                    supplierName TEXT NOT NULL,
                    cropName TEXT NOT NULL,
                    requestedQuantity REAL NOT NULL,
                    quantityUnit TEXT NOT NULL,
                    listedPrice REAL NOT NULL,
                    status TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_requests_listingId ON order_requests(listingId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_requests_supplierId ON order_requests(supplierId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_requests_farmerId ON order_requests(farmerId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_requests_status ON order_requests(status)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN harvestStatus TEXT NOT NULL DEFAULT 'READY'")
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN estimatedQuantity REAL")
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN harvestFailureReason TEXT")
            database.execSQL("ALTER TABLE harvest_listings ADD COLUMN harvestUpdatedAt INTEGER")
            database.execSQL("ALTER TABLE order_requests ADD COLUMN isPreHarvest INTEGER NOT NULL DEFAULT 0")
        }
    }
}