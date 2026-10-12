package com.agroroute.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE bids ADD COLUMN requestedQuantity REAL")
        }
    }
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE bids ADD COLUMN deliveryLocation TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE order_requests ADD COLUMN deliveryLocation TEXT NOT NULL DEFAULT ''")
        }
    }
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

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add points column to user_profiles
            database.execSQL("ALTER TABLE user_profiles ADD COLUMN points INTEGER NOT NULL DEFAULT 0")

            // Create order_completion_records table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS order_completion_records (
                    orderId TEXT PRIMARY KEY NOT NULL,
                    farmerId TEXT NOT NULL,
                    supplierId TEXT NOT NULL,
                    pointsAwardedAt INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_completion_records_farmerId ON order_completion_records(farmerId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_order_completion_records_supplierId ON order_completion_records(supplierId)")

            // Create reviews table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS reviews (
                    reviewId TEXT PRIMARY KEY NOT NULL,
                    orderId TEXT NOT NULL,
                    reviewerId TEXT NOT NULL,
                    revieweeId TEXT NOT NULL,
                    rating INTEGER NOT NULL,
                    comment TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_orderId ON reviews(orderId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_reviewerId ON reviews(reviewerId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reviews_revieweeId ON reviews(revieweeId)")
        }
    }
}
