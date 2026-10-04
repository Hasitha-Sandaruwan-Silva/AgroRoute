package com.agroroute.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Brand colours that screens and shells use directly (outside the Material
 * [androidx.compose.material3.ColorScheme]).
 *
 * Every LIGHT value is byte-identical to the literal it replaces in the screens,
 * so the light theme looks exactly as before. Each entry carries a DARK
 * counterpart (mostly the existing dark palette in [Color.kt] plus a few
 * dark-only additions) so text and surfaces stay readable in dark mode.
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
    val orangeBadge: Color
)

/** Light palette — mirrors the historical hard-coded values exactly. */
val LightExtendedColors = AgroExtendedColors(
    pageBg = Color(0xFFF6FAF6),
    brandGreen = Color(0xFF2E7D32),
    brandGreenDark = Color(0xFF1B5E20),
    brandGreenMid = Color(0xFF66BB6A),
    brandLeaf = Color(0xFF43A047),
    headingGreen = Color(0xFF14532D),
    inkBlack = Color(0xFF1B1B1B),
    bodyCharcoal = Color(0xFF4B5563),
    mutedGreen = Color(0xFF5F6B5F),
    textSecondary = Color(0xFF64748B),
    lineGray = Color(0xFFE5E7EB),
    borderGray = Color(0xFFE2E8F0),
    softGreen = Color(0xFFE6F4EA),
    softAmber = Color(0xFFFFF8E1),
    goldAccent = Color(0xFFF59E0B),
    goldSoft = Color(0xFFFEF3C7),
    goldText = Color(0xFFB45309),
    tabInactive = Color(0xFF9CA3AF),
    pagerDot = Color(0xFFCBD5E1),
    quoteBg = Color(0xFFF8FAFC),
    orangeBadge = Color(0xFFF97316)
)

/** Dark palette — built from the existing dark theme colours (WCAG AA text contrast). */
val DarkExtendedColors = AgroExtendedColors(
    pageBg = DarkBg,
    brandGreen = MintAccent,
    brandGreenDark = MintAccent,
    brandGreenMid = MintAccent,
    brandLeaf = MintAccent,
    headingGreen = DarkTextPrimary,
    inkBlack = DarkTextPrimary,
    bodyCharcoal = DarkTextSecondary,
    mutedGreen = DarkTextSecondary,
    textSecondary = DarkTextSecondary,
    lineGray = DarkOutline,
    borderGray = DarkOutline,
    softGreen = DarkSurface2,
    softAmber = Color(0xFF3A321B),
    goldAccent = AccentAmber,
    goldSoft = Color(0xFF3A321B),
    goldText = AccentAmber,
    tabInactive = DarkTextSecondary,
    pagerDot = DarkOutline,
    quoteBg = DarkSurface2,
    orangeBadge = Color(0xFFFFA46B)
)

val LocalAgroExtendedColors = compositionLocalOf { LightExtendedColors }

/** Syntax probe: proves @Composable property getters compile in this module. */
private val ColorSchemeProbe: Color
    @Composable get() = LocalAgroExtendedColors.current.pageBg
