package com.soniq.player.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniq.player.R
import com.soniq.player.core.designsystem.component.SoniqScaffold
import com.soniq.player.core.designsystem.component.SoniqSectionHeader
import com.soniq.player.core.designsystem.component.SoniqSegmentedChoice
import com.soniq.player.core.designsystem.component.SoniqSurfaceCard
import com.soniq.player.core.designsystem.component.SoniqTopAppBar
import com.soniq.player.core.designsystem.theme.ColorMath
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.designsystem.theme.isDynamicColorSupported
import com.soniq.player.core.designsystem.token.SoniqAccents
import com.soniq.player.core.model.appearance.BackgroundStyle
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.appearance.TextScale
import com.soniq.player.core.model.appearance.ThemeMode
import com.soniq.player.core.model.appearance.ThemePreset

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.setBackgroundImage(uri.toString())
        } catch (_: SecurityException) {
            viewModel.onBackgroundImageAccessError()
        }
    }

    SettingsScreen(
        uiState = uiState,
        dynamicColorSupported = isDynamicColorSupported,
        onThemeModeSelected = viewModel::setThemeMode,
        onAmoledBlackToggled = viewModel::setAmoledBlack,
        onDynamicColorToggled = viewModel::setDynamicColor,
        onAccentColorSelected = viewModel::setAccentColor,
        onFontFamilySelected = viewModel::setFontFamily,
        onTextScaleSelected = viewModel::setTextScale,
        onBackgroundStyleSelected = viewModel::setBackgroundStyle,
        onChooseBackgroundImage = { imagePicker.launch(arrayOf("image/*")) },
        onPlayerLayoutSelected = viewModel::setPlayerLayout,
        onPresetNameChanged = viewModel::onPresetNameChanged,
        onSavePreset = viewModel::savePreset,
        onApplyPreset = viewModel::applyPreset,
        onDeletePreset = viewModel::deletePreset,
        onFeedbackDismissed = viewModel::onFeedbackDismissed,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    dynamicColorSupported: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onAmoledBlackToggled: (Boolean) -> Unit,
    onDynamicColorToggled: (Boolean) -> Unit,
    onAccentColorSelected: (Int) -> Unit,
    onFontFamilySelected: (FontFamilyOption) -> Unit,
    onTextScaleSelected: (TextScale) -> Unit,
    onBackgroundStyleSelected: (BackgroundStyle) -> Unit,
    onChooseBackgroundImage: () -> Unit,
    onPlayerLayoutSelected: (PlayerLayout) -> Unit,
    onPresetNameChanged: (String) -> Unit,
    onSavePreset: () -> Unit,
    onApplyPreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onFeedbackDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    val border = SoniqTheme.tokens.border
    val snackbarHostState = remember { SnackbarHostState() }
    val feedbackText = uiState.feedback?.let { stringResource(it.messageRes()) }

    LaunchedEffect(uiState.feedback) {
        if (feedbackText != null) {
            snackbarHostState.showSnackbar(feedbackText)
            onFeedbackDismissed()
        }
    }

    SoniqScaffold(
        modifier = modifier,
        topBar = { SoniqTopAppBar(title = stringResource(R.string.destination_settings)) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val appearance = uiState.appearance
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                bottom = spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item(key = "appearance") {
                SoniqSectionHeader(title = stringResource(R.string.settings_section_appearance))
                SoniqSurfaceCard {
                    Text(
                        text = stringResource(R.string.settings_theme_mode),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SoniqSegmentedChoice(
                        options = ThemeMode.entries,
                        selected = appearance.themeMode,
                        labelFor = { mode ->
                            when (mode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                            }
                        },
                        onSelect = onThemeModeSelected,
                    )
                    HorizontalDivider()
                    SettingSwitchRow(
                        title = stringResource(R.string.settings_amoled),
                        summary = stringResource(R.string.settings_amoled_summary),
                        checked = appearance.useAmoledBlack,
                        onCheckedChange = onAmoledBlackToggled,
                    )
                    HorizontalDivider()
                    SettingSwitchRow(
                        title = stringResource(R.string.settings_dynamic_color),
                        summary = stringResource(
                            if (dynamicColorSupported) {
                                R.string.settings_dynamic_color_summary
                            } else {
                                R.string.settings_dynamic_color_unavailable
                            },
                        ),
                        checked = appearance.useDynamicColor && dynamicColorSupported,
                        enabled = dynamicColorSupported,
                        onCheckedChange = onDynamicColorToggled,
                    )
                }
            }

            item(key = "accent") {
                SoniqSectionHeader(title = stringResource(R.string.settings_section_accent))
                SoniqSurfaceCard {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        SoniqAccents.all.forEach { swatch ->
                            val isSelected = !appearance.useDynamicColor &&
                                appearance.accentColorArgb == swatch.argb
                            val swatchLabel = stringResource(R.string.settings_accent_swatch, swatch.name)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(sizes.accentSwatch)
                                    .clip(CircleShape)
                                    .background(swatch.color)
                                    .border(
                                        width = if (isSelected) border.selected else border.thin,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = CircleShape,
                                    )
                                    .semantics {
                                        contentDescription = swatchLabel
                                        selected = isSelected
                                    }
                                    .clickable(role = Role.RadioButton) {
                                        onAccentColorSelected(swatch.argb)
                                    },
                            ) {
                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = stringResource(R.string.cd_selected),
                                        tint = Color(ColorMath.readableOn(swatch.argb)),
                                        modifier = Modifier.size(sizes.iconSmall),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "typography") {
                SoniqSectionHeader(title = stringResource(R.string.settings_section_typography))
                SoniqSurfaceCard {
                    Text(
                        text = stringResource(R.string.settings_font_family),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SoniqSegmentedChoice(
                        options = FontFamilyOption.entries,
                        selected = appearance.fontFamily,
                        labelFor = { option ->
                            when (option) {
                                FontFamilyOption.SYSTEM -> stringResource(R.string.settings_font_system)
                                FontFamilyOption.SANS_SERIF -> stringResource(R.string.settings_font_sans)
                                FontFamilyOption.SERIF -> stringResource(R.string.settings_font_serif)
                                FontFamilyOption.MONOSPACE -> stringResource(R.string.settings_font_mono)
                            }
                        },
                        onSelect = onFontFamilySelected,
                    )
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.settings_text_size),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SoniqSegmentedChoice(
                        options = TextScale.entries,
                        selected = appearance.textScale,
                        labelFor = { scale ->
                            when (scale) {
                                TextScale.SMALL -> stringResource(R.string.settings_text_small)
                                TextScale.DEFAULT -> stringResource(R.string.settings_text_default)
                                TextScale.LARGE -> stringResource(R.string.settings_text_large)
                            }
                        },
                        onSelect = onTextScaleSelected,
                    )
                }
            }

            item(key = "background") {
                SoniqSectionHeader(title = stringResource(R.string.settings_section_background))
                SoniqSurfaceCard {
                    SoniqSegmentedChoice(
                        options = BackgroundStyle.entries,
                        selected = appearance.backgroundStyle,
                        labelFor = { style ->
                            when (style) {
                                BackgroundStyle.SOLID -> stringResource(R.string.settings_background_solid)
                                BackgroundStyle.GRADIENT -> stringResource(R.string.settings_background_gradient)
                                BackgroundStyle.IMAGE -> stringResource(R.string.settings_background_image)
                            }
                        },
                        onSelect = onBackgroundStyleSelected,
                    )
                    if (appearance.backgroundStyle == BackgroundStyle.IMAGE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!appearance.backgroundImageUri.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.settings_background_image_selected),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            OutlinedButton(onClick = onChooseBackgroundImage) {
                                Text(text = stringResource(R.string.settings_background_choose_image))
                            }
                        }
                    }
                }
            }

            item(key = "player_layout") {
                SoniqSectionHeader(title = stringResource(R.string.settings_section_player))
                SoniqSurfaceCard {
                    SoniqSegmentedChoice(
                        options = PlayerLayout.entries,
                        selected = appearance.playerLayout,
                        labelFor = { layout ->
                            when (layout) {
                                PlayerLayout.CLASSIC -> stringResource(R.string.settings_player_classic)
                                PlayerLayout.ARTWORK_FOCUS -> stringResource(R.string.settings_player_artwork)
                                PlayerLayout.COMPACT -> stringResource(R.string.settings_player_compact)
                            }
                        },
                        onSelect = onPlayerLayoutSelected,
                    )
                }
            }

            item(key = "presets_header") {
                SoniqSectionHeader(title = stringResource(R.string.settings_section_presets))
                SoniqSurfaceCard {
                    OutlinedTextField(
                        value = uiState.presetNameDraft,
                        onValueChange = onPresetNameChanged,
                        label = { Text(text = stringResource(R.string.settings_preset_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = onSavePreset,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(text = stringResource(R.string.settings_preset_save))
                    }
                    if (uiState.presets.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_presets_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(items = uiState.presets, key = { preset -> "preset_${preset.id}" }) { preset ->
                PresetRowCard(
                    preset = preset,
                    onApply = { onApplyPreset(preset.id) },
                    onDelete = { onDeletePreset(preset.id) },
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val spacing = SoniqTheme.tokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = SoniqTheme.tokens.alpha.disabled)
                },
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun PresetRowCard(
    preset: ThemePreset,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    SoniqSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(sizes.icon)
                    .clip(CircleShape)
                    .background(Color(preset.config.accentColorArgb)),
            )
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = onApply) {
                Text(text = stringResource(R.string.settings_preset_apply))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.settings_preset_delete, preset.name),
                )
            }
        }
    }
}

private fun SettingsFeedback.messageRes(): Int = when (this) {
    SettingsFeedback.PRESET_SAVED -> R.string.settings_preset_saved
    SettingsFeedback.PRESET_APPLIED -> R.string.settings_preset_applied
    SettingsFeedback.PRESET_NAME_INVALID -> R.string.settings_preset_name_invalid
    SettingsFeedback.PRESET_ERROR -> R.string.settings_preset_error
    SettingsFeedback.BACKGROUND_IMAGE_ERROR -> R.string.settings_background_image_error
}
