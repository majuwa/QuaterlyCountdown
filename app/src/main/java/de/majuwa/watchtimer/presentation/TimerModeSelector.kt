package de.majuwa.watchtimer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import de.majuwa.watchtimer.timer.TimerMode

@Composable
fun TimerModeSelector(
    onSelect: (TimerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onSelect(TimerMode.THREE_MINUTES) }
                    .semantics { contentDescription = "select 3 minute timer" },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "3:00",
                    style = MaterialTheme.typography.display1.copy(fontSize = 32.sp),
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colors.onBackground.copy(alpha = 0.3f)),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable { onSelect(TimerMode.TWO_MINUTES) }
                    .semantics { contentDescription = "select 2 minute timer" },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "2:00",
                    style = MaterialTheme.typography.display1.copy(fontSize = 32.sp),
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
