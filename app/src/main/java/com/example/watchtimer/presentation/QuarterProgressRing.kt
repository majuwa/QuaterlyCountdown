package com.example.watchtimer.presentation

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
import com.example.watchtimer.presentation.theme.QuarterActive
import com.example.watchtimer.presentation.theme.QuarterCompleted
import com.example.watchtimer.presentation.theme.QuarterEmpty
import com.example.watchtimer.timer.QUARTER_COUNT
import com.example.watchtimer.timer.TimerStatus
import com.example.watchtimer.timer.TimerUiState

private const val GAP_DEGREES  = 4f
private const val ARC_SWEEP    = 90f - GAP_DEGREES  // 86° per quarter

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
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quarter_blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    // Only pulse when the timer is actively running; freeze solid otherwise
    val activeAlpha = if (state.status == TimerStatus.RUNNING) blinkAlpha else 1f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        val strokeWidth = size.minDimension * 0.065f
        val ringRadius  = size.minDimension / 2f - strokeWidth / 2f
        val topLeft     = Offset(center.x - ringRadius, center.y - ringRadius)
        val arcSize     = Size(ringRadius * 2f, ringRadius * 2f)

        repeat(QUARTER_COUNT) { index ->
            // Quarter 0 starts at 12 o'clock; each subsequent quarter is +90°
            val startAngle = 270f + index * 90f + GAP_DEGREES / 2f

            when {
                index < state.completedQuarters -> {
                    drawQuarterArc(topLeft, arcSize, startAngle, ARC_SWEEP,
                        QuarterCompleted, strokeWidth, alpha = 1f)
                }
                index == state.completedQuarters && state.status != TimerStatus.IDLE -> {
                    drawQuarterArc(topLeft, arcSize, startAngle, ARC_SWEEP,
                        QuarterActive, strokeWidth, alpha = activeAlpha)
                }
                else -> {
                    drawQuarterArc(topLeft, arcSize, startAngle, ARC_SWEEP,
                        QuarterEmpty, strokeWidth, alpha = 1f)
                }
            }
        }
    }
}

private fun DrawScope.drawQuarterArc(
    topLeft: Offset,
    size: Size,
    startAngle: Float,
    sweepAngle: Float,
    color: Color,
    strokeWidth: Float,
    alpha: Float
) {
    drawArc(
        color      = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter  = false,
        topLeft    = topLeft,
        size       = size,
        alpha      = alpha,
        style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
