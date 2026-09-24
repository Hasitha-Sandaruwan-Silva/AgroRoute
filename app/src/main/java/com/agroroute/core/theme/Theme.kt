package com.agroroute.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

val AgroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AgroRouteTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgroTypography,
        shapes = AgroShapes,
        content = content
    )
}
