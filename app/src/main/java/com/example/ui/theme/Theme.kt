package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShivaDarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleDark,
    onPrimaryContainer = PurpleLight,
    secondary = AmberGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = AmberGoldLight,
    tertiary = CyanAccent,
    onTertiary = Color.Black,
    background = ShivaBackground,
    onBackground = TextWhite,
    surface = ShivaSurface,
    onSurface = TextWhite,
    surfaceVariant = ShivaCard,
    onSurfaceVariant = TextGray,
    outline = ShivaBorder,
    outlineVariant = Color(0xFF1E2842)
)

@Composable
fun ShivaPdfTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ShivaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
