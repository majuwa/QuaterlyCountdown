package de.majuwa.watchtimer

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.majuwa.watchtimer.presentation.TimerScreen
import de.majuwa.watchtimer.presentation.theme.WatchTimerTheme
import de.majuwa.watchtimer.viewmodel.TimerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var clearKeepScreenOnJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Allow the activity to turn on the screen and show over the lock screen
        // so the service can wake the display on quarter transitions and finish.
        setTurnScreenOn(true)
        setShowWhenLocked(true)
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: TimerViewModel = viewModel()
            // collectAsStateWithLifecycle stops collection on ON_STOP (battery-safe)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            WatchTimerTheme {
                TimerScreen(
                    state = state,
                    onTap = viewModel::onTap,
                    onLongPress = viewModel::onLongPress,
                    onSelectMode = viewModel::onSelectMode,
                )
            }
        }
    }

    // Called when the service uses FLAG_ACTIVITY_SINGLE_TOP to bring this instance
    // to the front (e.g. on a quarter transition or finish while in ambient mode).
    // Adding window flags explicitly forces WindowManager to re-evaluate the window
    // state and poke the power manager, even when the activity was already resumed.
    // FLAG_KEEP_SCREEN_ON is cleared after 5 s so it doesn't drain the battery.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setTurnScreenOn(true)
        setShowWhenLocked(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )
        clearKeepScreenOnJob?.cancel()
        clearKeepScreenOnJob =
            lifecycleScope.launch {
                delay(5_000L)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
    }
}
