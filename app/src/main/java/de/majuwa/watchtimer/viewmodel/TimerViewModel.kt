package de.majuwa.watchtimer.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import de.majuwa.watchtimer.service.TimerService
import de.majuwa.watchtimer.timer.TimerStatus
import de.majuwa.watchtimer.timer.TimerUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin command dispatcher.  All timer logic now lives in [TimerService] so that
 * the tick loop and vibration survive the screen going blank (the service holds
 * a PARTIAL_WAKE_LOCK that keeps the CPU awake).
 *
 * State transitions (triggered by [onTap]):
 *   IDLE → RUNNING → PAUSED → RUNNING → … → FINISHED → (tap) → IDLE
 */
class TimerViewModel(application: Application) : AndroidViewModel(application) {

    val uiState: StateFlow<TimerUiState> = TimerService.uiState

    /** Single tap: start / pause / restart after finish. */
    fun onTap() {
        when (uiState.value.status) {
            TimerStatus.IDLE,
            TimerStatus.PAUSED   -> sendAction(TimerService.ACTION_START)
            TimerStatus.RUNNING  -> sendAction(TimerService.ACTION_PAUSE)
            TimerStatus.FINISHED -> TimerService.resetState()
        }
    }

    /** Long press: reset to IDLE from any non-idle state. */
    fun onLongPress() {
        when (uiState.value.status) {
            TimerStatus.IDLE     -> Unit
            TimerStatus.FINISHED -> TimerService.resetState()
            else                 -> sendAction(TimerService.ACTION_RESET)
        }
    }

    private fun sendAction(action: String) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            this.action = action
        }
        getApplication<Application>().startService(intent)
    }
}
