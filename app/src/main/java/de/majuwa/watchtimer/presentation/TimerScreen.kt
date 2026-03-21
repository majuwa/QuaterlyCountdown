package de.majuwa.watchtimer.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import de.majuwa.watchtimer.timer.TimerMode
import de.majuwa.watchtimer.timer.TimerStatus
import de.majuwa.watchtimer.timer.TimerUiState
import kotlin.math.min

/**
 * Root screen composable. Fully stateless: receives [TimerUiState] and
 * forwards taps via [onTap]. All state lives in [TimerViewModel].
 *
 * Font size is derived from the actual screen dimension at runtime so the
 * digits always sit inside the ring on any watch size.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
    state: TimerUiState,
    onTap: () -> Unit,
    onLongPress: () -> Unit = {},
    onSelectMode: (TimerMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
    ) {
        if (state.status == TimerStatus.IDLE) {
            TimerModeSelector(onSelect = onSelectMode)
        } else {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onClick = onTap,
                            onLongClick = onLongPress,
                        ).semantics { contentDescription = "timer tap area" },
                contentAlignment = Alignment.Center,
            ) {
                // Ring occupies the full canvas; the inner clear area is ~80% of
                // the screen's shorter dimension.
                QuarterProgressRing(state = state)

                // Scale the font to 20% of the shorter screen edge.
                // "03:00" at that size is ≈63% of the inner ring diameter,
                // leaving a comfortable margin on all watch sizes (160–200 dp).
                val screenMin = min(maxWidth.value, maxHeight.value)
                TimerDisplay(
                    state = state,
                    fontSize = (screenMin * 0.20f).sp,
                )
            }
        }
    }
}

@Composable
private fun TimerDisplay(
    state: TimerUiState,
    fontSize: TextUnit = MaterialTheme.typography.display1.fontSize,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "%02d:%02d".format(state.displayMinutes, state.displaySeconds),
        modifier = modifier,
        style = MaterialTheme.typography.display1.copy(fontSize = fontSize),
        color = MaterialTheme.colors.onBackground,
        textAlign = TextAlign.Center,
    )
}
