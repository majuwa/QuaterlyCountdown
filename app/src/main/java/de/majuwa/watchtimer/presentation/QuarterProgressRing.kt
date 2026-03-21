package de.majuwa.watchtimer.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.majuwa.watchtimer.presentation.theme.QuarterActive
import de.majuwa.watchtimer.presentation.theme.QuarterCompleted
import de.majuwa.watchtimer.presentation.theme.QuarterEmpty
import de.majuwa.watchtimer.timer.QUARTER_COUNT
import de.majuwa.watchtimer.timer.TimerStatus
import de.majuwa.watchtimer.timer.TimerUiState

private const val SMALL_GAP_DEGREES = 4f

// Larger gap at 12 o'clock so the Scaffold's TimeText is never overlapped.
private const val TOP_GAP_DEGREES = 36f

// Remaining degrees distributed equally among the 4 arcs.
// (360 - 36 - 3 × 4) / 4 = 312 / 4 = 78°
private val ARC_SWEEP = (360f - TOP_GAP_DEGREES - (QUARTER_COUNT - 1) * SMALL_GAP_DEGREES) / QUARTER_COUNT

private data class ArcStyle(
    val color: Color,
    val strokeWidth: Float,
    val alpha: Float,
)

/** Returns the alpha for the active-quarter blink animation, frozen solid when not RUNNING. */
@Composable
private fun rememberActiveAlpha(status: TimerStatus): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "quarter_blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "blinkAlpha",
    )
    return if (status == TimerStatus.RUNNING) blinkAlpha else 1f
}

/**
 * Draws the four-quarter progress ring using [Canvas].
 *
 * Rendering rules:
 * - Completed quarters → solid cyan arc
 * - Active quarter     → blinking green arc (alpha pulses when RUNNING, solid when PAUSED)
 * - Future quarters    → dim grey arc
 *
 * Arcs start at 12 o'clock (270° in Android canvas) and sweep clockwise.
 */
@Composable
fun QuarterProgressRing(
    state: TimerUiState,
    modifier: Modifier = Modifier,
) {
    val activeAlpha = rememberActiveAlpha(state.status)

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .padding(8.dp),
    ) {
        val strokeWidth = size.minDimension * 0.065f
        val ringRadius = size.minDimension / 2f - strokeWidth / 2f
        val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
        val arcSize = Size(ringRadius * 2f, ringRadius * 2f)

        repeat(QUARTER_COUNT) { index ->
            // Quarter 0 starts TOP_GAP_DEGREES/2 past 12 o'clock (clockwise).
            // Each subsequent quarter follows after its arc sweep + a small gap.
            val startAngle = 270f + TOP_GAP_DEGREES / 2f + index * (ARC_SWEEP + SMALL_GAP_DEGREES)
            val arcStyle =
                when {
                    index < state.completedQuarters -> {
                        ArcStyle(QuarterCompleted, strokeWidth, 1f)
                    }

                    index == state.completedQuarters && state.status != TimerStatus.IDLE -> {
                        ArcStyle(QuarterActive, strokeWidth, activeAlpha)
                    }

                    else -> {
                        ArcStyle(QuarterEmpty, strokeWidth, 1f)
                    }
                }
            drawQuarterArc(topLeft, arcSize, startAngle, ARC_SWEEP, arcStyle)
        }
    }
}

private fun DrawScope.drawQuarterArc(
    topLeft: Offset,
    size: Size,
    startAngle: Float,
    sweepAngle: Float,
    style: ArcStyle,
) {
    drawArc(
        color = style.color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        alpha = style.alpha,
        style = Stroke(width = style.strokeWidth, cap = StrokeCap.Round),
    )
}
