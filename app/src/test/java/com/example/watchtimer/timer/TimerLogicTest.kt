package com.example.watchtimer.timer

import app.cash.turbine.test
import de.majuwa.watchtimer.timer.QUARTER_COUNT
import de.majuwa.watchtimer.timer.TOTAL_DURATION_MS
import de.majuwa.watchtimer.timer.TimerConfig
import de.majuwa.watchtimer.timer.TimerMode
import de.majuwa.watchtimer.timer.TimerStatus
import de.majuwa.watchtimer.timer.completedQuarters
import de.majuwa.watchtimer.timer.computeUiState
import de.majuwa.watchtimer.timer.currentQuarterProgress
import de.majuwa.watchtimer.timer.tickerFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerLogicTest {
    // ── computeUiState ──────────────────────────────────────────────────────

    @Test
    fun `computeUiState at full duration returns IDLE 3 minutes`() {
        val state = computeUiState(TOTAL_DURATION_MS, TimerStatus.IDLE)
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(TOTAL_DURATION_MS, state.remainingMs)
        assertEquals(0, state.completedQuarters)
        assertEquals(0f, state.currentQuarterProgress, 0.001f)
        assertEquals(3, state.displayMinutes)
        assertEquals(0, state.displaySeconds)
    }

    @Test
    fun `computeUiState after exactly one quarter (135s remaining)`() {
        val state = computeUiState(135_000L, TimerStatus.RUNNING)
        assertEquals(1, state.completedQuarters)
        assertEquals(0f, state.currentQuarterProgress, 0.001f)
        assertEquals(2, state.displayMinutes)
        assertEquals(15, state.displaySeconds)
    }

    @Test
    fun `computeUiState at midpoint of first quarter (157500ms remaining)`() {
        val state = computeUiState(157_500L, TimerStatus.RUNNING)
        assertEquals(0, state.completedQuarters)
        assertEquals(0.5f, state.currentQuarterProgress, 0.001f)
        assertEquals(2, state.displayMinutes)
        assertEquals(37, state.displaySeconds)
    }

    @Test
    fun `computeUiState at zero shows finished with 00_00`() {
        val state = computeUiState(0L, TimerStatus.FINISHED)
        assertEquals(TimerStatus.FINISHED, state.status)
        assertEquals(4, state.completedQuarters)
        assertEquals(1f, state.currentQuarterProgress, 0.001f)
        assertEquals(0, state.displayMinutes)
        assertEquals(0, state.displaySeconds)
    }

    @Test
    fun `computeUiState clamps negative remainingMs to zero`() {
        val state = computeUiState(-1_000L, TimerStatus.RUNNING)
        assertEquals(0L, state.remainingMs)
        assertEquals(0, state.displayMinutes)
        assertEquals(0, state.displaySeconds)
    }

    @Test
    fun `computeUiState clamps over-maximum remainingMs`() {
        val state = computeUiState(TOTAL_DURATION_MS + 10_000L, TimerStatus.RUNNING)
        assertEquals(TOTAL_DURATION_MS, state.remainingMs)
    }

    // ── completedQuarters ───────────────────────────────────────────────────

    @Test
    fun `completedQuarters is 0 at full duration`() {
        assertEquals(0, completedQuarters(TOTAL_DURATION_MS))
    }

    @Test
    fun `completedQuarters is 1 after one quarter`() {
        assertEquals(1, completedQuarters(135_000L))
    }

    @Test
    fun `completedQuarters is 2 at half duration`() {
        assertEquals(2, completedQuarters(90_000L))
    }

    @Test
    fun `completedQuarters is 4 at zero`() {
        assertEquals(QUARTER_COUNT, completedQuarters(0L))
    }

    // ── currentQuarterProgress ──────────────────────────────────────────────

    @Test
    fun `currentQuarterProgress is 0 at start of each quarter`() {
        assertEquals(0f, currentQuarterProgress(TOTAL_DURATION_MS), 0.001f)
        assertEquals(0f, currentQuarterProgress(135_000L), 0.001f)
        assertEquals(0f, currentQuarterProgress(90_000L), 0.001f)
        assertEquals(0f, currentQuarterProgress(45_000L), 0.001f)
    }

    @Test
    fun `currentQuarterProgress is 0_5 at midpoint of a quarter`() {
        assertEquals(0.5f, currentQuarterProgress(157_500L), 0.001f)
    }

    @Test
    fun `currentQuarterProgress is 1 at zero remaining`() {
        assertEquals(1f, currentQuarterProgress(0L), 0.001f)
    }

    // ── 2-minute mode ────────────────────────────────────────────────────────

    @Test
    fun `computeUiState 2-min full duration shows 2 minutes`() {
        val config = TimerConfig(TimerMode.TWO_MINUTES)
        val state = computeUiState(config.totalDurationMs, TimerStatus.IDLE, config)
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(2, state.displayMinutes)
        assertEquals(0, state.displaySeconds)
        assertEquals(0, state.completedQuarters)
        assertEquals(0f, state.currentQuarterProgress, 0.001f)
    }

    @Test
    fun `completedQuarters is 1 after 30s elapsed in 2-min mode`() {
        // 2-min total = 120 000ms, quarter = 30 000ms; after 30s elapsed → 90 000ms remaining
        val config = TimerConfig(TimerMode.TWO_MINUTES)
        assertEquals(1, completedQuarters(90_000L, config))
    }

    @Test
    fun `currentQuarterProgress is 0_5 at midpoint of 2-min first quarter`() {
        // first quarter is 30 000ms; midpoint elapsed = 15 000ms → remaining = 105 000ms
        val config = TimerConfig(TimerMode.TWO_MINUTES)
        assertEquals(0.5f, currentQuarterProgress(105_000L, config), 0.001f)
    }

    // ── tickerFlow ──────────────────────────────────────────────────────────

    @Test
    fun `tickerFlow emits three values then can be cancelled`() =
        runTest {
            tickerFlow(intervalMs = 50L).test {
                awaitItem()
                awaitItem()
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
