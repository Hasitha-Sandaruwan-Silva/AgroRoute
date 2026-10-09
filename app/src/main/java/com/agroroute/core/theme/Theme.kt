package com.agroroute.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GreenEmerald,
    onPrimary = PureWhite,
    primaryContainer = MintContainer,
    onPrimaryContainer = GreenDeep,
    secondary = HarvestGoldDeep,
    onSecondary = PureWhite,
    secondaryContainer = HarvestCream,
    onSecondaryContainer = AmberDeepText,
    tertiary = OliveLeaf,
    onTertiary = PureWhite,
    tertiaryContainer = HarvestCream,
    onTertiaryContainer = SoilBrown,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = SoftMintBg,
    onBackground = TextDarkGreen,
    surface = CreamCard,
    onSurface = TextDarkGreen,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OutlineGreen,
    outlineVariant = OutlineSoft,
    surfaceTint = GreenEmerald,
    surfaceContainerHighest = SurfaceVariant,
    surfaceContainerHigh = Color(0xFFEAE5D8),
    surfaceContainer = Color(0xFFF0EBDF),
    surfaceContainerLow = SoftMintBg,
    surfaceContainerLowest = PureWhite
)

// Applied whenever the system (or caller) asks for dark mode
private val DarkColorScheme = darkColorScheme(
    primary = MintAccent,
    onPrimary = GreenDeep,
    primaryContainer = DarkSurface2,
    onPrimaryContainer = MintAccent,
    secondary = WarmSand,
    onSecondary = Color(0xFF382C18),
    secondaryContainer = Color(0xFF4A342B),
    onSecondaryContainer = HarvestCream,
    tertiary = WarmSand,
    onTertiary = GreenDeep,
    tertiaryContainer = DarkSurface2,
    onTertiaryContainer = MintAccent,
    error = ErrorRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkCardBorder,
    surfaceTint = MintAccent,
    surfaceContainerHighest = DarkSurface2,
    surfaceContainerHigh = Color(0xFF294237),
    surfaceContainer = DarkSurface,
    surfaceContainerLow = Color(0xFF132A22),
    surfaceContainerLowest = DarkBg
)

/**
 * Keeps the system status-bar icons legible on whichever scheme is active.
 * Purely presentational — no app logic lives here.
 */
@Composable
private fun UpdateStatusBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view)
            .isAppearanceLightStatusBars = !darkTheme
    }
}

/** Large rounded-corner scale — cards 24dp, hero surfaces 28dp. */
val AgroShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/** Pill shape for buttons and chips. */
val PillShape = RoundedCornerShape(percent = 50)

/** Hero surfaces bleed to the screen edges and round off at the bottom only. */
val HeroBottomShape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)

/**
 * Corner-radius scale. Every card, sheet, chip and icon container picks its
 * radius from here so the whole app curves consistently.
 */
object AgroRadius {
    val xs = 10.dp
    val sm = 14.dp
    val md = 18.dp
    val lg = 24.dp
    val xl = 24.dp
    val hero = 30.dp
    // Tinted icon containers scale with the icon they hold.
    val iconSm = 12.dp
    val iconMd = 16.dp
    val iconLg = 20.dp
    val iconXl = 28.dp
}

/** 4dp-base spacing scale used across every screen. */
object AgroSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Horizontal gutter applied to every scrolling screen. */
    val gutter = 16.dp

    /** Inner padding of an [com.agroroute.core.components.AppCard]. */
    val cardPadding = 16.dp

    /** Gap between a section header and the content it introduces. */
    val sectionGap = 12.dp

    /** Extra bottom room so nothing hides behind the bottom bar. */
    val bottomClearance = 96.dp
}

/** Touch-target and icon-container sizes. */
object AgroSize {
    val touchTarget = 48.dp
    val iconSm = 20.dp
    val iconMd = 22.dp
    val iconLg = 24.dp
    val iconXl = 30.dp
    val boxSm = 36.dp
    val boxMd = 44.dp
    val boxLg = 52.dp
    val boxXl = 64.dp
    val strokeWidth = 2.dp
}

/** Soft elevation levels — tonal surfaces instead of harsh shadows. */
object AgroElevation {
    val flat = 0.dp
    val card = 1.dp
    val raised = 2.dp
    val floating = 3.dp
    val lifted = 6.dp
}

@Composable
fun AgroRouteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    UpdateStatusBarAppearance(darkTheme)

    CompositionLocalProvider(LocalAgroExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AgroTypography,
            shapes = AgroShapes,
            content = content
        )
    }
}
