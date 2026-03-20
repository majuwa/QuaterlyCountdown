package de.majuwa.watchtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.majuwa.watchtimer.presentation.TimerScreen
import de.majuwa.watchtimer.presentation.theme.WatchTimerTheme
import de.majuwa.watchtimer.viewmodel.TimerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
                )
            }
        }
    }
}
