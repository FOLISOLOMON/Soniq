package com.soniq.player.core.playback

/**
 * Immutable snapshot of SONIQ's audio processing capabilities and active settings.
 *
 * Clearly distinguishes hardware-supported effects from unsupported device audio outputs so the
 * UI never renders fake or non-functional sliders.
 */
data class AudioEffectsState(
    val isSessionAttached: Boolean = false,
    val isEqualizerSupported: Boolean = false,
    val eqUnsupportedReason: String? = null,
    val isEqualizerEnabled: Boolean = false,
    val bandCount: Int = 0,
    val bandCenterFreqHz: List<Int> = emptyList(),
    val minBandLevelMb: Int = -1500,
    val maxBandLevelMb: Int = 1500,
    val bandLevelsMb: List<Int> = emptyList(),
    val presetNames: List<String> = emptyList(),
    val selectedPresetIndex: Int = -1,
    val isBassBoostSupported: Boolean = false,
    val bassBoostUnsupportedReason: String? = null,
    val isBassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val gaplessEnabled: Boolean = true,
    val crossfadeDurationSec: Int = 0,
) {
    companion object {
        val CROSSFADE_OPTIONS_SEC = listOf(0, 2, 4, 6, 8, 10)
    }
}
