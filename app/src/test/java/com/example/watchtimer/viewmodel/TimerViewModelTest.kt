package com.example.watchtimer.viewmodel

import android.app.Application
import android.os.Vibrator
import com.example.watchtimer.timer.TOTAL_DURATION_MS
import com.example.watchtimer.timer.TimerStatus
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    /**
     * Shared scheduler so that [Dispatchers.Main] (used by viewModelScope) and
     * [runTest] both advance virtual time together.
     */
    private val testScheduler  = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    // Mock Application + Vibrator so AndroidViewModel can be constructed in JVM tests
    private val mockVibrator    = mockk<Vibrator>(relaxed = true)
    private val mockApplication = mockk<Application>(relaxed = true) {
        every { getSystemService(Vibrator::class.java) } returns mockVibrator
        justRun { registerComponentCallbacks(any()) }
    }

    private fun buildVm() = TimerViewModel(mockApplication)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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

    // ── State machine transitions ───────────────────────────────────────────

    @Test
    fun `onTap from IDLE transitions to RUNNING`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.onTap()
        runCurrent()
        assertEquals(TimerStatus.RUNNING, vm.uiState.value.status)
        vm.onCleared()
    }

    @Test
    fun `onTap from RUNNING transitions to PAUSED`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.onTap(); runCurrent()   // IDLE → RUNNING
        vm.onTap(); runCurrent()   // RUNNING → PAUSED
        assertEquals(TimerStatus.PAUSED, vm.uiState.value.status)
    }

    @Test
    fun `onTap from PAUSED resumes to RUNNING`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.onTap(); runCurrent()   // IDLE → RUNNING
        vm.onTap(); runCurrent()   // RUNNING → PAUSED
        vm.onTap(); runCurrent()   // PAUSED → RUNNING
        assertEquals(TimerStatus.RUNNING, vm.uiState.value.status)
        vm.onCleared()
    }

    @Test
    fun `timer reaches FINISHED after 180 seconds`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.onTap()
        advanceTimeBy(181_000L)
        assertEquals(TimerStatus.FINISHED, vm.uiState.value.status)
        assertEquals(0L, vm.uiState.value.remainingMs)
        assertEquals(0, vm.uiState.value.displayMinutes)
        assertEquals(0, vm.uiState.value.displaySeconds)
    }

    @Test
    fun `onTap from FINISHED resets to IDLE`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.onTap()
        advanceTimeBy(181_000L)
        vm.onTap(); runCurrent()   // FINISHED → IDLE
        assertEquals(TimerStatus.IDLE, vm.uiState.value.status)
        assertEquals(TOTAL_DURATION_MS, vm.uiState.value.remainingMs)
    }

    // ── Pause freezes the clock ─────────────────────────────────────────────

    @Test
    fun `paused timer does not decrement remainingMs`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.onTap()
        advanceTimeBy(1_000L)
        val frozenMs = vm.uiState.value.remainingMs
        vm.onTap()                   // pause
        advanceTimeBy(5_000L)        // 5 more seconds while paused
        assertEquals(frozenMs, vm.uiState.value.remainingMs)
    }

    // ── Quarter progression ─────────────────────────────────────────────────

    @Test
    fun `completedQuarters increments each 45 seconds`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.onTap()

        advanceTimeBy(45_100L)
        assertEquals(1, vm.uiState.value.completedQuarters)

        advanceTimeBy(45_000L)
        assertEquals(2, vm.uiState.value.completedQuarters)

        advanceTimeBy(45_000L)
        assertEquals(3, vm.uiState.value.completedQuarters)

        vm.onCleared()
    }
}
