package com.soniq.player.core.playback

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SoniqAudioEffectsManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun supportedBackendAppliesPresetsBandsBassBoostCrossfadeAndDetachesCleanly() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "effects_supported.preferences_pb") },
        )
        val prefs = PlaybackPreferencesDataSource(dataStore)
        val backend = FakeSupportedBackend()
        val manager = SoniqAudioEffectsManager(prefs, backend, Dispatchers.Unconfined)

        manager.attachToAudioSession(42)
        val attached = manager.state.first { it.isSessionAttached }
        assertThat(attached.isEqualizerSupported).isTrue()
        assertThat(attached.isBassBoostSupported).isTrue()
        assertThat(attached.bandCenterFreqHz).containsExactly(60, 230, 910, 3600, 14000).inOrder()
        assertThat(attached.presetNames).containsExactly("Normal", "Rock").inOrder()

        manager.setEqualizerEnabled(true)
        assertThat(backend.eqEnabled).isTrue()

        manager.selectEqualizerPreset(1)
        assertThat(manager.state.value.selectedPresetIndex).isEqualTo(1)
        assertThat(manager.state.value.bandLevelsMb).containsExactly(400, 200, 0, 200, 400).inOrder()

        // Adjusting an individual band switches preset to Custom (-1) and clamps to range.
        manager.setBandLevel(0, 9999)
        assertThat(manager.state.value.selectedPresetIndex).isEqualTo(-1)
        assertThat(manager.state.value.bandLevelsMb[0]).isEqualTo(1500)

        manager.setBassBoostEnabled(true)
        manager.setBassBoostStrength(750)
        assertThat(backend.bbEnabled).isTrue()
        assertThat(backend.bbStrength).isEqualTo(750)

        manager.setGaplessEnabled(false)
        manager.setCrossfadeDurationSec(4)
        assertThat(manager.state.value.gaplessEnabled).isFalse()
        assertThat(manager.state.value.crossfadeDurationSec).isEqualTo(4)

        manager.detachFromAudioSession()
        assertThat(backend.released).isTrue()
        assertThat(manager.state.value.isSessionAttached).isFalse()
    }

    @Test
    fun unsupportedBackendReportsReasonsAndIgnoresMutationsSafely() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "effects_unsupported.preferences_pb") },
        )
        val prefs = PlaybackPreferencesDataSource(dataStore)
        val backend = object : AudioEffectsBackend {
            override fun attach(audioSessionId: Int) = AttachedEffectsCapabilities(
                equalizerSupported = false,
                equalizerUnsupportedReason = "Hardware EQ unavailable on USB DAC.",
                bassBoostSupported = false,
                bassBoostUnsupportedReason = "Bass Boost unavailable.",
            )
            override fun setEqualizerEnabled(enabled: Boolean) = error("Should not be called")
            override fun setBandLevel(bandIndex: Int, levelMb: Int) = error("Should not be called")
            override fun usePreset(presetIndex: Int): List<Int>? = error("Should not be called")
            override fun setBassBoost(enabled: Boolean, strength: Int) = error("Should not be called")
            override fun release() = Unit
        }
        val manager = SoniqAudioEffectsManager(prefs, backend, Dispatchers.Unconfined)
        manager.attachToAudioSession(99)

        val state = manager.state.first { it.isSessionAttached }
        assertThat(state.isEqualizerSupported).isFalse()
        assertThat(state.eqUnsupportedReason).isEqualTo("Hardware EQ unavailable on USB DAC.")
        assertThat(state.isBassBoostSupported).isFalse()
        assertThat(state.bassBoostUnsupportedReason).isEqualTo("Bass Boost unavailable.")

        // Should safely no-op without throwing.
        manager.setEqualizerEnabled(true)
        manager.setBassBoostEnabled(true)
        manager.setBassBoostStrength(500)
        assertThat(manager.state.value.isEqualizerSupported).isFalse()
    }

    private class FakeSupportedBackend : AudioEffectsBackend {
        var eqEnabled = false
        var bbEnabled = false
        var bbStrength = 0
        var released = false
        private val levels = mutableListOf(0, 0, 0, 0, 0)

        override fun attach(audioSessionId: Int) = AttachedEffectsCapabilities(
            equalizerSupported = true,
            bandCenterFreqHz = listOf(60, 230, 910, 3600, 14000),
            minBandLevelMb = -1500,
            maxBandLevelMb = 1500,
            defaultBandLevelsMb = listOf(0, 0, 0, 0, 0),
            presetNames = listOf("Normal", "Rock"),
            bassBoostSupported = true,
        )

        override fun setEqualizerEnabled(enabled: Boolean) {
            eqEnabled = enabled
        }

        override fun setBandLevel(bandIndex: Int, levelMb: Int) {
            levels[bandIndex] = levelMb
        }

        override fun usePreset(presetIndex: Int): List<Int> {
            val preset = if (presetIndex == 1) listOf(400, 200, 0, 200, 400) else listOf(0, 0, 0, 0, 0)
            for (i in levels.indices) levels[i] = preset[i]
            return preset
        }

        override fun setBassBoost(enabled: Boolean, strength: Int) {
            bbEnabled = enabled
            bbStrength = strength
        }

        override fun release() {
            released = true
        }
    }
}
