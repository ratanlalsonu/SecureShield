package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 1. Cyber Blue (Default / Classic Security)
private val CyberBlueLightScheme = lightColorScheme(
    primary = ShieldBluePrimary,
    onPrimary = Color.White,
    primaryContainer = ShieldBlueContainer,
    onPrimaryContainer = ShieldOnBlueContainer,
    secondary = ShieldSecondary,
    onSecondary = Color.White,
    secondaryContainer = ShieldSecondaryContainer,
    background = ShieldBackgroundLight,
    surface = ShieldSurfaceLight,
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
)

private val CyberBlueDarkScheme = darkColorScheme(
    primary = ShieldBlueLight,
    onPrimary = Color.White,
    primaryContainer = ShieldBlueDark,
    onPrimaryContainer = ShieldBlueContainer,
    secondary = ShieldSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF004D40),
    background = Color(0xFF101928),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF8FAFC)
)

// 2. Emerald Sentinel (Cyber Matrix Green)
private val EmeraldSentinelLightScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = EmeraldSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    background = Color(0xFFF7FDF9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6F4EA),
    onBackground = Color(0xFF061A14),
    onSurface = Color(0xFF0E2E25)
)

private val EmeraldSentinelDarkScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF003730),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = Color(0xFFA7FFEB),
    secondary = EmeraldSecondaryDark,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF004D40),
    background = Color(0xFF071B15),
    surface = Color(0xFF0F2E24),
    surfaceVariant = Color(0xFF1B3D33),
    onBackground = Color(0xFFE8F5E9),
    onSurface = Color(0xFFF1F8E9)
)

// 3. Midnight Stealth (Dark Obsidian & Indigo)
private val MidnightStealthLightScheme = lightColorScheme(
    primary = MidnightPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = MidnightContainerLight,
    onPrimaryContainer = MidnightOnContainerLight,
    secondary = MidnightSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFFFAFAFD),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDE9FE),
    onBackground = Color(0xFF1E1B4B),
    onSurface = Color(0xFF312E81)
)

private val MidnightStealthDarkScheme = darkColorScheme(
    primary = MidnightPrimaryDark,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = MidnightContainerDark,
    onPrimaryContainer = Color(0xFFEEF2FF),
    secondary = MidnightSecondaryDark,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF3730A3),
    background = Color(0xFF0B0F19),
    surface = Color(0xFF131B2E),
    surfaceVariant = Color(0xFF1E293B),
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF9FAFB)
)

// 4. Crimson Aegis (Tactical Ruby)
private val CrimsonAegisLightScheme = lightColorScheme(
    primary = CrimsonPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainerLight,
    onPrimaryContainer = CrimsonOnContainerLight,
    secondary = CrimsonSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEBEE),
    background = Color(0xFFFFFBFB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFEECEC),
    onBackground = Color(0xFF2B0A0C),
    onSurface = Color(0xFF3B1215)
)

private val CrimsonAegisDarkScheme = darkColorScheme(
    primary = CrimsonPrimaryDark,
    onPrimary = Color(0xFF490002),
    primaryContainer = CrimsonContainerDark,
    onPrimaryContainer = Color(0xFFFFCDD2),
    secondary = CrimsonSecondaryDark,
    onSecondary = Color(0xFF5F0003),
    secondaryContainer = Color(0xFF8B0000),
    background = Color(0xFF1A0A0C),
    surface = Color(0xFF281114),
    surfaceVariant = Color(0xFF3D181C),
    onBackground = Color(0xFFFFEBEE),
    onSurface = Color(0xFFFFF0F2)
)

// 5. Nordic Frost (Clean Arctic Slate)
private val NordicFrostLightScheme = lightColorScheme(
    primary = NordicPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = NordicContainerLight,
    onPrimaryContainer = NordicOnContainerLight,
    secondary = NordicSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    background = Color(0xFFF0F9FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Color(0xFF082F49),
    onSurface = Color(0xFF0C4A6E)
)

private val NordicFrostDarkScheme = darkColorScheme(
    primary = NordicPrimaryDark,
    onPrimary = Color(0xFF082F49),
    primaryContainer = NordicContainerDark,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = NordicSecondaryDark,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF075985),
    background = Color(0xFF081422),
    surface = Color(0xFF0E2238),
    surfaceVariant = Color(0xFF163250),
    onBackground = Color(0xFFF0F9FF),
    onSurface = Color(0xFFF8FAFC)
)

fun resolveColorScheme(
    palette: AppThemePalette,
    darkTheme: Boolean,
    amoledPureBlack: Boolean,
    context: Context
): ColorScheme {
    val baseScheme = when (palette) {
        AppThemePalette.DYNAMIC_MATERIAL -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) CyberBlueDarkScheme else CyberBlueLightScheme
            }
        }
        AppThemePalette.CYBER_BLUE -> if (darkTheme) CyberBlueDarkScheme else CyberBlueLightScheme
        AppThemePalette.EMERALD_SENTINEL -> if (darkTheme) EmeraldSentinelDarkScheme else EmeraldSentinelLightScheme
        AppThemePalette.MIDNIGHT_STEALTH -> if (darkTheme) MidnightStealthDarkScheme else MidnightStealthLightScheme
        AppThemePalette.CRIMSON_AEGIS -> if (darkTheme) CrimsonAegisDarkScheme else CrimsonAegisLightScheme
        AppThemePalette.NORDIC_FROST -> if (darkTheme) NordicFrostDarkScheme else NordicFrostLightScheme
    }

    return if (darkTheme && amoledPureBlack) {
        baseScheme.copy(
            background = Color.Black,
            surface = Color(0xFF08080A),
            surfaceVariant = Color(0xFF141416)
        )
    } else {
        baseScheme
    }
}

@Composable
fun SecureShieldTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeConfig.themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = resolveColorScheme(
        palette = themeConfig.palette,
        darkTheme = isDark,
        amoledPureBlack = themeConfig.amoledPureBlack,
        context = context
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Overload for backwards compatibility
@Composable
fun SecureShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (dynamicColor) AppThemePalette.DYNAMIC_MATERIAL else AppThemePalette.CYBER_BLUE
    val themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
    SecureShieldTheme(
        themeConfig = ThemeConfig(palette = palette, themeMode = themeMode),
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) = SecureShieldTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
