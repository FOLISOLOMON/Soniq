package com.soniq.player.feature.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import com.soniq.player.core.designsystem.token.SoniqAccents
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.ThemeMode
import com.soniq.player.core.model.appearance.ThemePreset
import com.soniq.player.core.permissions.NotificationPermissionChecker
import com.soniq.player.core.theme.domain.AppearanceRepository
import com.soniq.player.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun updatesAppearancePlaybackPrefsAndManagesPresetsWithFeedback() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.newFolder(), "settings_playback.preferences_pb") },
        )
        val repository = InMemoryAppearanceRepository()
        val playbackPrefs = PlaybackPreferencesDataSource(dataStore)
        val notifChecker = object : NotificationPermissionChecker {
            override fun canPostNotifications(): Boolean = true
        }
        val viewModel = SettingsViewModel(repository, playbackPrefs, notifChecker)

        viewModel.uiState.test {
            val initial = awaitItem()
            assertThat(initial.appearance).isEqualTo(AppearanceConfig())
            assertThat(initial.continueVideoInBackground).isFalse()

            viewModel.setContinueVideoInBackground(true)
            assertThat(awaitItem().continueVideoInBackground).isTrue()

            viewModel.setDynamicColor(true)
            assertThat(awaitItem().appearance.useDynamicColor).isTrue()

            // Choosing a custom accent turns dynamic color off so the accent actually takes effect.
            viewModel.setAccentColor(SoniqAccents.Emerald.argb)
            val accented = awaitItem()
            assertThat(accented.appearance.accentColorArgb).isEqualTo(SoniqAccents.Emerald.argb)
            assertThat(accented.appearance.useDynamicColor).isFalse()

            viewModel.setThemeMode(ThemeMode.DARK)
            assertThat(awaitItem().appearance.themeMode).isEqualTo(ThemeMode.DARK)

            // Saving with a blank name emits validation feedback.
            viewModel.savePreset()
            assertThat(awaitItem().feedback).isEqualTo(SettingsFeedback.PRESET_NAME_INVALID)
            viewModel.onFeedbackDismissed()
            assertThat(awaitItem().feedback).isNull()

            // Saving with a valid name persists a preset, clears draft, and emits confirmation.
            viewModel.onPresetNameChanged("Emerald Night")
            assertThat(awaitItem().presetNameDraft).isEqualTo("Emerald Night")

            viewModel.savePreset()
            val savedState = expectMostRecentItem()
            assertThat(savedState.presets).hasSize(1)
            assertThat(savedState.presets.first().name).isEqualTo("Emerald Night")
            assertThat(savedState.presetNameDraft).isEmpty()
            assertThat(savedState.feedback).isEqualTo(SettingsFeedback.PRESET_SAVED)

            val presetId = savedState.presets.first().id
            viewModel.renamePreset(presetId, "Emerald Pro")
            assertThat(awaitItem().presets.first().name).isEqualTo("Emerald Pro")

            viewModel.duplicatePreset(presetId)
            val duplicated = expectMostRecentItem()
            assertThat(duplicated.presets).hasSize(2)

            viewModel.setBackgroundImage("content://broken/img")
            assertThat(awaitItem().appearance.backgroundImageUri).isEqualTo("content://broken/img")
            viewModel.setBackgroundScrimAlpha(0.5f)
            assertThat(awaitItem().appearance.backgroundScrimAlpha).isWithin(0.01f).of(0.5f)
            viewModel.setBackgroundBlurDp(16)
            assertThat(awaitItem().appearance.backgroundBlurDp).isEqualTo(16)
            viewModel.clearBackgroundImage()
            assertThat(awaitItem().appearance.backgroundImageUri).isNull()

            viewModel.installStarterPresets()
            assertThat(expectMostRecentItem().presets.size).isGreaterThan(2)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private class InMemoryAppearanceRepository : AppearanceRepository {
        private val _appearance = MutableStateFlow(AppearanceConfig())
        private val _presets = MutableStateFlow<List<ThemePreset>>(emptyList())
        private var nextId = 1L

        override val appearance: Flow<AppearanceConfig> = _appearance.asStateFlow()
        override val presets: Flow<List<ThemePreset>> = _presets.asStateFlow()

        override suspend fun updateAppearance(transform: (AppearanceConfig) -> AppearanceConfig) {
            _appearance.update(transform)
        }

        override suspend fun saveCurrentAsPreset(name: String): Long {
            val id = nextId++
            val preset = ThemePreset(
                id = id,
                name = name.trim(),
                config = _appearance.value,
                createdAtEpochMillis = id * 1_000L,
            )
            _presets.update { listOf(preset) + it }
            return id
        }

        override suspend fun applyPreset(id: Long): Boolean {
            val match = _presets.value.firstOrNull { it.id == id } ?: return false
            _appearance.value = match.config
            return true
        }

        override suspend fun renamePreset(id: Long, newName: String): Boolean {
            var found = false
            _presets.update { list ->
                list.map {
                    if (it.id == id) {
                        found = true
                        it.copy(name = newName.trim())
                    } else {
                        it
                    }
                }
            }
            return found
        }

        override suspend fun duplicatePreset(id: Long): Long? {
            val match = _presets.value.firstOrNull { it.id == id } ?: return null
            val copyId = nextId++
            _presets.update { listOf(match.copy(id = copyId, name = "${match.name} Copy")) + it }
            return copyId
        }

        override suspend fun installStarterPresets(): Int {
            val id = nextId++
            _presets.update { listOf(ThemePreset(id, "SONIQ Violet", AppearanceConfig(), id * 1_000L)) + it }
            return 1
        }

        override suspend fun deletePreset(id: Long) {
            _presets.update { list -> list.filterNot { it.id == id } }
        }
    }
}
