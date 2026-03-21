package de.majuwa.watchtimer.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import de.majuwa.watchtimer.service.TimerService
import de.majuwa.watchtimer.timer.TimerConfig
import de.majuwa.watchtimer.timer.TimerMode
import de.majuwa.watchtimer.timer.TimerStatus
import de.majuwa.watchtimer.timer.TimerUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin command dispatcher.  All timer logic now lives in [TimerService] so that
 * the tick loop and vibration survive the screen going blank (the service holds
 * a PARTIAL_WAKE_LOCK that keeps the CPU awake).
 *
 * State transitions:
 *   IDLE: tap on mode selector half → onSelectMode → RUNNING
 *   RUNNING → (tap) → PAUSED → (tap) → RUNNING → … → FINISHED → (tap) → IDLE
 *   RUNNING/PAUSED/FINISHED → (long press) → IDLE
 */
class TimerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val uiState: StateFlow<TimerUiState> = TimerService.uiState

    /** Single tap: pause / restart after finish. IDLE is a no-op (mode selector handles start). */
    fun onTap() {
        when (uiState.value.status) {
            TimerStatus.IDLE -> Unit

            TimerStatus.PAUSED -> sendAction(TimerService.ACTION_START)

            TimerStatus.RUNNING -> sendAction(TimerService.ACTION_PAUSE)

            TimerStatus.FINISHED -> TimerService.resetState()
        }
    }

    /** Called when the user taps a mode half in the idle split-screen selector. */
    fun onSelectMode(mode: TimerMode) {
        if (uiState.value.status != TimerStatus.IDLE) return
        sendStart(TimerConfig(mode))
    }

    /** Long press: reset to IDLE from any non-idle state. */
    fun onLongPress() {
        when (uiState.value.status) {
            TimerStatus.IDLE -> Unit
            TimerStatus.FINISHED -> TimerService.resetState()
            else -> sendAction(TimerService.ACTION_RESET)
        }
    }

    private fun sendStart(config: TimerConfig) {
        val intent =
            Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_CONFIG_MODE, config.mode.name)
            }
        getApplication<Application>().startService(intent)
    }

    private fun sendAction(action: String) {
        val intent =
            Intent(getApplication(), TimerService::class.java).apply {
                this.action = action
            }
        getApplication<Application>().startService(intent)
    }
}
