package com.neuroSKai.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Extended color system for neuroSKai theme
 * Contains custom colors not covered by Material3 ColorScheme
 */
data class ExtendedColors(
    // Glow effects
    val primaryGlow: Color,
    val glowShadow: Color,
    
    // Node/Arc colors for visualizations
    val nodeDark: Color,
    val nodeRed: Color,
    val arcDark: Color,
    val arcRed: Color,
    
    // Card variations
    val cardBackground: Color,
    val cardForeground: Color,
    
    // Input colors
    val inputBackground: Color,
    val inputBorder: Color,
    val ringColor: Color,
    
    // Sidebar colors
    val sidebarBackground: Color,
    val sidebarForeground: Color,
    val sidebarPrimary: Color,
    val sidebarPrimaryForeground: Color,
    val sidebarAccent: Color,
    val sidebarAccentForeground: Color,
    val sidebarBorder: Color,
    
    // Gradients
    val radialGradient: Brush,
    val glowGradient: Brush
)

val DarkExtendedColors = ExtendedColors(
    primaryGlow = DarkPrimaryGlow,
    glowShadow = DarkPrimary.copy(alpha = 0.3f),
    
    nodeDark = NodeDark,
    nodeRed = NodeRed,
    arcDark = ArcDark,
    arcRed = ArcRed,
    
    cardBackground = DarkCard,
    cardForeground = DarkCardForeground,
    
    inputBackground = DarkInput,
    inputBorder = DarkBorder,
    ringColor = DarkRing,
    
    sidebarBackground = Color(0xFF0F1114),
    sidebarForeground = Color(0xFFF2F2F2),
    sidebarPrimary = DarkPrimary,
    sidebarPrimaryForeground = Color(0xFFFFFFFF),
    sidebarAccent = DarkSecondary,
    sidebarAccentForeground = Color(0xFFF2F2F2),
    sidebarBorder = DarkBorder,
    
    radialGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF121418),  // center
            Color(0xFF0A0B0D)   // edge
        )
    ),
    glowGradient = Brush.radialGradient(
        colors = listOf(
            DarkPrimary.copy(alpha = 0.15f),
            Color.Transparent
        )
    )
)

val LightExtendedColors = ExtendedColors(
    primaryGlow = LightPrimary.copy(alpha = 0.8f),
    glowShadow = LightPrimary.copy(alpha = 0.2f),
    
    nodeDark = Color(0xFF94A3B8),
    nodeRed = LightPrimary,
    arcDark = Color(0xFF94A3B8),
    arcRed = LightPrimary,
    
    cardBackground = LightCard,
    cardForeground = LightCardForeground,
    
    inputBackground = Color(0xFFFFFFFF),
    inputBorder = LightBorder,
    ringColor = LightRing,
    
    sidebarBackground = Color(0xFFF8FAFC),
    sidebarForeground = Color(0xFF0F172A),
    sidebarPrimary = LightPrimary,
    sidebarPrimaryForeground = Color(0xFFFFFFFF),
    sidebarAccent = LightSecondary,
    sidebarAccentForeground = Color(0xFF1E293B),
    sidebarBorder = LightBorder,
    
    radialGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF8FAFC)
        )
    ),
    glowGradient = Brush.radialGradient(
        colors = listOf(
            LightPrimary.copy(alpha = 0.1f),
            Color.Transparent
        )
    )
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

/**
 * Access extended colors from the current theme
 */
object ExtendedTheme {
    val colors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
