package com.soniq.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soniq.player.R
import com.soniq.player.core.common.MediaFormatters
import com.soniq.player.core.designsystem.component.SoniqGlassCard
import com.soniq.player.core.designsystem.component.SoniqGlassModalSheet
import com.soniq.player.core.designsystem.component.SoniqGlassPillChip
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.playback.AudioEffectsState
import com.soniq.player.core.playback.SoniqSleepTimerManager
import kotlin.math.roundToInt

/**
 * Material 3 modal sheet for SONIQ's hardware Equalizer, Bass Boost, Gapless, and Crossfade
 * controls. Explicitly surfaces device capability limits when an effect is unsupported.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSheet(
    effects: AudioEffectsState,
    onDismiss: () -> Unit,
    onSetEqualizerEnabled: (Boolean) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onSetBandLevel: (Int, Int) -> Unit,
    onSetBassBoostEnabled: (Boolean) -> Unit,
    onSetBassBoostStrength: (Int) -> Unit,
    onSetGaplessEnabled: (Boolean) -> Unit,
    onSetCrossfadeDurationSec: (Int) -> Unit,
    sleepTimerRemainingMs: Long? = null,
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
) {
    val spacing = SoniqTheme.tokens.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SoniqGlassModalSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = stringResource(R.string.audio_effects_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )

            // Equalizer section (Interactive Frequency Graph + Glass Preset List)
            SoniqGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.audio_effects_equalizer),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Switch(
                        checked = effects.isEqualizerSupported && effects.isEqualizerEnabled,
                        onCheckedChange = onSetEqualizerEnabled,
                        enabled = effects.isEqualizerSupported,
                    )
                }

                if (!effects.isEqualizerSupported) {
                    UnsupportedEffectNotice(
                        reason = effects.eqUnsupportedReason
                            ?: stringResource(R.string.audio_effects_equalizer),
                    )
                } else {
                    EqualizerFrequencyGraph(
                        bandCenterFreqHz = effects.bandCenterFreqHz,
                        bandLevelsMb = effects.bandLevelsMb,
                        minBandLevelMb = effects.minBandLevelMb,
                        maxBandLevelMb = effects.maxBandLevelMb,
                        enabled = effects.isEqualizerEnabled,
                        onBandLevelChanged = onSetBandLevel,
                    )

                    Text(
                        text = stringResource(R.string.audio_effects_presets),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = spacing.xs),
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                        modifier = Modifier.alpha(if (effects.isEqualizerEnabled) 1f else 0.55f),
                    ) {
                        EqualizerPresetRow(
                            label = stringResource(R.string.audio_effects_custom_preset),
                            selected = effects.selectedPresetIndex < 0,
                            enabled = false,
                            onClick = {},
                        )
                        effects.presetNames.forEachIndexed { index, name ->
                            EqualizerPresetRow(
                                label = name,
                                selected = effects.selectedPresetIndex == index,
                                enabled = effects.isEqualizerEnabled,
                                onClick = { onSelectPreset(index) },
                            )
                        }
                    }
                }
            }

            // Bass Boost section
            SoniqGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.audio_effects_bass_boost),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(
                        checked = effects.isBassBoostSupported && effects.isBassBoostEnabled,
                        onCheckedChange = onSetBassBoostEnabled,
                        enabled = effects.isBassBoostSupported,
                    )
                }

                if (!effects.isBassBoostSupported) {
                    UnsupportedEffectNotice(
                        reason = effects.bassBoostUnsupportedReason
                            ?: stringResource(R.string.audio_effects_bass_boost),
                    )
                } else {
                    val percent = (effects.bassBoostStrength / 10f).roundToInt()
                    Text(
                        text = stringResource(R.string.audio_effects_bass_strength, percent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = effects.bassBoostStrength.toFloat(),
                        onValueChange = { value -> onSetBassBoostStrength(value.roundToInt()) },
                        valueRange = 0f..1000f,
                        enabled = effects.isBassBoostEnabled,
                    )
                }
            }

            // Gapless playback & Crossfade transitions
            SoniqGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = spacing.md),
                    ) {
                        Text(
                            text = stringResource(R.string.audio_effects_gapless),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.audio_effects_gapless_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = effects.gaplessEnabled,
                        onCheckedChange = onSetGaplessEnabled,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = stringResource(R.string.audio_effects_crossfade),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.audio_effects_crossfade_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        AudioEffectsState.CROSSFADE_OPTIONS_SEC.forEach { sec ->
                            SoniqGlassPillChip(
                                label = if (sec == 0) {
                                    stringResource(R.string.audio_effects_crossfade_off)
                                } else {
                                    stringResource(R.string.audio_effects_crossfade_sec, sec)
                                },
                                selected = effects.crossfadeDurationSec == sec,
                                onClick = { onSetCrossfadeDurationSec(sec) },
                            )
                        }
                    }
                }
            }

            // Sleep timer
            SoniqGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = stringResource(R.string.sleep_timer_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (sleepTimerRemainingMs != null) {
                        Text(
                            text = stringResource(
                                R.string.sleep_timer_remaining,
                                MediaFormatters.formatDuration(sleepTimerRemainingMs),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = SoniqTheme.glass.scrubberStart,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        SoniqGlassPillChip(
                            label = stringResource(R.string.sleep_timer_off),
                            selected = sleepTimerRemainingMs == null,
                            onClick = onCancelSleepTimer,
                        )
                        SoniqSleepTimerManager.PRESET_MINUTES.forEach { minutes ->
                            SoniqGlassPillChip(
                                label = stringResource(R.string.sleep_timer_minutes, minutes),
                                selected = false,
                                onClick = { onStartSleepTimer(minutes) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerPresetRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    val glass = SoniqTheme.glass
    val shape = RoundedCornerShape(14.dp)
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            listOf(
                glass.activePillStart.copy(alpha = 0.28f),
                glass.activePillEnd.copy(alpha = 0.20f),
            ),
        )
    } else {
        Brush.verticalGradient(listOf(glass.surfaceTop, glass.surfaceBottom))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (selected) glass.borderHighlight else glass.borderSubtle,
                shape = shape,
            )
            .semantics { this.selected = selected }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm + 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.cd_selected),
                tint = glass.scrubberEnd,
                modifier = Modifier.size(SoniqTheme.tokens.sizes.iconSmall),
            )
        }
    }
}

@Composable
private fun UnsupportedEffectNotice(reason: String) {
    val spacing = SoniqTheme.tokens.spacing
    SoniqGlassCard(
        elevated = false,
        contentPadding = PaddingValues(spacing.md),
    ) {
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
