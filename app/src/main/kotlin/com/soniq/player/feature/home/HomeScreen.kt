package com.soniq.player.feature.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.soniq.player.R
import com.soniq.player.core.common.MediaFormatters
import com.soniq.player.core.designsystem.component.SoniqGlassCard
import com.soniq.player.core.designsystem.component.SoniqGlassIconButton
import com.soniq.player.core.designsystem.component.SoniqGlassMediaRow
import com.soniq.player.core.designsystem.component.SoniqGlassSearchBar
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.feature.music.MusicLibraryTab
import java.util.Calendar

@Composable
fun HomeRoute(
    onPlayTracks: (List<AudioTrack>, Int) -> Unit,
    onOpenMusicTab: (MusicLibraryTab) -> Unit,
    onOpenVideo: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onPlayTracks = onPlayTracks,
        onOpenMusicTab = onOpenMusicTab,
        onOpenVideo = onOpenVideo,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChanged: (String) -> Unit,
    onPlayTracks: (List<AudioTrack>, Int) -> Unit,
    onOpenMusicTab: (MusicLibraryTab) -> Unit,
    onOpenVideo: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val greetingRes = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..16 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = spacing.md,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        // Header greeting + Settings quick button
        item(key = "home_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(greetingRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                SoniqGlassIconButton(
                    onClick = onOpenSettings,
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.destination_settings),
                )
            }
        }

        // Glass Search Bar
        item(key = "home_search") {
            SoniqGlassSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                placeholder = stringResource(R.string.home_search_hint),
            )
        }

        // Live search results when typing, otherwise Hero + Categories + Recently Played
        if (uiState.searchQuery.isNotBlank()) {
            itemsIndexed(
                items = uiState.searchResults,
                key = { _, track -> "home_search_${track.id}" },
            ) { index, track ->
                SoniqGlassMediaRow(
                    title = track.title,
                    subtitle = "${track.artist} • ${MediaFormatters.formatDuration(track.durationMs)}",
                    artworkUri = track.albumArtUri,
                    fallbackIcon = painterResource(R.drawable.ic_music_note),
                    onClick = { onPlayTracks(uiState.searchResults, index) },
                    trailing = {
                        SoniqGlassIconButton(
                            onClick = { onPlayTracks(uiState.searchResults, index) },
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = stringResource(R.string.player_action_play),
                            size = 40.dp,
                            iconSize = 20.dp,
                        )
                    },
                )
            }
        } else {
            // Featured / Continue Listening Hero Card
            item(key = "home_hero") {
                val featured = uiState.featuredTrack
                val heroShape = RoundedCornerShape(metrics.heroRadius)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp)
                        .clip(heroShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    glass.activePillStart.copy(alpha = 0.72f),
                                    glass.ambientMid.copy(alpha = 0.85f),
                                    glass.activePillEnd.copy(alpha = 0.55f),
                                ),
                            ),
                        )
                        .border(metrics.borderWidth, glass.borderHighlight, heroShape)
                        .clickable {
                            if (featured != null) {
                                onPlayTracks(listOf(featured), 0)
                            } else {
                                onOpenMusicTab(MusicLibraryTab.SONGS)
                            }
                        },
                ) {
                    if (featured?.albumArtUri != null) {
                        AsyncImage(
                            model = featured.albumArtUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alpha = 0.42f,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.lg),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.home_featured_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.86f),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = spacing.md),
                            ) {
                                Text(
                                    text = featured?.title ?: stringResource(R.string.home_featured_empty_title),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(spacing.xxs))
                                Text(
                                    text = if (featured != null) {
                                        "${featured.artist} • ${pluralStringResource(R.plurals.library_track_count, uiState.totalTracks, uiState.totalTracks)}"
                                    } else {
                                        stringResource(R.string.home_featured_empty_subtitle)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            SoniqGlassIconButton(
                                onClick = {
                                    if (featured != null) {
                                        onPlayTracks(listOf(featured), 0)
                                    } else {
                                        onOpenMusicTab(MusicLibraryTab.SONGS)
                                    }
                                },
                                painter = painterResource(R.drawable.ic_play),
                                contentDescription = stringResource(R.string.player_action_play),
                                highlighted = true,
                                size = 52.dp,
                            )
                        }
                    }
                }
            }

            // Quick Access Categories Row
            item(key = "home_categories") {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_categories),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() },
                        )
                        TextButton(onClick = { onOpenMusicTab(MusicLibraryTab.SONGS) }) {
                            Text(stringResource(R.string.home_see_all))
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                    ) {
                        HomeCategoryBubble(R.string.music_tab_songs, R.drawable.ic_music_note) {
                            onOpenMusicTab(MusicLibraryTab.SONGS)
                        }
                        HomeCategoryBubble(R.string.music_tab_playlists, R.drawable.ic_queue_music) {
                            onOpenMusicTab(MusicLibraryTab.PLAYLISTS)
                        }
                        HomeCategoryBubble(R.string.music_tab_favorites, R.drawable.ic_favorite) {
                            onOpenMusicTab(MusicLibraryTab.FAVORITES)
                        }
                        HomeCategoryBubble(R.string.music_tab_albums, R.drawable.ic_album) {
                            onOpenMusicTab(MusicLibraryTab.ALBUMS)
                        }
                        HomeCategoryBubble(R.string.music_tab_artists, R.drawable.ic_person) {
                            onOpenMusicTab(MusicLibraryTab.ARTISTS)
                        }
                        HomeCategoryBubble(R.string.destination_video, R.drawable.ic_movie, onOpenVideo)
                    }
                }
            }

            // Recently Played Section
            item(key = "home_recent_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_recently_played),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (uiState.recentlyPlayed.isNotEmpty()) {
                        TextButton(onClick = { onPlayTracks(uiState.recentlyPlayed, 0) }) {
                            Text(stringResource(R.string.home_see_all))
                        }
                    }
                }
            }

            if (uiState.recentlyPlayed.isEmpty()) {
                item(key = "home_recent_empty") {
                    SoniqGlassCard {
                        Text(
                            text = stringResource(R.string.home_recently_played_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.recentlyPlayed,
                    key = { _, track -> "home_recent_${track.id}" },
                ) { index, track ->
                    SoniqGlassMediaRow(
                        title = track.title,
                        subtitle = track.artist,
                        artworkUri = track.albumArtUri,
                        fallbackIcon = painterResource(R.drawable.ic_music_note),
                        onClick = { onPlayTracks(uiState.recentlyPlayed, index) },
                        trailing = {
                            SoniqGlassIconButton(
                                onClick = { onPlayTracks(uiState.recentlyPlayed, index) },
                                painter = painterResource(R.drawable.ic_play),
                                contentDescription = stringResource(R.string.player_action_play),
                                size = 40.dp,
                                iconSize = 20.dp,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCategoryBubble(
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    val glass = SoniqTheme.glass
    val label = stringResource(labelRes)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(glass.surfaceElevated, glass.surfaceBottom)))
                .border(1.dp, glass.borderHighlight, CircleShape),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
