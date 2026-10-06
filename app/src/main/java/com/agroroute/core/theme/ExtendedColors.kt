package com.agroroute.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand colours that screens and shells use directly (outside the Material
 * [androidx.compose.material3.ColorScheme]).
 *
 * Values follow the "Fresh Harvest" palette: forest green for the farmer feel,
 * harvest amber for the supplier feel, plus soft status containers that stay
 * readable in both light and dark themes. Provided via
 * [LocalAgroExtendedColors] from [AgroRouteTheme].
 */
data class AgroExtendedColors(
    val pageBg: Color,
    val brandGreen: Color,
    val brandGreenDark: Color,
    val brandGreenMid: Color,
    val brandLeaf: Color,
    val headingGreen: Color,
    val inkBlack: Color,
    val bodyCharcoal: Color,
    val mutedGreen: Color,
    val textSecondary: Color,
    val lineGray: Color,
    val borderGray: Color,
    val softGreen: Color,
    val softAmber: Color,
    val goldAccent: Color,
    val goldSoft: Color,
    val goldText: Color,
    val tabInactive: Color,
    val pagerDot: Color,
    val quoteBg: Color,
    val orangeBadge: Color,
    // Role accents — same design language, slightly different accent usage.
    val farmerAccent: Color,
    val farmerAccentContainer: Color,
    val farmerOnAccentContainer: Color,
    val supplierAccent: Color,
    val supplierAccentContainer: Color,
    val supplierOnAccentContainer: Color,
    val tealAccent: Color,
    val tealAccentContainer: Color,
    val tealOnAccentContainer: Color,
    // Soft status containers shared by chips, banners and timelines.
    val successContainer: Color,
    val successOnContainer: Color,
    val warningContainer: Color,
    val warningOnContainer: Color,
    val neutralContainer: Color,
    val neutralOnContainer: Color
)

/** Light palette — fresh cream surfaces with forest and harvest accents. */
val LightExtendedColors = AgroExtendedColors(
    pageBg = SoftMintBg,
    brandGreen = GreenEmerald,
    brandGreenDark = Color(0xFF1C4A35),
    brandGreenMid = GreenLight,
    brandLeaf = Color(0xFF48A06B),
    headingGreen = Color(0xFF123723),
    inkBlack = Color(0xFF1A2B20),
    bodyCharcoal = Color(0xFF44544A),
    mutedGreen = Color(0xFF5C6B5F),
    textSecondary = Color(0xFF5E6E63),
    lineGray = Color(0xFFE3E9DE),
    borderGray = Color(0xFFDCE4D7),
    softGreen = Color(0xFFE3F1E6),
    softAmber = Color(0xFFFDF3D7),
    goldAccent = HarvestGold,
    goldSoft = HarvestCream,
    goldText = AmberDeepText,
    tabInactive = Color(0xFF93A396),
    pagerDot = Color(0xFFC9D6CB),
    quoteBg = Color(0xFFF3F7F1),
    orangeBadge = Color(0xFFE07B39),
    farmerAccent = GreenEmerald,
    farmerAccentContainer = Color(0xFFD5EBDC),
    farmerOnAccentContainer = Color(0xFF0B3620),
    supplierAccent = HarvestGoldDeep,
    supplierAccentContainer = HarvestCream,
    supplierOnAccentContainer = AmberDeepText,
    tealAccent = Color(0xFF1E7A6F),
    tealAccentContainer = Color(0xFFD3EDE9),
    tealOnAccentContainer = Color(0xFF0F4A43),
    successContainer = Color(0xFFD9EEDD),
    successOnContainer = Color(0xFF1E5C3A),
    warningContainer = AmberSoftBg,
    warningOnContainer = AmberDeepText,
    neutralContainer = NeutralSoftBg,
    neutralOnContainer = NeutralDeepText
)

/** Dark palette — rich green-black surfaces with mint and amber accents. */
val DarkExtendedColors = AgroExtendedColors(
    pageBg = DarkBg,
    brandGreen = MintAccent,
    brandGreenDark = Color(0xFFA9DDBB),
    brandGreenMid = GreenLight,
    brandLeaf = Color(0xFF7FCB97),
    headingGreen = DarkTextPrimary,
    inkBlack = Color(0xFFF0F6F1),
    bodyCharcoal = DarkTextSecondary,
    mutedGreen = Color(0xFF9DAD9F),
    textSecondary = Color(0xFFA7B7AB),
    lineGray = Color(0xFF24352B),
    borderGray = Color(0xFF2A3D31),
    softGreen = Color(0xFF1E3226),
    softAmber = Color(0xFF3A3010),
    goldAccent = Color(0xFFEBC26E),
    goldSoft = Color(0xFF3A3010),
    goldText = Color(0xFFEBC26E),
    tabInactive = Color(0xFF8FA296),
    pagerDot = Color(0xFF3A4A3E),
    quoteBg = DarkSurface2,
    orangeBadge = Color(0xFFF0A468),
    farmerAccent = MintAccent,
    farmerAccentContainer = Color(0xFF1E4631),
    farmerOnAccentContainer = Color(0xFFB7EFC9),
    supplierAccent = Color(0xFFEBC26E),
    supplierAccentContainer = Color(0xFF544310),
    supplierOnAccentContainer = Color(0xFFF4DFA6),
    tealAccent = Color(0xFF6FD1C5),
    tealAccentContainer = Color(0xFF12403C),
    tealOnAccentContainer = Color(0xFFB2ECE3),
    successContainer = Color(0xFF1E3A2A),
    successOnContainer = Color(0xFF9FDCAF),
    warningContainer = Color(0xFF3A3010),
    warningOnContainer = Color(0xFFEFCF85),
    neutralContainer = Color(0xFF232E27),
    neutralOnContainer = DarkTextSecondary
)

val LocalAgroExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Syntax probe: proves @Composable property getters compile in this module. */
private val ColorSchemeProbe: Color
    @Composable get() = LocalAgroExtendedColors.current.pageBg
