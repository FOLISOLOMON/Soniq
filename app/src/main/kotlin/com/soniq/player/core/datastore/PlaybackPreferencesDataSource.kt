package com.soniq.player.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soniq.player.core.common.enumValueOrDefault
import com.soniq.player.core.playback.PlaybackRepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** User-configurable playback and audio-effect behavior preferences. */
data class PlaybackPreferences(
    val continueVideoInBackground: Boolean = false,
    val notificationPromptShown: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val equalizerPresetIndex: Int = -1,
    val equalizerBandLevelsMb: List<Int> = emptyList(),
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val gaplessEnabled: Boolean = true,
    val crossfadeDurationSec: Int = 0,
)

/**
 * Lightweight persisted snapshot of the last active audio queue and playback position.
 * Enables seamless restoration after service termination, app restart, or Bluetooth/lock-screen
 * playback resumption (`MediaSession.Callback.onPlaybackResumption`).
 */
data class PlaybackSessionSnapshot(
    val queueTrackIds: List<Long> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
) {
    val isRestorable: Boolean get() = queueTrackIds.isNotEmpty()
}

@Singleton
class PlaybackPreferencesDataSource @Inject constructor(
    @param:PlaybackDataStore private val dataStore: DataStore<Preferences>,
) {

    val preferences: Flow<PlaybackPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            PlaybackPreferences(
                continueVideoInBackground = prefs[Keys.CONTINUE_VIDEO_IN_BG] ?: false,
                notificationPromptShown = prefs[Keys.NOTIFICATION_PROMPT_SHOWN] ?: false,
                equalizerEnabled = prefs[Keys.EQ_ENABLED] ?: false,
                equalizerPresetIndex = prefs[Keys.EQ_PRESET_INDEX] ?: -1,
                equalizerBandLevelsMb = prefs[Keys.EQ_BAND_LEVELS]
                    .orEmpty()
                    .split(',')
                    .mapNotNull { it.trim().toIntOrNull() },
                bassBoostEnabled = prefs[Keys.BASS_BOOST_ENABLED] ?: false,
                bassBoostStrength = (prefs[Keys.BASS_BOOST_STRENGTH] ?: 0).coerceIn(0, 1000),
                gaplessEnabled = prefs[Keys.GAPLESS_ENABLED] ?: true,
                crossfadeDurationSec = (prefs[Keys.CROSSFADE_DURATION_SEC] ?: 0).coerceIn(0, 10),
            )
        }
        .distinctUntilChanged()

    val sessionSnapshot: Flow<PlaybackSessionSnapshot> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> prefs.toSessionSnapshot() }
        .distinctUntilChanged()

    suspend fun getSessionSnapshot(): PlaybackSessionSnapshot = sessionSnapshot.first()

    suspend fun setContinueVideoInBackground(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.CONTINUE_VIDEO_IN_BG] = enabled }
    }

    suspend fun markNotificationPromptShown() {
        dataStore.edit { prefs -> prefs[Keys.NOTIFICATION_PROMPT_SHOWN] = true }
    }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.EQ_ENABLED] = enabled }
    }

    suspend fun setEqualizerPreset(presetIndex: Int, bandLevelsMb: List<Int>) {
        dataStore.edit { prefs ->
            prefs[Keys.EQ_PRESET_INDEX] = presetIndex
            prefs[Keys.EQ_BAND_LEVELS] = bandLevelsMb.joinToString(",")
        }
    }

    suspend fun setBassBoost(enabled: Boolean, strength: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.BASS_BOOST_ENABLED] = enabled
            prefs[Keys.BASS_BOOST_STRENGTH] = strength.coerceIn(0, 1000)
        }
    }

    suspend fun setGaplessEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.GAPLESS_ENABLED] = enabled }
    }

    suspend fun setCrossfadeDurationSec(seconds: Int) {
        dataStore.edit { prefs -> prefs[Keys.CROSSFADE_DURATION_SEC] = seconds.coerceIn(0, 10) }
    }

    suspend fun saveSessionSnapshot(snapshot: PlaybackSessionSnapshot) {
        dataStore.edit { prefs ->
            if (snapshot.queueTrackIds.isEmpty()) {
                prefs.remove(Keys.QUEUE_IDS)
                prefs.remove(Keys.CURRENT_INDEX)
                prefs.remove(Keys.POSITION_MS)
            } else {
                prefs[Keys.QUEUE_IDS] = snapshot.queueTrackIds
                    .take(MAX_PERSISTED_QUEUE_SIZE)
                    .joinToString(separator = ",")
                prefs[Keys.CURRENT_INDEX] = snapshot.currentIndex.coerceAtLeast(0)
                prefs[Keys.POSITION_MS] = snapshot.positionMs.coerceAtLeast(0L)
            }
            prefs[Keys.REPEAT_MODE] = snapshot.repeatMode.name
            prefs[Keys.SHUFFLE_ENABLED] = snapshot.shuffleEnabled
            prefs[Keys.PLAYBACK_SPEED] = snapshot.playbackSpeed.coerceIn(0.5f, 2.5f)
        }
    }

    internal object Keys {
        val CONTINUE_VIDEO_IN_BG = booleanPreferencesKey("continue_video_in_background")
        val NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
        val QUEUE_IDS = stringPreferencesKey("queue_track_ids")
        val CURRENT_INDEX = intPreferencesKey("queue_current_index")
        val POSITION_MS = longPreferencesKey("queue_position_ms")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET_INDEX = intPreferencesKey("eq_preset_index")
        val EQ_BAND_LEVELS = stringPreferencesKey("eq_band_levels_mb")
        val BASS_BOOST_ENABLED = booleanPreferencesKey("bass_boost_enabled")
        val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        val GAPLESS_ENABLED = booleanPreferencesKey("gapless_enabled")
        val CROSSFADE_DURATION_SEC = intPreferencesKey("crossfade_duration_sec")
    }

    private companion object {
        const val MAX_PERSISTED_QUEUE_SIZE = 500

        fun Preferences.toSessionSnapshot(): PlaybackSessionSnapshot {
            val rawIds = this[Keys.QUEUE_IDS].orEmpty()
            val ids = if (rawIds.isBlank()) {
                emptyList()
            } else {
                rawIds.split(',').mapNotNull { it.trim().toLongOrNull() }
            }
            val index = (this[Keys.CURRENT_INDEX] ?: 0).let { idx ->
                if (ids.isEmpty()) 0 else idx.coerceIn(0, ids.lastIndex)
            }
            return PlaybackSessionSnapshot(
                queueTrackIds = ids,
                currentIndex = index,
                positionMs = (this[Keys.POSITION_MS] ?: 0L).coerceAtLeast(0L),
                repeatMode = enumValueOrDefault(this[Keys.REPEAT_MODE], PlaybackRepeatMode.OFF),
                shuffleEnabled = this[Keys.SHUFFLE_ENABLED] ?: false,
                playbackSpeed = (this[Keys.PLAYBACK_SPEED] ?: 1.0f).coerceIn(0.5f, 2.5f),
            )
        }
    }
}
