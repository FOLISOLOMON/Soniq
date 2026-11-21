package com.soniq.player

import com.google.common.truth.Truth.assertThat
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.ThemeMode
import com.soniq.player.core.model.appearance.ThemePreset
import com.soniq.player.core.theme.domain.AppearanceRepository
import com.soniq.player.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun transitionsFromLoadingToReadyEagerlyWithoutActiveUiCollector() = runTest {
        val appearanceFlow = MutableSharedFlow<AppearanceConfig>(replay = 0)
        val repository = object : AppearanceRepository {
            override val appearance: Flow<AppearanceConfig> = appearanceFlow
            override val presets: Flow<List<ThemePreset>> = emptyFlow()
            override suspend fun updateAppearance(transform: (AppearanceConfig) -> AppearanceConfig) = Unit
            override suspend fun saveCurrentAsPreset(name: String): Long = 0L
            override suspend fun applyPreset(id: Long): Boolean = false
            override suspend fun renamePreset(id: Long, newName: String): Boolean = false
            override suspend fun duplicatePreset(id: Long): Long? = null
            override suspend fun installStarterPresets(): Int = 0
            override suspend fun deletePreset(id: Long) = Unit
        }

        val coordinator = LaunchEntranceCoordinator()
        val viewModel = MainViewModel(repository, coordinator)

        // Starts in Loading so splash screen stays up until persisted appearance emits.
        assertThat(viewModel.uiState.value).isEqualTo(MainUiState.Loading)
        assertThat(viewModel.shouldShowLaunchEntrance.value).isTrue()

        // Even with zero collectors attached to uiState, eager sharing delivers Ready immediately.
        val expected = AppearanceConfig(themeMode = ThemeMode.DARK, useAmoledBlack = true)
        appearanceFlow.emit(expected)
        assertThat(viewModel.uiState.value).isEqualTo(MainUiState.Ready(expected))

        // Completing the entrance persists across recreated ViewModels in the same app process.
        viewModel.onLaunchEntranceFinished()
        assertThat(viewModel.shouldShowLaunchEntrance.value).isFalse()

        val recreatedViewModel = MainViewModel(repository, coordinator)
        assertThat(recreatedViewModel.shouldShowLaunchEntrance.value).isFalse()
    }
}
