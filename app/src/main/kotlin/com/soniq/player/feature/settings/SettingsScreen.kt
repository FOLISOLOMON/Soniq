package com.soniq.player.feature.settings

import android.content.Intent
import android.os.Build
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soniq.player.R
import com.soniq.player.core.designsystem.component.SoniqGlassDialog
import com.soniq.player.core.designsystem.component.SoniqGlassIconButton
import com.soniq.player.core.designsystem.component.SoniqGlassPillChip
import com.soniq.player.core.designsystem.component.SoniqScaffold
import com.soniq.player.core.designsystem.component.SoniqSectionHeader
import com.soniq.player.core.designsystem.component.SoniqSegmentedChoice
import com.soniq.player.core.designsystem.component.SoniqSurfaceCard
import com.soniq.player.core.designsystem.component.SoniqTopAppBar
import com.soniq.player.core.designsystem.theme.ColorMath
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.designsystem.theme.isDynamicColorSupported
import com.soniq.player.core.designsystem.token.SoniqAccents
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.BackgroundStyle
import com.soniq.player.core.model.appearance.FontFamilyOption
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.model.appearance.TextScale
import com.soniq.player.core.model.appearance.ThemeMode
import com.soniq.player.core.model.appearance.ThemePreset
import com.soniq.player.core.permissions.NotificationPermissions

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

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.refreshNotificationPermission()
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshNotificationPermission()
        onPauseOrDispose {}
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
        onClearBackgroundImage = viewModel::clearBackgroundImage,
        onBackgroundScrimChanged = viewModel::setBackgroundScrimAlpha,
        onBackgroundBlurChanged = viewModel::setBackgroundBlurDp,
        onPlayerLayoutSelected = viewModel::setPlayerLayout,
        onContinueVideoInBackgroundToggled = viewModel::setContinueVideoInBackground,
        onRequestNotificationPermission = {
            NotificationPermissions.requiredPermission()?.let(notificationLauncher::launch)
        },
        onPresetNameChanged = viewModel::onPresetNameChanged,
        onSavePreset = viewModel::savePreset,
        onLoadStarterPresets = viewModel::installStarterPresets,
        onApplyPreset = viewModel::applyPreset,
        onRenamePreset = viewModel::renamePreset,
        onDuplicatePreset = viewModel::duplicatePreset,
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
    onClearBackgroundImage: () -> Unit = {},
    onBackgroundScrimChanged: (Float) -> Unit = {},
    onBackgroundBlurChanged: (Int) -> Unit = {},
    onLoadStarterPresets: () -> Unit = {},
    onRenamePreset: (Long, String) -> Unit = { _, _ -> },
    onDuplicatePreset: (Long) -> Unit = {},
    onContinueVideoInBackgroundToggled: (Boolean) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
) {
    val context = LocalContext.current
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    val border = SoniqTheme.tokens.border
    val snackbarHostState = remember { SnackbarHostState() }
    val feedbackText = uiState.feedback?.let { stringResource(it.messageRes()) }
    var showCustomAccentDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ThemePreset?>(null) }

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
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                bottom = spacing.xxxl + spacing.xl,
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
                        OutlinedButton(onClick = { showCustomAccentDialog = true }) {
                            Text(text = stringResource(R.string.settings_custom_accent_button))
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
                        val imageUri = appearance.backgroundImageUri
                        val isImageReadable = remember(imageUri) {
                            if (imageUri.isNullOrBlank()) {
                                true
                            } else {
                                runCatching {
                                    context.contentResolver.openAssetFileDescriptor(imageUri.toUri(), "r")?.use { true } ?: false
                                }.getOrDefault(false)
                            }
                        }
                        if (!imageUri.isNullOrBlank() && !isImageReadable) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_background_broken),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = onClearBackgroundImage) {
                                    Text(text = stringResource(R.string.settings_background_clear))
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!imageUri.isNullOrBlank() && isImageReadable) {
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
                        if (!imageUri.isNullOrBlank() && isImageReadable) {
                            HorizontalDivider()
                            Text(
                                text = stringResource(
                                    R.string.settings_background_scrim,
                                    (appearance.backgroundScrimAlpha * 100).toInt(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Slider(
                                value = appearance.backgroundScrimAlpha,
                                onValueChange = onBackgroundScrimChanged,
                                valueRange = AppearanceConfig.MIN_BACKGROUND_SCRIM_ALPHA..AppearanceConfig.MAX_BACKGROUND_SCRIM_ALPHA,
                            )
                            Text(
                                text = stringResource(R.string.settings_background_blur, appearance.backgroundBlurDp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Slider(
                                    value = appearance.backgroundBlurDp.toFloat(),
                                    onValueChange = { onBackgroundBlurChanged(it.toInt()) },
                                    valueRange = 0f..AppearanceConfig.MAX_BACKGROUND_BLUR_DP.toFloat(),
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.settings_background_blur_api31),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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

            item(key = "playback_behavior") {
                SoniqSectionHeader(title = stringResource(R.string.settings_section_playback))
                SoniqSurfaceCard {
                    SettingSwitchRow(
                        title = stringResource(R.string.settings_video_background),
                        summary = stringResource(R.string.settings_video_background_summary),
                        checked = uiState.continueVideoInBackground,
                        onCheckedChange = onContinueVideoInBackgroundToggled,
                    )
                    if (uiState.showNotificationPermissionPrompt) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_notifications_title),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = stringResource(R.string.settings_notifications_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedButton(onClick = onRequestNotificationPermission) {
                                Text(text = stringResource(R.string.settings_notifications_allow))
                            }
                        }
                    }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = onLoadStarterPresets) {
                            Text(text = stringResource(R.string.settings_load_starter_presets))
                        }
                        Button(onClick = onSavePreset) {
                            Text(text = stringResource(R.string.settings_preset_save))
                        }
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
                    onRename = { renameTarget = preset },
                    onDuplicate = { onDuplicatePreset(preset.id) },
                    onDelete = { onDeletePreset(preset.id) },
                )
            }

            item(key = "privacy_offline") {
                SoniqSectionHeader(title = stringResource(R.string.settings_section_privacy))
                SoniqSurfaceCard {
                    Text(
                        text = stringResource(R.string.settings_privacy_offline_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_privacy_offline_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showCustomAccentDialog) {
        CustomAccentColorDialog(
            initialArgb = uiState.appearance.accentColorArgb,
            onDismiss = { showCustomAccentDialog = false },
            onApply = { argb ->
                showCustomAccentDialog = false
                onAccentColorSelected(argb)
            },
        )
    }

    renameTarget?.let { target ->
        RenamePresetDialog(
            initialName = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                renameTarget = null
                onRenamePreset(target.id, newName)
            },
        )
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
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    var menuExpanded by remember { mutableStateOf(false) }

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
            SoniqGlassPillChip(
                label = stringResource(R.string.settings_preset_apply),
                selected = false,
                onClick = onApply,
            )
            Box {
                SoniqGlassIconButton(
                    onClick = { menuExpanded = true },
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.settings_preset_actions, preset.name),
                    size = 40.dp,
                    iconSize = sizes.iconSmall,
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_preset_rename)) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_preset_duplicate)) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_preset_delete, preset.name)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomAccentColorDialog(
    initialArgb: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit,
) {
    val initialHsl = remember(initialArgb) { ColorMath.toHsl(initialArgb) }
    var hue by remember { mutableFloatStateOf(initialHsl.hue) }
    var saturation by remember { mutableFloatStateOf(initialHsl.saturation.coerceIn(0.2f, 1f)) }
    var lightness by remember { mutableFloatStateOf(initialHsl.lightness.coerceIn(0.25f, 0.75f)) }
    val previewArgb = ColorMath.fromHsl(ColorMath.Hsl(hue, saturation, lightness))
    val onPreviewArgb = ColorMath.readableOn(previewArgb)
    val hexText = remember(previewArgb) { "#%06X".format(previewArgb and 0xFFFFFF) }
    val spacing = SoniqTheme.tokens.spacing

    SoniqGlassDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_custom_accent_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Color(previewArgb),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_custom_accent_preview),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(onPreviewArgb),
                        )
                        Text(
                            text = hexText,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(onPreviewArgb),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_custom_accent_hue, hue.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..359f)
                Text(
                    text = stringResource(R.string.settings_custom_accent_saturation, (saturation * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0.1f..1f)
                Text(
                    text = stringResource(R.string.settings_custom_accent_lightness, (lightness * 100).toInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(value = lightness, onValueChange = { lightness = it }, valueRange = 0.2f..0.8f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(previewArgb) }) {
                Text(stringResource(R.string.settings_custom_accent_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.playlist_cancel))
            }
        },
    )
}

@Composable
private fun RenamePresetDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    SoniqGlassDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_preset_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotEmpty(),
            ) {
                Text(stringResource(R.string.settings_preset_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.playlist_cancel))
            }
        },
    )
}

private fun SettingsFeedback.messageRes(): Int = when (this) {
    SettingsFeedback.PRESET_SAVED -> R.string.settings_preset_saved
    SettingsFeedback.PRESET_APPLIED -> R.string.settings_preset_applied
    SettingsFeedback.PRESET_NAME_INVALID -> R.string.settings_preset_name_invalid
    SettingsFeedback.PRESET_ERROR -> R.string.settings_preset_error
    SettingsFeedback.BACKGROUND_IMAGE_ERROR -> R.string.settings_background_image_error
}
