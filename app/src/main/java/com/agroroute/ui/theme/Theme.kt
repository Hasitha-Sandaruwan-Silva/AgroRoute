package com.agroroute.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 color scheme — light and dark variants based on the AgroRoute palette.
 */
private val LightColorScheme = lightColorScheme(
    primary            = Green40,
    onPrimary          = Grey99,
    primaryContainer   = Green90,
    onPrimaryContainer = Green10,
    secondary          = Amber40,
    onSecondary        = Grey99,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber10,
    tertiary           = Brown40,
    onTertiary         = Grey99,
    tertiaryContainer  = Brown90,
    onTertiaryContainer= Brown10,
    error              = ErrorRed,
    background         = Grey99,
    onBackground       = Grey10,
    surface            = Grey99,
    onSurface          = Grey10,
    surfaceVariant     = Grey95,
    onSurfaceVariant   = Grey20,
    outline            = Grey90
)

private val DarkColorScheme = darkColorScheme(
    primary            = Green80,
    onPrimary          = Green20,
    primaryContainer   = Green30,
    onPrimaryContainer = Green90,
    secondary          = Amber80,
    onSecondary        = Amber20,
    secondaryContainer = Amber30,
    onSecondaryContainer = Amber90,
    tertiary           = Brown80,
    onTertiary         = Brown20,
    tertiaryContainer  = Brown30,
    onTertiaryContainer= Brown90,
    error              = ErrorRedDark,
    background         = Grey10,
    onBackground       = Grey90,
    surface            = Grey10,
    onSurface          = Grey90,
    surfaceVariant     = Grey20,
    onSurfaceVariant   = Grey90
)

/**
 * AgroRoute Material 3 theme wrapper — supports dynamic colour on Android 12+ with graceful fallback.
 */
@Composable
fun AgroRouteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Enable Material You on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AgroTypography,
        content     = content
    )
}
