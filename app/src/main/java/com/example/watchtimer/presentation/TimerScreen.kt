package com.example.watchtimer.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.example.watchtimer.timer.TimerUiState

/**
 * Root screen composable. Fully stateless: receives [TimerUiState] and
 * forwards taps via [onTap]. All state lives in [TimerViewModel].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
    state: TimerUiState,
    onTap: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier  = modifier,
        timeText  = { TimeText() },
        vignette  = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick     = onTap,
                    onLongClick = onLongPress
                )
                .semantics { contentDescription = "timer tap area" },
            contentAlignment = Alignment.Center
        ) {
            QuarterProgressRing(state = state)
            TimerDisplay(state = state)
        }
    }
}

@Composable
private fun TimerDisplay(
    state: TimerUiState,
    modifier: Modifier = Modifier
) {
    Text(
        text      = "%02d:%02d".format(state.displayMinutes, state.displaySeconds),
        modifier  = modifier,
        style     = MaterialTheme.typography.display1,
        color     = MaterialTheme.colors.onBackground,
        textAlign = TextAlign.Center
    )
}
