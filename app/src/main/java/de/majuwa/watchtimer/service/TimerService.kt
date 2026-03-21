package de.majuwa.watchtimer.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import de.majuwa.watchtimer.MainActivity
import de.majuwa.watchtimer.timer.DEFAULT_CONFIG
import de.majuwa.watchtimer.timer.TICK_INTERVAL_MS
import de.majuwa.watchtimer.timer.TOTAL_DURATION_MS
import de.majuwa.watchtimer.timer.TimerConfig
import de.majuwa.watchtimer.timer.TimerMode
import de.majuwa.watchtimer.timer.TimerStatus
import de.majuwa.watchtimer.timer.TimerUiState
import de.majuwa.watchtimer.timer.computeUiState
import de.majuwa.watchtimer.timer.tickerFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the timer tick loop and a PARTIAL_WAKE_LOCK.
 *
 * Keeping the timer here (instead of the ViewModel) solves two problems:
 *   1. The CPU can sleep when the screen is off and there is no WakeLock, causing
 *      coroutine delay() to stall and the timer to freeze.
 *   2. Vibration called from a process without a power guarantee silently fails
 *      when the screen is off.
 *
 * The service is started/stopped via explicit Intent actions; it is never bound.
 * Timer state is shared through a companion-object StateFlow so the ViewModel
 * can observe it without binding.
 */
class TimerService : Service() {
    // ── Shared state (survives service recreation while companion object lives) ──

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESET = "ACTION_RESET"
        const val EXTRA_CONFIG_MODE = "EXTRA_CONFIG_MODE"

        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1

        // Internal visibility allows unit tests (same module) to set state directly
        // without needing Robolectric to run the full service.
        internal val mutableUiState =
            MutableStateFlow(
                computeUiState(TOTAL_DURATION_MS, TimerStatus.IDLE),
            )
        val uiState: StateFlow<TimerUiState> = mutableUiState.asStateFlow()

        /** Reset state directly without needing the service to be running. */
        internal fun resetState(config: TimerConfig = DEFAULT_CONFIG) {
            mutableUiState.value = computeUiState(config.totalDurationMs, TimerStatus.IDLE, config)
        }
    }

    // ── Instance state ────────────────────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    // Initialised from companion state in onStartCommand so that a service
    // recreation (e.g. after a PAUSE stopSelf) picks up where it left off.
    private var remainingMs: Long = TOTAL_DURATION_MS
    private var lastCompletedQuarters: Int = 0
    private var config: TimerConfig = DEFAULT_CONFIG

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var vibrator: Vibrator

    private val quarterEffect: VibrationEffect =
        VibrationEffect.createWaveform(longArrayOf(0, 70, 60, 70, 60, 70), -1)
    private val finishEffect: VibrationEffect =
        VibrationEffect.createWaveform(longArrayOf(0, 400, 150, 400, 150, 400), -1)

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Vibrator::class.java)
        wakeLock =
            (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WatchTimer::TimerWakeLock")
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // Restore instance state from the companion StateFlow so that resuming
        // after a stopSelf() (e.g. after PAUSE) works correctly.
        remainingMs = mutableUiState.value.remainingMs
        lastCompletedQuarters = mutableUiState.value.completedQuarters
        config = mutableUiState.value.config

        when (intent?.action) {
            ACTION_START -> {
                val modeExtra = intent.getStringExtra(EXTRA_CONFIG_MODE)
                if (modeExtra != null) {
                    config = TimerConfig(TimerMode.valueOf(modeExtra))
                    remainingMs = config.totalDurationMs
                    lastCompletedQuarters = 0
                }
                startTimer()
            }
            ACTION_PAUSE -> pauseAndStop()
            ACTION_RESET -> resetAndStop()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    // ── Timer operations ──────────────────────────────────────────────────────

    private fun startTimer() {
        if (!wakeLock.isHeld) wakeLock.acquire(4 * 60 * 1000L) // 4 minutes
        startForeground(NOTIFICATION_ID, buildNotification())

        tickJob?.cancel()
        tickJob =
            scope.launch {
                val thisJob = coroutineContext.job
                tickerFlow(TICK_INTERVAL_MS).collect {
                    remainingMs -= TICK_INTERVAL_MS
                    if (remainingMs <= 0L) {
                        remainingMs = 0L
                        mutableUiState.value = computeUiState(0L, TimerStatus.FINISHED, config)
                        vibrator.vibrate(finishEffect)
                        wakeScreen()
                        thisJob.cancel()
                        releaseAndStopForeground()
                        stopSelf()
                    } else {
                        val newState = computeUiState(remainingMs, TimerStatus.RUNNING, config)
                        if (newState.completedQuarters > lastCompletedQuarters) {
                            lastCompletedQuarters = newState.completedQuarters
                            vibrator.vibrate(quarterEffect)
                            wakeScreen()
                        }
                        mutableUiState.value = newState
                    }
                }
            }
        // Reflect RUNNING immediately without waiting for the first tick
        mutableUiState.value = computeUiState(remainingMs, TimerStatus.RUNNING, config)
    }

    private fun pauseAndStop() {
        tickJob?.cancel()
        tickJob = null
        mutableUiState.value = computeUiState(remainingMs, TimerStatus.PAUSED, config)
        releaseAndStopForeground()
        stopSelf()
    }

    private fun resetAndStop() {
        tickJob?.cancel()
        tickJob = null
        resetState()
        releaseAndStopForeground()
        stopSelf()
    }

    private fun releaseAndStopForeground() {
        if (wakeLock.isHeld) wakeLock.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /**
     * Brings [MainActivity] to the front, which triggers [Activity.setTurnScreenOn] to
     * wake the display. Using FLAG_ACTIVITY_REORDER_TO_FRONT avoids creating a new instance.
     */
    private fun wakeScreen() {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                // FLAG_ACTIVITY_SINGLE_TOP: if MainActivity is already at the top of its task
                // (e.g. ambient mode), deliver onNewIntent() instead of recreating it.
                // onNewIntent() calls setTurnScreenOn(true) which pokes the power manager.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        startActivity(intent)
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Timer",
                NotificationManager.IMPORTANCE_LOW,
            )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer running")
            .setOngoing(true)
            .build()
}
