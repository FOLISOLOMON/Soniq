package com.soniq.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniq.player.core.common.suspendRunCatching
import com.soniq.player.core.datastore.PlaybackPreferences
import com.soniq.player.core.datastore.PlaybackPreferencesDataSource
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.BackgroundStyle
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.appearance.TextScale
import com.soniq.player.core.model.appearance.ThemeMode
import com.soniq.player.core.model.appearance.ThemePreset
import com.soniq.player.core.permissions.NotificationPermissionChecker
import com.soniq.player.core.permissions.NotificationPermissions
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
    val continueVideoInBackground: Boolean = false,
    val showNotificationPermissionPrompt: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appearanceRepository: AppearanceRepository,
    private val playbackPreferences: PlaybackPreferencesDataSource,
    private val notificationPermissionChecker: NotificationPermissionChecker,
) : ViewModel() {

    private val presetNameDraft = MutableStateFlow("")
    private val feedback = MutableStateFlow<SettingsFeedback?>(null)
    private val notificationsGranted = MutableStateFlow(notificationPermissionChecker.canPostNotifications())

    val uiState: StateFlow<SettingsUiState> = combine(
        appearanceRepository.appearance,
        appearanceRepository.presets,
        presetNameDraft,
        feedback,
        playbackPreferences.preferences,
        notificationsGranted,
    ) { values ->
        val appearance = values[0] as AppearanceConfig
        @Suppress("UNCHECKED_CAST")
        val presets = values[1] as List<ThemePreset>
        val draft = values[2] as String
        val message = values[3] as SettingsFeedback?
        val playbackPrefs = values[4] as PlaybackPreferences
        val canPostNotifications = values[5] as Boolean

        SettingsUiState(
            appearance = appearance,
            presets = presets,
            presetNameDraft = draft,
            feedback = message,
            continueVideoInBackground = playbackPrefs.continueVideoInBackground,
            showNotificationPermissionPrompt =
                NotificationPermissions.isRuntimePermissionRequired() && !canPostNotifications,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    fun refreshNotificationPermission() {
        notificationsGranted.value = notificationPermissionChecker.canPostNotifications()
    }

    fun setContinueVideoInBackground(enabled: Boolean) {
        viewModelScope.launch {
            suspendRunCatching { playbackPreferences.setContinueVideoInBackground(enabled) }
                .onFailure { feedback.value = SettingsFeedback.PRESET_ERROR }
        }
    }

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

    fun clearBackgroundImage() = update {
        it.copy(
            backgroundStyle = BackgroundStyle.SOLID,
            backgroundImageUri = null,
        )
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

    fun renamePreset(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            feedback.value = SettingsFeedback.PRESET_NAME_INVALID
            return
        }
        viewModelScope.launch {
            suspendRunCatching { appearanceRepository.renamePreset(id, trimmed) }
                .onSuccess { renamed ->
                    if (!renamed) feedback.value = SettingsFeedback.PRESET_ERROR
                }
                .onFailure {
                    feedback.value = SettingsFeedback.PRESET_ERROR
                }
        }
    }

    fun duplicatePreset(id: Long) {
        viewModelScope.launch {
            suspendRunCatching { appearanceRepository.duplicatePreset(id) }
                .onSuccess { newId ->
                    feedback.value = if (newId != null) {
                        SettingsFeedback.PRESET_SAVED
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
