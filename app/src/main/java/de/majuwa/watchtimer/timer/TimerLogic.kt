package de.majuwa.watchtimer.timer

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits [Unit] every [intervalMs] milliseconds indefinitely.
 * Uses coroutine [delay], so it participates in virtual-time test scheduling.
 */
fun tickerFlow(intervalMs: Long = TICK_INTERVAL_MS): Flow<Unit> = flow {
    while (true) {
        delay(intervalMs)
        emit(Unit)
    }
}

/**
 * Returns how many full quarters have elapsed given [remainingMs].
 * Result is clamped to [0, QUARTER_COUNT].
 */
fun completedQuarters(remainingMs: Long): Int {
    val clampedMs = remainingMs.coerceIn(0L, TOTAL_DURATION_MS)
    val elapsedMs = TOTAL_DURATION_MS - clampedMs
    return (elapsedMs / QUARTER_DURATION_MS).toInt().coerceIn(0, QUARTER_COUNT)
}

/**
 * Returns the fraction of the current (active) quarter that has been consumed (0.0–1.0).
 * Returns 1.0 when [remainingMs] is 0 (all time elapsed).
 */
fun currentQuarterProgress(remainingMs: Long): Float {
    val clampedMs = remainingMs.coerceIn(0L, TOTAL_DURATION_MS)
    if (clampedMs <= 0L) return 1f
    val elapsedMs = TOTAL_DURATION_MS - clampedMs
    val elapsedInQuarter = elapsedMs % QUARTER_DURATION_MS
    return elapsedInQuarter.toFloat() / QUARTER_DURATION_MS.toFloat()
}

/**
 * Derives a complete [TimerUiState] from [remainingMs] and [status].
 * This is the single source of truth for all UI-facing data.
 */
fun computeUiState(remainingMs: Long, status: TimerStatus): TimerUiState {
    val clampedMs = remainingMs.coerceIn(0L, TOTAL_DURATION_MS)
    val totalSeconds = (clampedMs / 1_000L).toInt()
    return TimerUiState(
        remainingMs = clampedMs,
        status = status,
        completedQuarters = completedQuarters(clampedMs),
        currentQuarterProgress = currentQuarterProgress(clampedMs),
        displayMinutes = totalSeconds / 60,
        displaySeconds = totalSeconds % 60
    )
}
