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

/** Light palette — light green surfaces, primary green actions and deep teal text. */
val LightExtendedColors = AgroExtendedColors(
    pageBg = SoftMintBg,
    brandGreen = GreenEmerald,
    brandGreenDark = GreenDeep,
    brandGreenMid = GreenEmerald,
    brandLeaf = GreenEmerald,
    headingGreen = GreenDeep,
    inkBlack = GreenDeep,
    bodyCharcoal = TextSecondary,
    mutedGreen = TextSecondary,
    textSecondary = TextSecondary,
    lineGray = OutlineSoft,
    borderGray = OutlineGreen,
    softGreen = SoftMintBg,
    softAmber = MintContainer,
    goldAccent = GreenEmerald,
    goldSoft = MintContainer,
    goldText = GreenDeep,
    tabInactive = TextSecondary,
    pagerDot = MintContainer,
    quoteBg = SoftMintBg,
    orangeBadge = GreenEmerald,
    farmerAccent = GreenEmerald,
    farmerAccentContainer = MintContainer,
    farmerOnAccentContainer = GreenDeep,
    supplierAccent = GreenEmerald,
    supplierAccentContainer = MintContainer,
    supplierOnAccentContainer = GreenDeep,
    tealAccent = GreenEmerald,
    tealAccentContainer = MintContainer,
    tealOnAccentContainer = GreenDeep,
    successContainer = GreenEmerald,
    successOnContainer = PureWhite,
    warningContainer = MintContainer,
    warningOnContainer = GreenDeep,
    neutralContainer = SoftMintBg,
    neutralOnContainer = TextSecondary,
    cardSurface = CreamCard,
    cardBorder = CreamBorder,
    divider = OutlineSoft,
    iconContainer = MintContainer,
    iconOnContainer = GreenDeep,
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

/** Dark palette — deep teal surfaces with light-green text and accents. */
val DarkExtendedColors = AgroExtendedColors(
    pageBg = DarkBg,
    brandGreen = MintAccent,
    brandGreenDark = MintAccent,
    brandGreenMid = GreenEmerald,
    brandLeaf = GreenEmerald,
    headingGreen = DarkTextPrimary,
    inkBlack = Color(0xFFEAF9E7),
    bodyCharcoal = DarkTextSecondary,
    mutedGreen = DarkTextSecondary,
    textSecondary = DarkTextSecondary,
    lineGray = DarkOutline,
    borderGray = DarkOutline,
    softGreen = DarkSurface2,
    softAmber = DarkSurface2,
    goldAccent = MintAccent,
    goldSoft = DarkSurface2,
    goldText = MintAccent,
    tabInactive = DarkTextSecondary,
    pagerDot = DarkOutline,
    quoteBg = DarkSurface2,
    orangeBadge = GreenEmerald,
    farmerAccent = MintAccent,
    farmerAccentContainer = DarkSurface2,
    farmerOnAccentContainer = MintAccent,
    supplierAccent = MintAccent,
    supplierAccentContainer = DarkSurface2,
    supplierOnAccentContainer = MintAccent,
    tealAccent = MintAccent,
    tealAccentContainer = DarkSurface2,
    tealOnAccentContainer = MintAccent,
    successContainer = Color(0xFF246A4C),
    successOnContainer = Color(0xFFEAF9E7),
    warningContainer = DarkSurface2,
    warningOnContainer = MintAccent,
    neutralContainer = DarkSurface2,
    neutralOnContainer = DarkTextSecondary,
    cardSurface = DarkCard,
    cardBorder = DarkCardBorder,
    divider = Color(0xFF22695C),
    iconContainer = DarkSurface2,
    iconOnContainer = MintAccent,
    accentIconContainer = DarkSurface2,
    accentIconOnContainer = MintAccent,
    heroStart = ForestDeep,
    heroMid = ForestMid,
    heroEnd = ForestSoft,
    heroContent = Color(0xFFEAF9E7),
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
