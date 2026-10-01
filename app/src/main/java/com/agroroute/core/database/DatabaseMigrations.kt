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
}