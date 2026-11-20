package com.soniq.player.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.soniq.player.R
import com.soniq.player.core.designsystem.component.SoniqGlassCard
import com.soniq.player.core.designsystem.component.SoniqGlassIconButton
import com.soniq.player.core.designsystem.component.SoniqGlassLinearProgress
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.playback.PlaybackState

/**
 * Floating frosted-glass mini-player docked above SONIQ's Liquid Glass navigation bar.
 */
@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    onOpenPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    val sizes = SoniqTheme.tokens.sizes
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val progress = if (playbackState.durationMs > 0L) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val thumbShape = RoundedCornerShape(metrics.thumbnailRadius)

    SoniqGlassCard(
        modifier = modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
        cornerRadius = metrics.cardRadius,
        elevated = true,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlayer)
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(sizes.trackArtwork)
                        .clip(thumbShape)
                        .background(Brush.linearGradient(listOf(glass.surfaceElevated, glass.surfaceBottom)))
                        .border(metrics.borderWidth, glass.borderSubtle, thumbShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(sizes.icon),
                    )
                    val art = playbackState.artworkUri
                    if (!art.isNullOrBlank()) {
                        AsyncImage(
                            model = art.toUri(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playbackState.title ?: stringResource(R.string.player_now_playing),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!playbackState.subtitle.isNullOrBlank()) {
                        Text(
                            text = playbackState.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
                    size = 42.dp,
                    iconSize = 22.dp,
                )

                if (playbackState.hasNext) {
                    IconButton(onClick = onSkipNext) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_next),
                            contentDescription = stringResource(R.string.player_action_next),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            SoniqGlassLinearProgress(
                progress = progress,
                height = 3.dp,
            )
        }
    }
}
