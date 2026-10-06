package com.agroroute.core.theme

import androidx.compose.ui.graphics.Color

/**
 * "Fresh Harvest" palette — deep forest green primary, warm harvest amber
 * accent, soft cream surfaces in light mode and rich green-black surfaces in
 * dark mode. Earthy leaf-lime and soil tones fill out the tertiary range.
 */

// ── Primary (deep forest green) ──────────────────────────────────────────────
val GreenDeep      = Color(0xFF0F3D26)  // Deepest forest — headings, container text
val GreenEmerald   = Color(0xFF2D6A4F)  // Primary forest green
val GreenPrimary   = Color(0xFF2D6A4F)  // Primary forest green
val GreenLight     = Color(0xFF6FBF8E)  // Soft leaf green

// ── Secondary (harvest amber / gold) ─────────────────────────────────────────
val FreshGreen     = Color(0xFF55A273)  // Secondary green variant

// ── Tonal / Container ────────────────────────────────────────────────────────
val MintContainer  = Color(0xFFD8ECDC)  // Soft sage container
val SoftMintBg     = Color(0xFFFAF8F1)  // Warm cream page background

// ── Forest identity (hero surfaces, headers, snackbars) ─────────────────────
val ForestDeep     = Color(0xFF123B28)  // Hero / header background
val ForestMid      = Color(0xFF245841)  // Hero card surface
val ForestSoft     = Color(0xFF357354)  // Hero card highlight
val LeafAccent     = Color(0xFFA8CE4E)  // Fresh leaf-lime accent on dark green
val EarthAmber     = Color(0xFFE2A73C)  // Warm earthy accent

// ── Harvest amber family ────────────────────────────────────────────────────
val HarvestGold    = Color(0xFFE9A93E)  // Harvest gold accent
val HarvestGoldDeep = Color(0xFF8F6A1E) // Deep amber for light-mode text/icons
val HarvestCream   = Color(0xFFF8E9C4)  // Soft amber container

// ── Earthy tertiary (leaf lime / soil) ──────────────────────────────────────
val OliveLeaf      = Color(0xFF617535)  // Olive-lime tertiary
val SoilBrown      = Color(0xFF7A5B3F)  // Soil brown highlight

// ── Soft status containers (light, readable text pairs) ─────────────────────
val AmberSoftBg    = Color(0xFFFBEFD1)
val AmberDeepText  = Color(0xFF6E4E0E)
val RedSoftBg      = Color(0xFFFCE7E3)
val RedDeepText    = Color(0xFFA02519)
val NeutralSoftBg  = Color(0xFFEDF0E8)
val NeutralDeepText = Color(0xFF4C574C)

// ── Surface / Cards ──────────────────────────────────────────────────────────
val PureWhite      = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFE9EEE3)

// ── Text ─────────────────────────────────────────────────────────────────────
val TextDarkGreen  = Color(0xFF152B1F)
val TextSecondary  = Color(0xFF47584C)

// ── Outline / Dividers ───────────────────────────────────────────────────────
val OutlineGreen   = Color(0xFFC7D4C4)
val OutlineSoft    = Color(0xFFDFE8DC)

// ── Accent (highlights only) ─────────────────────────────────────────────────
val AccentAmber    = Color(0xFFE9A93E)

// ── Error ────────────────────────────────────────────────────────────────────
val ErrorRed       = Color(0xFFBA1A1A)
val ErrorRedDark   = Color(0xFFFFB4AB)
val ErrorSoftBg    = Color(0xFFFCE7E3)
val ErrorDeepText  = Color(0xFFA02519)

// ── Dark Theme Surfaces & Accents ────────────────────────────────────────────
val DarkBg         = Color(0xFF0C1410)  // Green-black page
val DarkSurface    = Color(0xFF12211A)
val DarkSurface2   = Color(0xFF1D2D24)
val DarkTextPrimary = Color(0xFFE9EFE7)
val DarkTextSecondary = Color(0xFFB9C9BC)
val MintAccent     = Color(0xFF93D6A8)
val DarkOutline    = Color(0xFF33493C)

// ── Status Colors ────────────────────────────────────────────────────────────
val StatusPending   = Color(0xFFE9A93E) // Amber
val StatusAccepted  = Color(0xFF2D7A4C) // Primary green
val StatusDelivered = Color(0xFF2D7A4C) // Primary green
val StatusInTransit = Color(0xFF3E8E68) // Secondary green
val StatusRejected  = Color(0xFFBA1A1A) // Red
val StatusCancelled = Color(0xFFBA1A1A) // Red
val StatusWithdrawn = Color(0xFF6E7B70) // Grey-green

// ── Structural tokens (light) ───────────────────────────────────────────────
// Warm off-white cards read softer than pure white against the cream page.
val CreamCard      = Color(0xFFFFFDF7)
val CreamBorder    = Color(0xFFE4EBE0)
val InputFillLight = Color(0xFFFBFCF9)
val InputLineLight = Color(0xFFD6E0D2)
val ShimmerBaseL   = Color(0xFFE7EDE3)
val ShimmerHighL   = Color(0xFFF8FBF6)
val DisabledBgL    = Color(0xFFE6EAE4)
val DisabledFgL    = Color(0xFF9AA79D)

// ── Structural tokens (dark) ────────────────────────────────────────────────
val DarkCard       = Color(0xFF16241C)
val DarkCardBorder = Color(0xFF2B3E32)
val InputFillDark  = Color(0xFF18271F)
val InputLineDark  = Color(0xFF33493C)
val ShimmerBaseD   = Color(0xFF1C2C23)
val ShimmerHighD   = Color(0xFF2E4335)
val DisabledBgD    = Color(0xFF24332A)
val DisabledFgD    = Color(0xFF7C8D81)
