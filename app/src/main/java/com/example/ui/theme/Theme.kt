package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DinaLiveColorScheme = darkColorScheme(
    primary = BrightPink,
    secondary = Gold,
    tertiary = HotOrange,
    background = LiveBackground,
    surface = CardPurple,
    onPrimary = PureWhite,
    onSecondary = LiveBackground,
    onTertiary = PureWhite,
    onBackground = PureWhite,
    onSurface = PureWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We default to dark theme for immersive live streaming vibe
    dynamicColor: Boolean = false, // Preserve original DinaLive brand scheme
    content: @Composable () -> Unit,
) {
    // We enforce our DinaLive custom high-intensity immersive dark theme
    MaterialTheme(
        colorScheme = DinaLiveColorScheme,
        typography = Typography,
        content = content
    )
}
