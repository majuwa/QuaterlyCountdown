package com.example.watchtimer.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.example.watchtimer.presentation.theme.WatchTimerTheme
import com.example.watchtimer.timer.TimerStatus
import com.example.watchtimer.timer.TimerUiState
import com.example.watchtimer.timer.computeUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose UI tests for [TimerScreen].
 *
 * Composables are exercised in isolation by injecting [TimerUiState] directly —
 * no [TimerViewModel] or coroutines are needed here.
 *
 * Run on a Wear OS emulator (API 30+):
 *   ./gradlew :app:connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class TimerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun idleState_displaysThreeMinutes() {
        composeTestRule.setContent {
            WatchTimerTheme {
                TimerScreen(state = TimerUiState(), onTap = {})
            }
        }
        composeTestRule.onNodeWithText("03:00").assertIsDisplayed()
    }

    @Test
    fun finishedState_displaysZero() {
        composeTestRule.setContent {
            WatchTimerTheme {
                TimerScreen(
                    state = computeUiState(0L, TimerStatus.FINISHED),
                    onTap = {}
                )
            }
        }
        composeTestRule.onNodeWithText("00:00").assertIsDisplayed()
    }

    @Test
    fun runningState_displaysCorrectTime() {
        // 90 000 ms remaining → 01:30
        composeTestRule.setContent {
            WatchTimerTheme {
                TimerScreen(
                    state = computeUiState(90_000L, TimerStatus.RUNNING),
                    onTap = {}
                )
            }
        }
        composeTestRule.onNodeWithText("01:30").assertIsDisplayed()
    }

    @Test
    fun tap_invokesOnTapCallback() {
        var tapped = false
        composeTestRule.setContent {
            WatchTimerTheme {
                TimerScreen(state = TimerUiState(), onTap = { tapped = true })
            }
        }
        composeTestRule.onNodeWithContentDescription("timer tap area").performClick()
        assertTrue("onTap should have been called", tapped)
    }

    @Test
    fun quarterRing_rendersWithoutCrash() {
        // Smoke test: verify the Canvas-based ring draws all four states without throwing
        listOf(
            computeUiState(TOTAL_DURATION_MS_TEST, TimerStatus.IDLE),
            computeUiState(135_000L, TimerStatus.RUNNING),
            computeUiState(90_000L, TimerStatus.PAUSED),
            computeUiState(0L, TimerStatus.FINISHED)
        ).forEach { state ->
            composeTestRule.setContent {
                WatchTimerTheme { QuarterProgressRing(state = state) }
            }
            composeTestRule.waitForIdle()
        }
    }
}

private const val TOTAL_DURATION_MS_TEST = 180_000L
