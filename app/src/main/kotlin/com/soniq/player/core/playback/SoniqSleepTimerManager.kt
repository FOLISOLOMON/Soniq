package com.soniq.player.core.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service-coordinated sleep timer that counts down independently of UI lifecycles and triggers
 * playback pause when the configured duration elapses.
 */
@Singleton
class SoniqSleepTimerManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _remainingMillis = MutableStateFlow<Long?>(null)
    val remainingMillis: StateFlow<Long?> = _remainingMillis.asStateFlow()

    private var timerJob: Job? = null
    private var onExpireCallback: (() -> Unit)? = null

    fun attachExpirationHandler(onExpire: () -> Unit) {
        onExpireCallback = onExpire
    }

    fun detachExpirationHandler() {
        onExpireCallback = null
    }

    fun startTimer(minutes: Int) {
        cancelTimer()
        if (minutes <= 0) return
        val totalMs = minutes * 60_000L
        _remainingMillis.value = totalMs
        timerJob = scope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0L) {
                delay(TICK_INTERVAL_MS)
                remaining = (remaining - TICK_INTERVAL_MS).coerceAtLeast(0L)
                _remainingMillis.value = if (remaining > 0L) remaining else null
            }
            if (isActive) {
                _remainingMillis.value = null
                onExpireCallback?.invoke()
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingMillis.value = null
    }

    companion object {
        val PRESET_MINUTES = listOf(15, 30, 45, 60)
        private const val TICK_INTERVAL_MS = 1_000L
    }
}
