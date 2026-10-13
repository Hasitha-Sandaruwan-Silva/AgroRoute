package com.agroroute.features.supplier.home

import com.agroroute.core.model.*
import org.junit.Assert.*
import org.junit.Test

class SupplierHomePresentationTest {
    @Test fun activeProduceUsesCurrentDeadlineStockAndHarvestState() {
        val active = HarvestListing(listingId = "active", quantity = 100.0, soldQuantity = 40.0, createdAt = 2)
        val expected = active.copy(listingId = "expected", harvestStatus = HarvestStatus.EXPECTED, availableQuantity = 0.0, createdAt = 3)
        val rows = listOf(active, expected, active.copy(listingId = "expired", expiresAt = 10),
            active.copy(listingId = "draft", isDraft = true), active.copy(listingId = "sold", availableQuantity = 0.0),
            active.copy(listingId = "closed", status = ListingStatus.CLOSED), active.copy(listingId = "failed", harvestStatus = HarvestStatus.FAILED))
        assertEquals(listOf("expected", "active"), homeActiveListings(rows, 10).map { it.listingId })
        assertEquals("60 kg available", homeQuantity(active))
    }
    @Test fun futureHarvestIsClearlyAnEstimate() {
        assertEquals("Est. 120 kg", homeQuantity(HarvestListing(harvestStatus = HarvestStatus.EXPECTED, estimatedQuantity = 120.0, availableQuantity = 0.0)))
    }
    @Test fun displayGroupsDoNotUseOrganicCategoryAsProduceType() {
        assertEquals("Fruits", supplierProduceGroup("Banana"))
        assertEquals("Grains", supplierProduceGroup("Paddy rice"))
        assertEquals("Spices", supplierProduceGroup("Cinnamon"))
        assertEquals("Dairy", supplierProduceGroup("Milk"))
        assertEquals("Vegetables", supplierProduceGroup("Tomato"))
    }
    @Test fun timeFormattingHandlesFutureAndOldListings() {
        assertEquals("Just now", homeRelativeTime(100, 0))
        assertEquals("3h ago", homeRelativeTime(0, 3 * 3_600_000L))
        assertEquals("2d ago", homeRelativeTime(0, 2 * 86_400_000L))
    }
    @Test fun fractionalTermsNeverRoundSmallAvailableStockToZero() {
        assertEquals("0.01", homeNumber(0.01))
        assertEquals("320.25", homeNumber(320.25))
        assertEquals("60.1", homeNumber(60.099999999999994))
        assertEquals("Unavailable", homeNumber(Double.NaN))
    }
}
