package com.agroroute.core.location

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SelectedLocationTest {
    @Test fun `confirmed coordinates survive a location text round trip under Sinhala or decimal comma locales`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val selected = SelectedLocation("Dummy farm, Kandy", 7.290572, 80.633726)
            assertEquals(selected, SelectedLocation.fromStoredText(selected.toStoredText()))
        } finally { Locale.setDefault(previous) }
    }
    @Test fun `old text only addresses remain untouched and malformed coordinates are rejected`() {
        assertNull(SelectedLocation.fromStoredText("Kandy"))
        assertNull(SelectedLocation.fromStoredText("Farm\n[91.0, 80.0]"))
        assertNull(SelectedLocation.fromStoredText("Farm\n[7.0, -181.0]"))
        assertNull(SelectedLocation.fromStoredText("Farm\n[NaN, 80]"))
    }
    @Test fun `coordinate only selection remains recoverable when geocoding is unavailable`() {
        val selected = SelectedLocation("", -7.5, -80.25)
        assertEquals(selected, SelectedLocation.fromStoredText(selected.toStoredText()))
    }
}
