package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AethelgardColorScheme = darkColorScheme(
    primary = MoltenGold,
    onPrimary = ObsidianBg,
    primaryContainer = WarmAmber,
    onPrimaryContainer = ParchmentText,
    secondary = SapphireMana,
    onSecondary = Color.White,
    secondaryContainer = ObsidianCard,
    onSecondaryContainer = ParchmentText,
    tertiary = EmeraldHeal,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = ParchmentText,
    surface = ObsidianSurface,
    onSurface = ParchmentText,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = MutedStone,
    outline = ObsidianBorder,
    error = CrimsonHp,
    onError = Color.White
)

@Composable
fun AethelgardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AethelgardColorScheme,
        typography = Typography,
        content = content
    )
}
