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
    val neutralOnContainer: Color,
    // ── Structural tokens consumed by the shared widgets ────────────────────
    // Card surfaces and hairlines.
    val cardSurface: Color = PureWhite,
    val cardBorder: Color = CreamBorder,
    val divider: Color = OutlineSoft,
    // Tinted rounded containers that hold icons — green-led and amber-led.
    val iconContainer: Color = MintContainer,
    val iconOnContainer: Color = GreenDeep,
    val accentIconContainer: Color = HarvestCream,
    val accentIconOnContainer: Color = AmberDeepText,
    // Hero / gradient surfaces.
    val heroStart: Color = ForestDeep,
    val heroMid: Color = ForestMid,
    val heroEnd: Color = ForestSoft,
    val heroContent: Color = PureWhite,
    val heroContentMuted: Color = Color(0xCCFFFFFF),
    val heroChip: Color = Color(0x29FFFFFF),
    // Text fields.
    val inputContainer: Color = InputFillLight,
    val inputBorder: Color = InputLineLight,
    val inputFocusedBorder: Color = GreenEmerald,
    // Loading and disabled states.
    val shimmerBase: Color = ShimmerBaseL,
    val shimmerHighlight: Color = ShimmerHighL,
    val disabledContainer: Color = DisabledBgL,
    val disabledContent: Color = DisabledFgL,
    // Danger container for cancellations and failures.
    val dangerContainer: Color = ErrorSoftBg,
    val dangerOnContainer: Color = ErrorDeepText
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
    neutralOnContainer = NeutralDeepText,
    cardSurface = CreamCard,
    cardBorder = CreamBorder,
    divider = OutlineSoft,
    iconContainer = Color(0xFFDCEEE1),
    iconOnContainer = Color(0xFF1B5138),
    accentIconContainer = HarvestCream,
    accentIconOnContainer = AmberDeepText,
    heroStart = ForestDeep,
    heroMid = ForestMid,
    heroEnd = ForestSoft,
    heroContent = PureWhite,
    heroContentMuted = Color(0xD9FFFFFF),
    heroChip = Color(0x24FFFFFF),
    inputContainer = InputFillLight,
    inputBorder = InputLineLight,
    inputFocusedBorder = GreenEmerald,
    shimmerBase = ShimmerBaseL,
    shimmerHighlight = ShimmerHighL,
    disabledContainer = DisabledBgL,
    disabledContent = DisabledFgL,
    dangerContainer = ErrorSoftBg,
    dangerOnContainer = ErrorDeepText
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
    neutralOnContainer = DarkTextSecondary,
    cardSurface = DarkCard,
    cardBorder = DarkCardBorder,
    divider = Color(0xFF24352B),
    iconContainer = Color(0xFF1F4130),
    iconOnContainer = Color(0xFFA9E3BD),
    accentIconContainer = Color(0xFF4A3A11),
    accentIconOnContainer = Color(0xFFF0CE86),
    heroStart = Color(0xFF0B2A1C),
    heroMid = Color(0xFF17402D),
    heroEnd = Color(0xFF24583F),
    heroContent = Color(0xFFF2F8F3),
    heroContentMuted = Color(0xCCDCE7DE),
    heroChip = Color(0x2EFFFFFF),
    inputContainer = InputFillDark,
    inputBorder = InputLineDark,
    inputFocusedBorder = MintAccent,
    shimmerBase = ShimmerBaseD,
    shimmerHighlight = ShimmerHighD,
    disabledContainer = DisabledBgD,
    disabledContent = DisabledFgD,
    dangerContainer = Color(0xFF43201E),
    dangerOnContainer = Color(0xFFFFCFC9)
)

val LocalAgroExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Syntax probe: proves @Composable property getters compile in this module. */
private val ColorSchemeProbe: Color
    @Composable get() = LocalAgroExtendedColors.current.pageBg
