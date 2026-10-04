package com.agroroute.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    secondary = FreshGreen,
    onSecondary = PureWhite,
    secondaryContainer = MintContainer,
    onSecondaryContainer = GreenDeep,
    tertiary = GreenLight,
    onTertiary = PureWhite,
    tertiaryContainer = MintContainer,
    onTertiaryContainer = GreenDeep,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = ErrorRed,
    background = SoftMintBg,
    onBackground = TextDarkGreen,
    surface = PureWhite,
    onSurface = TextDarkGreen,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OutlineGreen,
    outlineVariant = OutlineGreen
)

// Applied whenever the system (or caller) asks for dark mode
private val DarkColorScheme = darkColorScheme(
    primary = MintAccent,
    onPrimary = DarkBg,
    primaryContainer = DarkSurface2,
    onPrimaryContainer = MintAccent,
    secondary = GreenLight,
    onSecondary = DarkBg,
    secondaryContainer = DarkSurface2,
    onSecondaryContainer = MintAccent,
    tertiary = FreshGreen,
    onTertiary = DarkBg,
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
    outlineVariant = DarkOutline
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

val AgroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AgroRouteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    UpdateStatusBarAppearance(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgroTypography,
        shapes = AgroShapes,
        content = content
    )
}
