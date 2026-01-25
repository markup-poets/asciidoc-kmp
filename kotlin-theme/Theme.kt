package com.neuroSKai.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkPrimaryForeground,
    primaryContainer = DarkPrimaryGlow,
    onPrimaryContainer = DarkPrimaryForeground,
    
    secondary = DarkSecondary,
    onSecondary = DarkSecondaryForeground,
    secondaryContainer = DarkSecondary,
    onSecondaryContainer = DarkSecondaryForeground,
    
    tertiary = DarkAccent,
    onTertiary = DarkAccentForeground,
    tertiaryContainer = DarkAccent,
    onTertiaryContainer = DarkAccentForeground,
    
    background = DarkBackground,
    onBackground = DarkForeground,
    
    surface = DarkSurface,
    onSurface = DarkForeground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedForeground,
    
    error = DarkDestructive,
    onError = DarkDestructiveForeground,
    errorContainer = DarkDestructive,
    onErrorContainer = DarkDestructiveForeground,
    
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    
    inverseSurface = LightSurface,
    inverseOnSurface = LightForeground,
    inversePrimary = LightPrimary,
    
    surfaceTint = DarkPrimary,
    scrim = DarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightPrimaryForeground,
    primaryContainer = LightPrimary.copy(alpha = 0.1f),
    onPrimaryContainer = LightPrimary,
    
    secondary = LightSecondary,
    onSecondary = LightSecondaryForeground,
    secondaryContainer = LightSecondary,
    onSecondaryContainer = LightSecondaryForeground,
    
    tertiary = LightAccent,
    onTertiary = LightAccentForeground,
    tertiaryContainer = LightAccent.copy(alpha = 0.1f),
    onTertiaryContainer = LightAccent,
    
    background = LightBackground,
    onBackground = LightForeground,
    
    surface = LightSurface,
    onSurface = LightForeground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedForeground,
    
    error = LightDestructive,
    onError = LightDestructiveForeground,
    errorContainer = LightDestructive.copy(alpha = 0.1f),
    onErrorContainer = LightDestructive,
    
    outline = LightBorder,
    outlineVariant = LightBorder,
    
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkForeground,
    inversePrimary = DarkPrimary,
    
    surfaceTint = LightPrimary,
    scrim = LightBackground
)

@Composable
fun neuroSKaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = neuroSKaiTypography,
        content = content
    )
}
