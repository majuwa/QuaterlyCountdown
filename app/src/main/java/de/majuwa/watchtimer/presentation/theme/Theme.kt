package de.majuwa.watchtimer.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val WatchTimerColors = Colors(
    primary = QuarterActive,
    primaryVariant = QuarterCompleted,
    secondary = QuarterCompleted,
    secondaryVariant = QuarterCompleted,
    background = ColorBackground,
    surface = ColorBackground,
    error = Color(0xFFCF6679),
    onPrimary = ColorBackground,
    onSecondary = ColorBackground,
    onBackground = ColorOnBackground,
    onSurface = ColorOnBackground,
    onSurfaceVariant = ColorOnBackground,
    onError = ColorBackground
)

@Composable
fun WatchTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WatchTimerColors,
        typography = WatchTimerTypography,
        content = content
    )
}
