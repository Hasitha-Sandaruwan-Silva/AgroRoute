package com.agroroute

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.demo.DemoBrowser
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DemoBrowserTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AppDatabase
    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
        compose.setContent { AgroRouteTheme { DemoBrowser(db) } }
    }
    @After fun close() { db.close() }
    private fun awaitText(value: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(value,substring=true).fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun explicitLoadShowsCachedListingsWithoutSessionOrServerActions() {
        awaitText("0 profiles")
        compose.onNodeWithText("Load Demo Data").performClick()
        awaitText("6 profiles ? 10 listings ? 7 orders")
        compose.onNodeWithText("Tomato").assertExists()
        compose.onAllNodesWithText("Accept",substring=false).assertCountEquals(0)
        compose.onNodeWithText("Bids",substring=false).performClick()
        awaitText("Stock permits acceptance (read-only demo)")
    }
    @Test fun clearReturnsToEmptyLocalBrowser() {
        compose.onNodeWithText("Load Demo Data").performClick()
        awaitText("6 profiles ? 10 listings ? 7 orders")
        compose.onNodeWithText("Clear Demo Data").performClick()
        awaitText("Demo records cleared.")
        compose.onNodeWithText("0 profiles ? 0 listings ? 0 orders").assertExists()
    }
}
