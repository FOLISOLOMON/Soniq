package com.soniq.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.soniq.player.R
import com.soniq.player.core.common.MediaFormatters
import com.soniq.player.core.designsystem.component.SoniqEmptyState
import com.soniq.player.core.designsystem.component.SoniqScaffold
import com.soniq.player.core.designsystem.component.SoniqTopAppBar
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.playback.PlaybackRepeatMode
import com.soniq.player.core.playback.PlaybackState
import com.soniq.player.core.playback.QueueItem

/**
 * Full-screen Audio Now Playing experience.
 * Adapts dynamically to the user's persisted [PlayerLayout] preference (`CLASSIC`, `ARTWORK_FOCUS`,
 * `COMPACT`) while keeping all styling driven by [SoniqTheme.tokens].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    onNavigateBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onCycleSpeed: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onSelectQueueItem: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onRemoveQueueItem: (Int) -> Unit = {},
    playerLayout: PlayerLayout = SoniqTheme.appearance.playerLayout,
) {
    var showQueueSheet by remember { mutableStateOf(false) }

    SoniqScaffold(
        modifier = modifier,
        topBar = {
            SoniqTopAppBar(
                title = stringResource(R.string.player_now_playing),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.player_action_close),
                        )
                    }
                },
                actions = {
                    if (playbackState.hasActiveItem) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                painter = painterResource(
                                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                                ),
                                contentDescription = stringResource(
                                    if (isFavorite) R.string.track_action_unfavorite else R.string.track_action_favorite,
                                ),
                                tint = if (isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onBackground
                                },
                            )
                        }
                        if (playbackState.queue.isNotEmpty()) {
                            IconButton(onClick = { showQueueSheet = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_queue_music),
                                    contentDescription = stringResource(R.string.player_action_queue),
                                )
                            }
                        }
                        FilledTonalButton(onClick = onCycleSpeed) {
                            Text(
                                text = stringResource(
                                    R.string.player_speed_label,
                                    PlaybackViewModel.formatSpeed(playbackState.playbackSpeed),
                                ),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (!playbackState.hasActiveItem) {
            SoniqEmptyState(
                icon = painterResource(R.drawable.ic_music_note),
                title = stringResource(R.string.player_nothing_playing),
                message = stringResource(R.string.player_select_media_prompt),
                modifier = Modifier.padding(innerPadding),
            )
            return@SoniqScaffold
        }

        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = SoniqTheme.tokens.spacing.xl)

        when (playerLayout) {
            PlayerLayout.CLASSIC -> ClassicAudioPlayerBody(
                playbackState = playbackState,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeekTo = onSeekTo,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode,
                modifier = contentModifier,
            )

            PlayerLayout.ARTWORK_FOCUS -> ArtworkFocusAudioPlayerBody(
                playbackState = playbackState,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeekTo = onSeekTo,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode,
                modifier = contentModifier,
            )

            PlayerLayout.COMPACT -> CompactAudioPlayerBody(
                playbackState = playbackState,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeekTo = onSeekTo,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode,
                modifier = contentModifier,
            )
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            QueueSheetContent(
                queue = playbackState.queue,
                onSelect = { index ->
                    showQueueSheet = false
                    onSelectQueueItem(index)
                },
                onMove = onMoveQueueItem,
                onRemove = onRemoveQueueItem,
            )
        }
    }
}

@Composable
private fun QueueSheetContent(
    queue: List<QueueItem>,
    onSelect: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = spacing.xl),
    ) {
        Text(
            text = stringResource(R.string.player_queue_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.md),
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            items(items = queue, key = { "${it.index}_${it.mediaId}" }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (item.isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                        )
                        .clickable { onSelect(item.index) }
                        .padding(horizontal = spacing.md, vertical = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (item.isCurrent) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!item.subtitle.isNullOrBlank()) {
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.isCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (item.index > 0) {
                        IconButton(onClick = { onMove(item.index, item.index - 1) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_upward),
                                contentDescription = stringResource(R.string.player_queue_move_up),
                            )
                        }
                    }
                    if (item.index < queue.lastIndex) {
                        IconButton(onClick = { onMove(item.index, item.index + 1) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_downward),
                                contentDescription = stringResource(R.string.player_queue_move_down),
                            )
                        }
                    }
                    IconButton(onClick = { onRemove(item.index) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.player_queue_remove),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicAudioPlayerBody(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        PlayerArtworkCard(
            artworkUri = playbackState.artworkUri,
            modifier = Modifier
                .widthIn(max = sizes.classicPlayerArtworkMax)
                .fillMaxWidth()
                .aspectRatio(1f),
        )
        TrackTitleBlock(playbackState = playbackState, centered = true)
        PlaybackScrubber(
            positionMs = playbackState.positionMs,
            durationMs = playbackState.durationMs,
            onSeekTo = onSeekTo,
        )
        TransportControlsRow(
            playbackState = playbackState,
            onTogglePlayPause = onTogglePlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeatMode = onCycleRepeatMode,
        )
        Spacer(modifier = Modifier.height(spacing.md))
    }
}

@Composable
private fun ArtworkFocusAudioPlayerBody(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    Column(
        modifier = modifier.padding(bottom = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayerArtworkCard(
            artworkUri = playbackState.artworkUri,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        TrackTitleBlock(playbackState = playbackState, centered = false)
        PlaybackScrubber(
            positionMs = playbackState.positionMs,
            durationMs = playbackState.durationMs,
            onSeekTo = onSeekTo,
        )
        TransportControlsRow(
            playbackState = playbackState,
            onTogglePlayPause = onTogglePlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeatMode = onCycleRepeatMode,
        )
    }
}

@Composable
private fun CompactAudioPlayerBody(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerArtworkCard(
                artworkUri = playbackState.artworkUri,
                modifier = Modifier.size(sizes.compactPlayerArtwork),
            )
            TrackTitleBlock(
                playbackState = playbackState,
                centered = false,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(spacing.xl))
        PlaybackScrubber(
            positionMs = playbackState.positionMs,
            durationMs = playbackState.durationMs,
            onSeekTo = onSeekTo,
        )
        Spacer(modifier = Modifier.height(spacing.lg))
        TransportControlsRow(
            playbackState = playbackState,
            onTogglePlayPause = onTogglePlayPause,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeatMode = onCycleRepeatMode,
        )
    }
}

@Composable
private fun PlayerArtworkCard(
    artworkUri: String?,
    modifier: Modifier = Modifier,
) {
    val sizes = SoniqTheme.tokens.sizes
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(sizes.emptyStateIcon),
        )
        if (!artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = artworkUri.toUri(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TrackTitleBlock(
    playbackState: PlaybackState,
    centered: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    val align = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = align,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = playbackState.title ?: stringResource(R.string.player_now_playing),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
        )
        if (!playbackState.subtitle.isNullOrBlank()) {
            Text(
                text = playbackState.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
            )
        }
        if (playbackState.queueCount > 1) {
            Text(
                text = stringResource(
                    R.string.player_queue_position,
                    playbackState.currentQueueIndex + 1,
                    playbackState.queueCount,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun PlaybackScrubber(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val sliderValue = if (isDragging) {
        dragValue
    } else {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }
    val displayedPositionMs = if (isDragging) {
        (dragValue * safeDuration).toLong()
    } else {
        positionMs
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = { fraction ->
                isDragging = true
                dragValue = fraction
            },
            onValueChangeFinished = {
                isDragging = false
                onSeekTo((dragValue * safeDuration).toLong())
            },
            enabled = durationMs > 0L,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = MediaFormatters.formatDuration(displayedPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = MediaFormatters.formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransportControlsRow(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
) {
    val sizes = SoniqTheme.tokens.sizes
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle),
                contentDescription = stringResource(R.string.player_action_shuffle),
                tint = if (playbackState.shuffleEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = onSkipPrevious) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_previous),
                contentDescription = stringResource(R.string.player_action_previous),
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
        IconButton(onClick = onSkipNext, enabled = playbackState.hasNext) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.player_action_next),
                modifier = Modifier.size(sizes.iconMedium),
            )
        }
        IconButton(onClick = onCycleRepeatMode) {
            val repeatIcon = if (playbackState.repeatMode == PlaybackRepeatMode.ONE) {
                R.drawable.ic_repeat_one
            } else {
                R.drawable.ic_repeat
            }
            Icon(
                painter = painterResource(repeatIcon),
                contentDescription = stringResource(R.string.player_action_repeat),
                tint = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
