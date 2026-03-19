package de.majuwa.watchtimer.timer

const val TOTAL_DURATION_MS: Long = 180_000L  // 3 minutes
const val QUARTER_DURATION_MS: Long = 45_000L  // 45 seconds per quarter
const val QUARTER_COUNT: Int = 4
const val TICK_INTERVAL_MS: Long = 100L

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class TimerUiState(
    val remainingMs: Long = TOTAL_DURATION_MS,
    val status: TimerStatus = TimerStatus.IDLE,
    /** Number of fully elapsed quarters (0–4). */
    val completedQuarters: Int = 0,
    /** Progress consumed within the currently active quarter (0.0–1.0). */
    val currentQuarterProgress: Float = 0f,
    val displayMinutes: Int = 3,
    val displaySeconds: Int = 0
)
