package com.example.watchtimer.viewmodel

import android.app.Application
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.watchtimer.timer.TICK_INTERVAL_MS
import com.example.watchtimer.timer.TOTAL_DURATION_MS
import com.example.watchtimer.timer.TimerStatus
import com.example.watchtimer.timer.TimerUiState
import com.example.watchtimer.timer.computeUiState
import com.example.watchtimer.timer.tickerFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Manages the countdown timer state machine.
 *
 * State transitions (triggered by [onTap]):
 *   IDLE → RUNNING → PAUSED → RUNNING → … → FINISHED → (tap) → IDLE
 *
 * Haptic feedback:
 *   - Quarter transition (every 45 s): two short pulses
 *   - Timer finished:                  three longer pulses
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val vibrator: Vibrator =
        application.getSystemService(Vibrator::class.java)

    // Two quick taps — confirms a quarter boundary
    private val quarterEffect: VibrationEffect =
        VibrationEffect.createWaveform(longArrayOf(0, 70, 60, 70), -1)

    // Two long buzzes with a short gap — unmistakably different from the
    // quarter double-tap (which is two quick 70ms taps) and leaves no doubt
    // the countdown is over
    private val finishEffect: VibrationEffect =
        VibrationEffect.createWaveform(longArrayOf(0, 400, 150, 400), -1)

    private val _uiState = MutableStateFlow(
        computeUiState(TOTAL_DURATION_MS, TimerStatus.IDLE)
    )
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var remainingMs: Long = TOTAL_DURATION_MS
    private var tickJob: Job? = null
    private var lastCompletedQuarters: Int = 0

    /** Single tap: start / pause / restart after finish. */
    fun onTap() {
        when (_uiState.value.status) {
            TimerStatus.IDLE,
            TimerStatus.PAUSED -> startTimer()
            TimerStatus.RUNNING -> pauseTimer()
            TimerStatus.FINISHED -> resetTimer()
        }
    }

    /** Long press: reset to IDLE from any state. */
    fun onLongPress() {
        if (_uiState.value.status != TimerStatus.IDLE) {
            resetTimer()
        }
    }

    private fun startTimer() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            val thisJob = coroutineContext.job
            tickerFlow(TICK_INTERVAL_MS).collect {
                remainingMs -= TICK_INTERVAL_MS
                if (remainingMs <= 0L) {
                    remainingMs = 0L
                    _uiState.value = computeUiState(0L, TimerStatus.FINISHED)
                    vibrator.vibrate(finishEffect)
                    thisJob.cancel()
                } else {
                    val newState = computeUiState(remainingMs, TimerStatus.RUNNING)
                    if (newState.completedQuarters > lastCompletedQuarters) {
                        lastCompletedQuarters = newState.completedQuarters
                        vibrator.vibrate(quarterEffect)
                    }
                    _uiState.value = newState
                }
            }
        }
        // Immediately reflect RUNNING state so the UI updates without waiting for the first tick
        _uiState.value = computeUiState(remainingMs, TimerStatus.RUNNING)
    }

    private fun pauseTimer() {
        tickJob?.cancel()
        tickJob = null
        _uiState.value = computeUiState(remainingMs, TimerStatus.PAUSED)
    }

    private fun resetTimer() {
        tickJob?.cancel()
        tickJob = null
        remainingMs = TOTAL_DURATION_MS
        lastCompletedQuarters = 0
        _uiState.value = computeUiState(TOTAL_DURATION_MS, TimerStatus.IDLE)
    }

    public override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
