package com.soniq.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.soniq.player.R
import com.soniq.player.core.common.MediaFormatters
import com.soniq.player.core.designsystem.component.SoniqEmptyState
import com.soniq.player.core.designsystem.component.SoniqGlassCard
import com.soniq.player.core.designsystem.component.SoniqGlassIconButton
import com.soniq.player.core.designsystem.component.SoniqGlassModalSheet
import com.soniq.player.core.designsystem.component.SoniqGlassPillChip
import com.soniq.player.core.designsystem.component.SoniqScaffold
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.appearance.PlayerLayout
import com.soniq.player.core.playback.AudioEffectsState
import com.soniq.player.core.playback.PlaybackRepeatMode
import com.soniq.player.core.playback.PlaybackState
import com.soniq.player.core.playback.QueueItem

/**
 * Full-screen Liquid Glass Audio Now Playing experience.
 * Adapts to the user's persisted [PlayerLayout] preference (`CLASSIC`, `ARTWORK_FOCUS`, `COMPACT`)
 * while surfacing Favorite, Scrubber, Transport, Queue, Audio Effects, Sleep Timer, and Speed.
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
    audioEffectsState: AudioEffectsState = AudioEffectsState(),
    onSetEqualizerEnabled: (Boolean) -> Unit = {},
    onSelectEqualizerPreset: (Int) -> Unit = {},
    onSetEqualizerBandLevel: (Int, Int) -> Unit = { _, _ -> },
    onSetBassBoostEnabled: (Boolean) -> Unit = {},
    onSetBassBoostStrength: (Int) -> Unit = {},
    onSetGaplessEnabled: (Boolean) -> Unit = {},
    onSetCrossfadeDurationSec: (Int) -> Unit = {},
    sleepTimerRemainingMs: Long? = null,
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    playerLayout: PlayerLayout = SoniqTheme.appearance.playerLayout,
) {
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEffectsSheet by remember { mutableStateOf(false) }
    val spacing = SoniqTheme.tokens.spacing

    SoniqScaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SoniqGlassIconButton(
                    onClick = onNavigateBack,
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.player_action_close),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = spacing.sm),
                ) {
                    Text(
                        text = stringResource(R.string.player_now_playing),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
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
                if (playbackState.hasActiveItem) {
                    SoniqGlassPillChip(
                        label = stringResource(
                            R.string.player_speed_label,
                            PlaybackViewModel.formatSpeed(playbackState.playbackSpeed),
                        ),
                        selected = false,
                        onClick = onCycleSpeed,
                    )
                } else {
                    Spacer(modifier = Modifier.size(SoniqTheme.tokens.sizes.minTouchTarget))
                }
            }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = SoniqTheme.tokens.sizes.contentMaxWidth),
            ) {
                when (playerLayout) {
                    PlayerLayout.CLASSIC -> ClassicAudioPlayerBody(
                        playbackState = playbackState,
                        isFavorite = isFavorite,
                        onToggleFavorite = onToggleFavorite,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onSeekTo = onSeekTo,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeatMode = onCycleRepeatMode,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PlayerLayout.ARTWORK_FOCUS -> ArtworkFocusAudioPlayerBody(
                        playbackState = playbackState,
                        isFavorite = isFavorite,
                        onToggleFavorite = onToggleFavorite,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onSeekTo = onSeekTo,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeatMode = onCycleRepeatMode,
                        modifier = Modifier.fillMaxSize(),
                    )

                    PlayerLayout.COMPACT -> CompactAudioPlayerBody(
                        playbackState = playbackState,
                        isFavorite = isFavorite,
                        onToggleFavorite = onToggleFavorite,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onSeekTo = onSeekTo,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeatMode = onCycleRepeatMode,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Bottom Frosted-Glass Utility Dock (Queue, Audio Effects, Sleep Timer)
            Spacer(modifier = Modifier.height(spacing.sm))
            SoniqGlassCard(
                modifier = Modifier.widthIn(max = SoniqTheme.tokens.sizes.contentMaxWidth),
                cornerRadius = SoniqTheme.glassMetrics.cardRadius,
                elevated = true,
                contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { showQueueSheet = true },
                        enabled = playbackState.queue.isNotEmpty(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_queue_music),
                            contentDescription = stringResource(R.string.player_action_queue),
                        )
                    }
                    IconButton(onClick = { showEffectsSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_equalizer),
                            contentDescription = stringResource(R.string.audio_effects_title),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showEffectsSheet = true }
                            .padding(horizontal = spacing.sm, vertical = spacing.xs),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_timer),
                            contentDescription = stringResource(R.string.sleep_timer_title),
                            tint = if (sleepTimerRemainingMs != null) {
                                SoniqTheme.glass.scrubberStart
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        if (sleepTimerRemainingMs != null) {
                            Text(
                                text = MediaFormatters.formatDuration(sleepTimerRemainingMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = SoniqTheme.glass.scrubberStart,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQueueSheet) {
        SoniqGlassModalSheet(onDismissRequest = { showQueueSheet = false }) {
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

    if (showEffectsSheet) {
        AudioEffectsSheet(
            effects = audioEffectsState,
            onDismiss = { showEffectsSheet = false },
            onSetEqualizerEnabled = onSetEqualizerEnabled,
            onSelectPreset = onSelectEqualizerPreset,
            onSetBandLevel = onSetEqualizerBandLevel,
            onSetBassBoostEnabled = onSetBassBoostEnabled,
            onSetBassBoostStrength = onSetBassBoostStrength,
            onSetGaplessEnabled = onSetGaplessEnabled,
            onSetCrossfadeDurationSec = onSetCrossfadeDurationSec,
            sleepTimerRemainingMs = sleepTimerRemainingMs,
            onStartSleepTimer = onStartSleepTimer,
            onCancelSleepTimer = onCancelSleepTimer,
        )
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
            modifier = Modifier
                .semantics { heading() }
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
        if (queue.isEmpty()) {
            Text(
                text = stringResource(R.string.player_queue_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(
                    items = queue,
                    key = { item -> "${item.index}_${item.mediaId}" },
                ) { item ->
                    SoniqGlassCard(
                        cornerRadius = SoniqTheme.glassMetrics.thumbnailRadius,
                        elevated = item.isCurrent,
                        onClick = { onSelect(item.index) },
                        contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (item.isCurrent) {
                                        MaterialTheme.colorScheme.primary
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    }
}

@Composable
private fun ClassicAudioPlayerBody(
    playbackState: PlaybackState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
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
        GlowingPlayerArtwork(
            artworkUri = playbackState.artworkUri,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = sizes.classicPlayerArtworkMax)
                .aspectRatio(1f, matchHeightConstraintsFirst = true),
        )
        PlayerTitleAndFavoriteRow(
            title = playbackState.title,
            subtitle = playbackState.subtitle,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
        )
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
private fun ArtworkFocusAudioPlayerBody(
    playbackState: PlaybackState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        GlowingPlayerArtwork(
            artworkUri = playbackState.artworkUri,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f, matchHeightConstraintsFirst = true),
        )
        PlayerTitleAndFavoriteRow(
            title = playbackState.title,
            subtitle = playbackState.subtitle,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
        )
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
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlowingPlayerArtwork(
                artworkUri = playbackState.artworkUri,
                modifier = Modifier.size(sizes.compactPlayerArtwork),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = playbackState.title ?: stringResource(R.string.player_now_playing),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!playbackState.subtitle.isNullOrBlank()) {
                    Text(
                        text = playbackState.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SoniqGlassIconButton(
                onClick = onToggleFavorite,
                painter = painterResource(
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                ),
                contentDescription = stringResource(
                    if (isFavorite) R.string.track_action_unfavorite else R.string.track_action_favorite,
                ),
                highlighted = isFavorite,
            )
        }

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
private fun PlayerTitleAndFavoriteRow(
    title: String?,
    subtitle: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title ?: stringResource(R.string.player_now_playing),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SoniqGlassIconButton(
            onClick = onToggleFavorite,
            painter = painterResource(
                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
            ),
            contentDescription = stringResource(
                if (isFavorite) R.string.track_action_unfavorite else R.string.track_action_favorite,
            ),
            highlighted = isFavorite,
        )
    }
}

@Composable
private fun GlowingPlayerArtwork(
    artworkUri: String?,
    modifier: Modifier = Modifier,
) {
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val shape = RoundedCornerShape(metrics.artworkRadius)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(glass.glowPrimary, glass.glowSecondary.copy(alpha = 0.14f), Color.Transparent),
                ),
            )
            .padding(6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.linearGradient(listOf(glass.surfaceElevated, glass.surfaceBottom)))
                .border(metrics.borderWidth, glass.borderHighlight, shape),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SoniqTheme.tokens.sizes.emptyStateIcon),
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
}

@Composable
internal fun PlaybackScrubber(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeDuration = durationMs.coerceAtLeast(0L)
    val clampedPosition = positionMs.coerceIn(0L, safeDuration.coerceAtLeast(1L))
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (isDragging) {
        dragFraction
    } else if (safeDuration > 0L) {
        clampedPosition.toFloat() / safeDuration.toFloat()
    } else {
        0f
    }
    val displayedPositionMs = if (isDragging) {
        (dragFraction * safeDuration).toLong()
    } else {
        clampedPosition
    }
    val glass = SoniqTheme.glass

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = currentFraction.coerceIn(0f, 1f),
            onValueChange = { value ->
                isDragging = true
                dragFraction = value
            },
            onValueChangeFinished = {
                val target = (dragFraction * safeDuration).toLong()
                isDragging = false
                onSeekTo(target)
            },
            enabled = safeDuration > 0L,
            colors = SliderDefaults.colors(
                thumbColor = glass.scrubberStart,
                activeTrackColor = glass.scrubberStart,
                inactiveTrackColor = glass.borderSubtle,
            ),
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
                text = MediaFormatters.formatDuration(safeDuration),
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
    val glass = SoniqTheme.glass
    val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle),
                contentDescription = stringResource(R.string.player_action_shuffle),
                tint = if (playbackState.shuffleEnabled) glass.scrubberStart else inactiveTint,
            )
        }

        IconButton(onClick = onSkipPrevious) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_previous),
                contentDescription = stringResource(R.string.player_action_previous),
                modifier = Modifier.size(sizes.iconMedium),
            )
        }

        SoniqGlassIconButton(
            onClick = onTogglePlayPause,
            painter = painterResource(
                if (playbackState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            ),
            contentDescription = stringResource(
                if (playbackState.isPlaying) R.string.player_action_pause else R.string.player_action_play,
            ),
            highlighted = true,
            size = sizes.primaryTransportButton + 4.dp,
            iconSize = sizes.iconMedium,
        )

        IconButton(
            onClick = onSkipNext,
            enabled = playbackState.hasNext,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_next),
                contentDescription = stringResource(R.string.player_action_next),
                modifier = Modifier.size(sizes.iconMedium),
            )
        }

        IconButton(onClick = onCycleRepeatMode) {
            val (iconRes, tint) = when (playbackState.repeatMode) {
                PlaybackRepeatMode.OFF -> R.drawable.ic_repeat to inactiveTint
                PlaybackRepeatMode.ALL -> R.drawable.ic_repeat to glass.scrubberEnd
                PlaybackRepeatMode.ONE -> R.drawable.ic_repeat_one to glass.scrubberEnd
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(R.string.player_action_repeat),
                tint = tint,
            )
        }
    }
}
