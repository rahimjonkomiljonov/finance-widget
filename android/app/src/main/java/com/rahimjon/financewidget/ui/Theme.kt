package com.rahimjon.financewidget.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2D7FF9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B4F9C),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondaryContainer = Color(0xFF2B3B55),
    onSecondaryContainer = Color(0xFFDDE7F8),
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEDEDED),
    surfaceContainer = Color(0xFF2A2A2A),
    surfaceContainerHigh = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFB0B0B8),
    outlineVariant = Color(0xFF3A3A3F),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6FE5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF0B2B66),
    secondaryContainer = Color(0xFFDDE7F8),
    onSecondaryContainer = Color(0xFF10233F),
    background = Color(0xFFF6F6F8),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFF6F6F8),
    onSurface = Color(0xFF1B1B1F),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEDEDF1),
    onSurfaceVariant = Color(0xFF6B6B72),
    outlineVariant = Color(0xFFE1E1E6),
)

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

// Categorical palette for the allocation chart: fixed order, never cycled. Both columns were
// validated for colour-blind separation against their surface (see the desktop app's chart).
private val ChartPaletteDark = listOf(
    Color(0xFF3987E5), Color(0xFFD95926), Color(0xFF199E70), Color(0xFFC98500),
    Color(0xFFD55181), Color(0xFF008300), Color(0xFF9085E9), Color(0xFFE66767),
)
private val ChartPaletteLight = listOf(
    Color(0xFF2A78D6), Color(0xFFEB6834), Color(0xFF1BAF7A), Color(0xFFEDA100),
    Color(0xFFE87BA4), Color(0xFF008300), Color(0xFF4A3AA7), Color(0xFFE34948),
)
val ChartOtherColor = Color(0xFF898781)

@Composable
fun chartPalette(): List<Color> = if (isSystemInDarkTheme()) ChartPaletteDark else ChartPaletteLight
