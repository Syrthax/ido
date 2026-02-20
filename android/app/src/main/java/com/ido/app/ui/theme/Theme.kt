package com.ido.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * iDo Design System Colors
 * Based on the app's visual identity with indigo primary
 */
private val PureBlack = Color(0xFF000000)
private val BackgroundLight = Color(0xFFF6F6F8)  // Light grayish background
private val CardDark = Color(0xFF1C1C1E)          // Dark card background
private val Primary = Color(0xFF5048E5)           // Indigo primary
private val PrimarySoft = Color(0xFF6366F1)       // Softer indigo variant

private val DarkColorScheme = darkColorScheme(
    primary = Primary,                 // iDo brand indigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF10B981),     // Green for success/sync
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFF59E0B),       // Amber for priority
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF92400E),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = PureBlack,             // OLED pure black background
    onBackground = Color.White,
    surface = PureBlack,                // OLED pure black surface
    onSurface = Color.White,
    surfaceVariant = CardDark,          // Dark card background
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF52525B),
    outlineVariant = Color(0xFF27272A),
    inverseSurface = Color(0xFFE5E5E5),
    inverseOnSurface = Color(0xFF1A1A1A)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,                  // iDo brand indigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF3730A3),
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = BackgroundLight,       // Light grayish background
    onBackground = Color(0xFF1F2937),
    surface = Color.White,              // White surface for cards
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color.White,       // White cards
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    inverseSurface = Color(0xFF1F2937),
    inverseOnSurface = Color(0xFFF9FAFB)
)

/**
 * iDo Theme composable
 * 
 * Theme behavior:
 * - AUTO (default): follows system theme
 * - LIGHT: always light mode
 * - DARK: always dark mode
 * 
 * System bars:
 * - Dark mode: pure black bars, light icons
 * - Light mode: pure white bars, dark icons
 * - Production-grade (Instagram/Google Calendar style)
 */
@Composable
fun IDoTheme(
    themePreferences: ThemePreferences? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val preferences = themePreferences ?: ThemePreferences.getInstance(context)
    val themeMode by preferences.themeMode.collectAsState()
    
    // Determine if dark theme should be used
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.AUTO -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    
    // Use iDo's custom color scheme - no dynamic colors to maintain brand consistency
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Enable edge-to-edge
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            if (useDarkTheme) {
                // Dark mode: black bars, light (white) icons
                window.statusBarColor = PureBlack.toArgb()
                window.navigationBarColor = PureBlack.toArgb()
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            } else {
                // Light mode: light grayish bars, dark (black) icons
                window.statusBarColor = BackgroundLight.toArgb()
                window.navigationBarColor = BackgroundLight.toArgb()
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

