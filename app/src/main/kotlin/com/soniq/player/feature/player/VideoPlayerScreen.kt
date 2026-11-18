package com.soniq.player.feature.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import com.soniq.player.R
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.designsystem.token.SoniqNeutrals
import com.soniq.player.core.playback.PlaybackState

private const val VIDEO_SEEK_STEP_MS = 10_000L

/**
 * Full-screen local Video playback experience.
 *
 * Binds a Media3 Compose [PlayerSurface] to the service-owned [player] without taking ownership
 * of the playback engine lifecycle. Exits pause active video playback automatically while keeping
 * background audio behavior separate.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    player: Player?,
    playbackState: PlaybackState,
    onNavigateBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onLeaveScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    val alpha = SoniqTheme.tokens.alpha
    var controlsVisible by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { onLeaveScreen() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoniqNeutrals.AmoledBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                controlsVisible = !controlsVisible
            },
    ) {
        if (player != null) {
            PlayerSurface(
                player = player,
                surfaceType = SURFACE_TYPE_SURFACE_VIEW,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (controlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoniqNeutrals.AmoledBackground.copy(alpha = alpha.videoControlsScrim))
                    .systemBarsPadding()
                    .padding(spacing.lg),
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.player_action_close),
                            tint = SoniqNeutrals.DarkOnSurface,
                        )
                    }
                    Text(
                        text = playbackState.title ?: stringResource(R.string.destination_video),
                        style = MaterialTheme.typography.titleMedium,
                        color = SoniqNeutrals.DarkOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(onClick = onCycleSpeed) {
                        Text(
                            text = stringResource(
                                R.string.player_speed_label,
                                PlaybackViewModel.formatSpeed(playbackState.playbackSpeed),
                            ),
                        )
                    }
                }

                // Center transport controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xl),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onSeekBy(-VIDEO_SEEK_STEP_MS) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_replay_10),
                            contentDescription = stringResource(R.string.player_action_rewind),
                            tint = SoniqNeutrals.DarkOnSurface,
                            modifier = Modifier.size(sizes.iconMedium),
                        )
                    }
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        modifier = Modifier.size(sizes.primaryTransportButton),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (playbackState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                            ),
                            contentDescription = stringResource(
                                if (playbackState.isPlaying) R.string.player_action_pause else R.string.player_action_play,
                            ),
                            modifier = Modifier.size(sizes.iconMedium),
                        )
                    }
                    IconButton(onClick = { onSeekBy(VIDEO_SEEK_STEP_MS) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_forward_10),
                            contentDescription = stringResource(R.string.player_action_forward),
                            tint = SoniqNeutrals.DarkOnSurface,
                            modifier = Modifier.size(sizes.iconMedium),
                        )
                    }
                }

                // Bottom seek scrubber
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    PlaybackScrubber(
                        positionMs = playbackState.positionMs,
                        durationMs = playbackState.durationMs,
                        onSeekTo = onSeekTo,
                    )
                }
            }
        }
    }
}
