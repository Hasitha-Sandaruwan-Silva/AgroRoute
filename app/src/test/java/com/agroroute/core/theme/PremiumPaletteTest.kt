package com.agroroute.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.agroroute.core.components.profileLocationLabel
import org.junit.Assert.*
import org.junit.Test

class PremiumPaletteTest {
    private fun contrast(a: Color, b: Color): Float {
        val first = a.luminance(); val second = b.luminance()
        return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
    }

    @Test fun featureAndActionTextHasAccessibleContrast() {
        listOf(ForestDeep, ForestMid, ForestSoft, SoilBrown, ClaySurface, FreshGreen, NightForest, CharcoalForest)
            .forEach { assertTrue("Feature contrast $it", contrast(CreamInk, it) >= 4.5f) }
        assertTrue(contrast(PureWhite, GreenEmerald) >= 4.5f)
        assertTrue(contrast(TextDarkGreen, CreamCard) >= 4.5f)
        assertTrue(contrast(DarkTextPrimary, DarkSurface) >= 4.5f)
        assertTrue(contrast(HarvestGoldDeep, HarvestCream) >= 4.5f)
    }

    @Test fun storedLocationDisplaysAddressWithoutCoordinates() {
        assertEquals("Kandy", profileLocationLabel("Kandy\n[7.290600, 80.633700]"))
        assertEquals("Saved map location", profileLocationLabel("[7.290600, 80.633700]"))
        assertEquals("Galle", profileLocationLabel("Galle"))
        assertEquals("Add your location", profileLocationLabel(null))
    }
}
