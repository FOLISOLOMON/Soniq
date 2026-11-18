package com.soniq.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniq.player.core.common.suspendRunCatching
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.BackgroundStyle
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.appearance.TextScale
import com.soniq.player.core.model.appearance.ThemeMode
import com.soniq.player.core.model.appearance.ThemePreset
import com.soniq.player.core.theme.data.DefaultAppearanceRepository
import com.soniq.player.core.theme.domain.AppearanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot user feedback emitted by [SettingsViewModel] and surfaced in a snackbar. */
enum class SettingsFeedback {
    PRESET_SAVED,
    PRESET_APPLIED,
    PRESET_NAME_INVALID,
    PRESET_ERROR,
    BACKGROUND_IMAGE_ERROR,
}

/** Unidirectional UI state for the Settings destination. */
data class SettingsUiState(
    val appearance: AppearanceConfig = AppearanceConfig(),
    val presets: List<ThemePreset> = emptyList(),
    val presetNameDraft: String = "",
    val feedback: SettingsFeedback? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appearanceRepository: AppearanceRepository,
) : ViewModel() {

    private val presetNameDraft = MutableStateFlow("")
    private val feedback = MutableStateFlow<SettingsFeedback?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        appearanceRepository.appearance,
        appearanceRepository.presets,
        presetNameDraft,
        feedback,
    ) { appearance, presets, draft, message ->
        SettingsUiState(
            appearance = appearance,
            presets = presets,
            presetNameDraft = draft,
            feedback = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setAmoledBlack(enabled: Boolean) = update { it.copy(useAmoledBlack = enabled) }

    fun setDynamicColor(enabled: Boolean) = update { it.copy(useDynamicColor = enabled) }

    fun setAccentColor(argb: Int) = update {
        it.copy(accentColorArgb = argb, useDynamicColor = false)
    }

    fun setFontFamily(family: FontFamilyOption) = update { it.copy(fontFamily = family) }

    fun setTextScale(scale: TextScale) = update { it.copy(textScale = scale) }

    fun setBackgroundStyle(style: BackgroundStyle) = update { it.copy(backgroundStyle = style) }

    fun setBackgroundImage(uri: String) = update {
        it.copy(
            backgroundStyle = BackgroundStyle.IMAGE,
            backgroundImageUri = uri,
        )
    }

    fun onBackgroundImageAccessError() {
        feedback.value = SettingsFeedback.BACKGROUND_IMAGE_ERROR
    }

    fun setPlayerLayout(layout: PlayerLayout) = update { it.copy(playerLayout = layout) }

    fun onPresetNameChanged(value: String) {
        presetNameDraft.value = value.take(DefaultAppearanceRepository.MAX_PRESET_NAME_LENGTH)
    }

    fun savePreset() {
        val name = presetNameDraft.value.trim()
        if (name.isEmpty()) {
            feedback.value = SettingsFeedback.PRESET_NAME_INVALID
            return
        }
        viewModelScope.launch {
            suspendRunCatching { appearanceRepository.saveCurrentAsPreset(name) }
                .onSuccess {
                    presetNameDraft.value = ""
                    feedback.value = SettingsFeedback.PRESET_SAVED
                }
                .onFailure {
                    feedback.value = SettingsFeedback.PRESET_ERROR
                }
        }
    }

    fun applyPreset(id: Long) {
        viewModelScope.launch {
            suspendRunCatching { appearanceRepository.applyPreset(id) }
                .onSuccess { applied ->
                    feedback.value = if (applied) {
                        SettingsFeedback.PRESET_APPLIED
                    } else {
                        SettingsFeedback.PRESET_ERROR
                    }
                }
                .onFailure {
                    feedback.value = SettingsFeedback.PRESET_ERROR
                }
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            suspendRunCatching { appearanceRepository.deletePreset(id) }
                .onFailure {
                    feedback.value = SettingsFeedback.PRESET_ERROR
                }
        }
    }

    fun onFeedbackDismissed() {
        feedback.value = null
    }

    private fun update(transform: (AppearanceConfig) -> AppearanceConfig) {
        viewModelScope.launch {
            suspendRunCatching { appearanceRepository.updateAppearance(transform) }
                .onFailure {
                    feedback.value = SettingsFeedback.PRESET_ERROR
                }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
