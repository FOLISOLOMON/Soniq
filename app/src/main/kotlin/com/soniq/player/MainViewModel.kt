package com.soniq.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.theme.domain.AppearanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Activity-level UI state keeping the splash screen up until persisted appearance loads. */
sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val appearance: AppearanceConfig) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    appearanceRepository: AppearanceRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = appearanceRepository.appearance
        .map<AppearanceConfig, MainUiState> { MainUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MainUiState.Loading,
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
