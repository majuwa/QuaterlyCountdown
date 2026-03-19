package com.example.watchtimer.viewmodel

import android.app.Application
import de.majuwa.watchtimer.service.TimerService
import de.majuwa.watchtimer.timer.QUARTER_DURATION_MS
import de.majuwa.watchtimer.timer.TOTAL_DURATION_MS
import de.majuwa.watchtimer.timer.TimerStatus
import de.majuwa.watchtimer.timer.computeUiState
import de.majuwa.watchtimer.viewmodel.TimerViewModel
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TimerViewModel].
 *
 * The timer tick loop now lives in [TimerService] (which requires Robolectric
 * or an instrumented test to run). These tests cover the ViewModel's dispatch
 * logic by:
 *   - setting [TimerService._uiState] directly to the desired pre-condition state, and
 *   - verifying that the correct Intent action is passed to [Application.startService].
 *
 * Pure timer-logic tests (quarter calculation, progress, etc.) remain in
 * [com.example.watchtimer.timer.TimerLogicTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testScheduler  = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private val mockApplication = mockk<Application>(relaxed = true)

    private fun buildVm() = TimerViewModel(mockApplication)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        TimerService.resetState()          // always start from IDLE
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        TimerService.resetState()
    }

    // ── Initial state ───────────────────────────────────────────────────────

    @Test
    fun `initial state is IDLE with full duration`() {
        val vm = buildVm()
        assertEquals(TimerStatus.IDLE, vm.uiState.value.status)
        assertEquals(TOTAL_DURATION_MS, vm.uiState.value.remainingMs)
        assertEquals(3, vm.uiState.value.displayMinutes)
        assertEquals(0, vm.uiState.value.displaySeconds)
    }

    // ── Dispatch: correct ACTION sent for each status ───────────────────────

    @Test
    fun `onTap from IDLE sends ACTION_START`() = runTest(testDispatcher) {
        val vm = buildVm()              // service state = IDLE
        vm.onTap()
        verify { mockApplication.startService(match { it.action == TimerService.ACTION_START }) }
    }

    @Test
    fun `onTap from RUNNING sends ACTION_PAUSE`() = runTest(testDispatcher) {
        TimerService._uiState.value = computeUiState(TOTAL_DURATION_MS, TimerStatus.RUNNING)
        val vm = buildVm()
        vm.onTap()
        verify { mockApplication.startService(match { it.action == TimerService.ACTION_PAUSE }) }
    }

    @Test
    fun `onTap from PAUSED sends ACTION_START`() = runTest(testDispatcher) {
        TimerService._uiState.value = computeUiState(TOTAL_DURATION_MS / 2, TimerStatus.PAUSED)
        val vm = buildVm()
        vm.onTap()
        verify { mockApplication.startService(match { it.action == TimerService.ACTION_START }) }
    }

    @Test
    fun `onTap from FINISHED resets state to IDLE without starting service`() =
        runTest(testDispatcher) {
            TimerService._uiState.value = computeUiState(0L, TimerStatus.FINISHED)
            val vm = buildVm()
            vm.onTap()
            verify(exactly = 0) { mockApplication.startService(any()) }
            assertEquals(TimerStatus.IDLE, vm.uiState.value.status)
            assertEquals(TOTAL_DURATION_MS, vm.uiState.value.remainingMs)
        }

    // ── Long press dispatch ─────────────────────────────────────────────────

    @Test
    fun `onLongPress from IDLE does nothing`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.onLongPress()
        verify(exactly = 0) { mockApplication.startService(any()) }
    }

    @Test
    fun `onLongPress from RUNNING sends ACTION_RESET`() = runTest(testDispatcher) {
        TimerService._uiState.value = computeUiState(TOTAL_DURATION_MS, TimerStatus.RUNNING)
        val vm = buildVm()
        vm.onLongPress()
        verify { mockApplication.startService(match { it.action == TimerService.ACTION_RESET }) }
    }

    @Test
    fun `onLongPress from PAUSED sends ACTION_RESET`() = runTest(testDispatcher) {
        TimerService._uiState.value = computeUiState(TOTAL_DURATION_MS / 2, TimerStatus.PAUSED)
        val vm = buildVm()
        vm.onLongPress()
        verify { mockApplication.startService(match { it.action == TimerService.ACTION_RESET }) }
    }

    @Test
    fun `onLongPress from FINISHED resets state to IDLE without starting service`() =
        runTest(testDispatcher) {
            TimerService._uiState.value = computeUiState(0L, TimerStatus.FINISHED)
            val vm = buildVm()
            vm.onLongPress()
            verify(exactly = 0) { mockApplication.startService(any()) }
            assertEquals(TimerStatus.IDLE, vm.uiState.value.status)
        }

    // ── Paused state preserves remainingMs ─────────────────────────────────

    @Test
    fun `paused state is reflected in uiState`() {
        val pausedMs = TOTAL_DURATION_MS - QUARTER_DURATION_MS
        TimerService._uiState.value = computeUiState(pausedMs, TimerStatus.PAUSED)
        val vm = buildVm()
        assertEquals(TimerStatus.PAUSED, vm.uiState.value.status)
        assertEquals(pausedMs, vm.uiState.value.remainingMs)
        assertEquals(1, vm.uiState.value.completedQuarters)
    }
}
