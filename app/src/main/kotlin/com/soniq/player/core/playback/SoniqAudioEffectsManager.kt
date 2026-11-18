package com.soniq.player.core.playback

import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.soniq.player.core.common.di.IoDispatcher
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over Android's [Equalizer] and [BassBoost] audio effects for testability and
 * clean lifecycle management.
 */
interface AudioEffectsBackend {
    fun attach(audioSessionId: Int): AttachedEffectsCapabilities
    fun setEqualizerEnabled(enabled: Boolean)
    fun setBandLevel(bandIndex: Int, levelMb: Int)
    fun usePreset(presetIndex: Int): List<Int>?
    fun setBassBoost(enabled: Boolean, strength: Int)
    fun release()
}

data class AttachedEffectsCapabilities(
    val equalizerSupported: Boolean,
    val equalizerUnsupportedReason: String? = null,
    val bandCenterFreqHz: List<Int> = emptyList(),
    val minBandLevelMb: Int = -1500,
    val maxBandLevelMb: Int = 1500,
    val defaultBandLevelsMb: List<Int> = emptyList(),
    val presetNames: List<String> = emptyList(),
    val bassBoostSupported: Boolean,
    val bassBoostUnsupportedReason: String? = null,
)

@Singleton
class AndroidAudioEffectsBackend @Inject constructor() : AudioEffectsBackend {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    override fun attach(audioSessionId: Int): AttachedEffectsCapabilities {
        release()
        if (audioSessionId == AudioManager.AUDIO_SESSION_ID_GENERATE) {
            return AttachedEffectsCapabilities(
                equalizerSupported = false,
                equalizerUnsupportedReason = "Waiting for an active audio playback session.",
                bassBoostSupported = false,
                bassBoostUnsupportedReason = "Waiting for an active audio playback session.",
            )
        }

        var eqSupported = false
        var eqReason: String? = null
        var centerFreqs = emptyList<Int>()
        var minMb = -1500
        var maxMb = 1500
        var defaultLevels = emptyList<Int>()
        var presets = emptyList<String>()

        try {
            val eq = Equalizer(0, audioSessionId)
            val numBands = eq.numberOfBands.toInt()
            if (numBands > 0) {
                val range = eq.bandLevelRange
                minMb = range[0].toInt()
                maxMb = range[1].toInt()
                centerFreqs = List(numBands) { band -> eq.getCenterFreq(band.toShort()) / 1000 }
                defaultLevels = List(numBands) { band -> eq.getBandLevel(band.toShort()).toInt() }
                presets = List(eq.numberOfPresets.toInt()) { preset ->
                    eq.getPresetName(preset.toShort()).orEmpty().ifBlank { "Preset ${preset + 1}" }
                }
                equalizer = eq
                eqSupported = true
            } else {
                eq.release()
                eqReason = "This device reports 0 hardware equalizer bands."
            }
        } catch (error: Throwable) {
            eqReason = error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Equalizer is not supported by this device's audio output."
        }

        var bbSupported = false
        var bbReason: String? = null
        try {
            val bb = BassBoost(0, audioSessionId)
            if (bb.strengthSupported) {
                bassBoost = bb
                bbSupported = true
            } else {
                bb.release()
                bbReason = "Variable Bass Boost strength is not supported on this device."
            }
        } catch (error: Throwable) {
            bbReason = error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Bass Boost is not supported by this device's audio output."
        }

        return AttachedEffectsCapabilities(
            equalizerSupported = eqSupported,
            equalizerUnsupportedReason = eqReason,
            bandCenterFreqHz = centerFreqs,
            minBandLevelMb = minMb,
            maxBandLevelMb = maxMb,
            defaultBandLevelsMb = defaultLevels,
            presetNames = presets,
            bassBoostSupported = bbSupported,
            bassBoostUnsupportedReason = bbReason,
        )
    }

    override fun setEqualizerEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
    }

    override fun setBandLevel(bandIndex: Int, levelMb: Int) {
        runCatching {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMb.toShort())
        }
    }

    override fun usePreset(presetIndex: Int): List<Int>? {
        val eq = equalizer ?: return null
        return runCatching {
            if (presetIndex in 0 until eq.numberOfPresets.toInt()) {
                eq.usePreset(presetIndex.toShort())
                List(eq.numberOfBands.toInt()) { band -> eq.getBandLevel(band.toShort()).toInt() }
            } else {
                null
            }
        }.getOrNull()
    }

    override fun setBassBoost(enabled: Boolean, strength: Int) {
        runCatching {
            bassBoost?.let { bb ->
                bb.enabled = enabled
                if (bb.strengthSupported) {
                    bb.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        }
    }

    override fun release() {
        runCatching { equalizer?.release() }
        equalizer = null
        runCatching { bassBoost?.release() }
        bassBoost = null
    }
}

/**
 * Coordinates SONIQ's persisted audio-effect settings, device capability detection, and
 * service-owned hardware [Equalizer]/[BassBoost] attachment.
 */
@Singleton
class SoniqAudioEffectsManager @Inject constructor(
    private val preferences: PlaybackPreferencesDataSource,
    private val backend: AudioEffectsBackend,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _state = MutableStateFlow(AudioEffectsState())
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    private var currentSessionId: Int = AudioManager.AUDIO_SESSION_ID_GENERATE

    init {
        scope.launch {
            val saved = preferences.preferences.first()
            _state.update { current ->
                current.copy(
                    isEqualizerEnabled = saved.equalizerEnabled,
                    selectedPresetIndex = saved.equalizerPresetIndex,
                    bandLevelsMb = if (current.bandLevelsMb.isEmpty()) saved.equalizerBandLevelsMb else current.bandLevelsMb,
                    isBassBoostEnabled = saved.bassBoostEnabled,
                    bassBoostStrength = saved.bassBoostStrength,
                    gaplessEnabled = saved.gaplessEnabled,
                    crossfadeDurationSec = saved.crossfadeDurationSec,
                )
            }
        }
    }

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId == currentSessionId && _state.value.isSessionAttached) return
        currentSessionId = audioSessionId
        val caps = backend.attach(audioSessionId)

        scope.launch {
            val saved = preferences.preferences.first()
            val bandCount = caps.bandCenterFreqHz.size
            val resolvedLevels = when {
                !caps.equalizerSupported || bandCount == 0 -> emptyList()
                saved.equalizerPresetIndex in caps.presetNames.indices -> {
                    backend.usePreset(saved.equalizerPresetIndex) ?: caps.defaultBandLevelsMb
                }
                saved.equalizerBandLevelsMb.size == bandCount -> {
                    saved.equalizerBandLevelsMb.map { it.coerceIn(caps.minBandLevelMb, caps.maxBandLevelMb) }
                }
                else -> caps.defaultBandLevelsMb
            }

            if (caps.equalizerSupported) {
                if (saved.equalizerPresetIndex !in caps.presetNames.indices) {
                    resolvedLevels.forEachIndexed { idx, level -> backend.setBandLevel(idx, level) }
                }
                backend.setEqualizerEnabled(saved.equalizerEnabled)
            }
            if (caps.bassBoostSupported) {
                backend.setBassBoost(saved.bassBoostEnabled, saved.bassBoostStrength)
            }

            _state.value = AudioEffectsState(
                isSessionAttached = audioSessionId != AudioManager.AUDIO_SESSION_ID_GENERATE,
                isEqualizerSupported = caps.equalizerSupported,
                eqUnsupportedReason = caps.equalizerUnsupportedReason,
                isEqualizerEnabled = saved.equalizerEnabled,
                bandCount = bandCount,
                bandCenterFreqHz = caps.bandCenterFreqHz,
                minBandLevelMb = caps.minBandLevelMb,
                maxBandLevelMb = caps.maxBandLevelMb,
                bandLevelsMb = resolvedLevels,
                presetNames = caps.presetNames,
                selectedPresetIndex = saved.equalizerPresetIndex.takeIf { it in caps.presetNames.indices } ?: -1,
                isBassBoostSupported = caps.bassBoostSupported,
                bassBoostUnsupportedReason = caps.bassBoostUnsupportedReason,
                isBassBoostEnabled = saved.bassBoostEnabled,
                bassBoostStrength = saved.bassBoostStrength,
                gaplessEnabled = saved.gaplessEnabled,
                crossfadeDurationSec = saved.crossfadeDurationSec,
            )
        }
    }

    fun detachFromAudioSession() {
        currentSessionId = AudioManager.AUDIO_SESSION_ID_GENERATE
        backend.release()
        _state.update {
            it.copy(
                isSessionAttached = false,
                isEqualizerSupported = false,
                isBassBoostSupported = false,
            )
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        if (!_state.value.isEqualizerSupported) return
        backend.setEqualizerEnabled(enabled)
        _state.update { it.copy(isEqualizerEnabled = enabled) }
        scope.launch { preferences.setEqualizerEnabled(enabled) }
    }

    fun selectEqualizerPreset(presetIndex: Int) {
        val current = _state.value
        if (!current.isEqualizerSupported || presetIndex !in current.presetNames.indices) return
        val newLevels = backend.usePreset(presetIndex) ?: return
        _state.update {
            it.copy(
                selectedPresetIndex = presetIndex,
                bandLevelsMb = newLevels,
            )
        }
        scope.launch { preferences.setEqualizerPreset(presetIndex, newLevels) }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val current = _state.value
        if (!current.isEqualizerSupported || bandIndex !in current.bandLevelsMb.indices) return
        val clamped = levelMb.coerceIn(current.minBandLevelMb, current.maxBandLevelMb)
        backend.setBandLevel(bandIndex, clamped)
        val updatedLevels = current.bandLevelsMb.toMutableList().apply { set(bandIndex, clamped) }
        _state.update {
            it.copy(
                selectedPresetIndex = -1,
                bandLevelsMb = updatedLevels,
            )
        }
        scope.launch { preferences.setEqualizerPreset(-1, updatedLevels) }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        val current = _state.value
        if (!current.isBassBoostSupported) return
        backend.setBassBoost(enabled, current.bassBoostStrength)
        _state.update { it.copy(isBassBoostEnabled = enabled) }
        scope.launch { preferences.setBassBoost(enabled, current.bassBoostStrength) }
    }

    fun setBassBoostStrength(strength: Int) {
        val current = _state.value
        if (!current.isBassBoostSupported) return
        val clamped = strength.coerceIn(0, 1000)
        backend.setBassBoost(current.isBassBoostEnabled, clamped)
        _state.update { it.copy(bassBoostStrength = clamped) }
        scope.launch { preferences.setBassBoost(current.isBassBoostEnabled, clamped) }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        _state.update { it.copy(gaplessEnabled = enabled) }
        scope.launch { preferences.setGaplessEnabled(enabled) }
    }

    fun setCrossfadeDurationSec(seconds: Int) {
        val clamped = seconds.coerceIn(0, 10)
        _state.update { it.copy(crossfadeDurationSec = clamped) }
        scope.launch { preferences.setCrossfadeDurationSec(clamped) }
    }
}
