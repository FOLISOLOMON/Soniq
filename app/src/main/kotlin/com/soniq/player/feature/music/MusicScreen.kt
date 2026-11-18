package com.soniq.player.feature.music

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.soniq.player.R
import com.soniq.player.core.common.MediaFormatters
import com.soniq.player.core.designsystem.component.SoniqEmptyState
import com.soniq.player.core.designsystem.component.SoniqScaffold
import com.soniq.player.core.designsystem.component.SoniqSurfaceCard
import com.soniq.player.core.designsystem.component.SoniqTopAppBar
import com.soniq.player.core.designsystem.theme.SoniqTheme
import com.soniq.player.core.model.media.AlbumSummary
import com.soniq.player.core.model.media.ArtistSummary
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaFolderSummary
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.MusicSortOrder
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.rememberMediaPermissionController

@Composable
fun MusicRoute(
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionController = rememberMediaPermissionController(
        kind = MediaKind.AUDIO,
        onResult = viewModel::onPermissionResult,
    )

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose {}
    }

    MusicScreen(
        uiState = uiState,
        onRequestPermission = permissionController.requestPermission,
        onOpenAppSettings = permissionController.openAppSettings,
        onSelectTab = viewModel::selectTab,
        onSelectSortOrder = viewModel::selectSortOrder,
        onToggleSearch = viewModel::setSearchActive,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onAlbumClick = viewModel::selectAlbum,
        onArtistClick = viewModel::selectArtist,
        onFolderClick = viewModel::selectFolder,
        onClearFilter = viewModel::clearFilter,
        onRefresh = viewModel::refreshLibrary,
        modifier = modifier,
    )
}

@Composable
fun MusicScreen(
    uiState: MusicUiState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectTab: (MusicLibraryTab) -> Unit = {},
    onSelectSortOrder: (MusicSortOrder) -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onAlbumClick: (AlbumSummary) -> Unit = {},
    onArtistClick: (ArtistSummary) -> Unit = {},
    onFolderClick: (MediaFolderSummary) -> Unit = {},
    onClearFilter: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onTrackClick: (AudioTrack) -> Unit = {},
) {
    val hasAccess = uiState.accessState == MediaAccessState.GRANTED ||
        uiState.accessState == MediaAccessState.PARTIAL
    var sortMenuExpanded by remember { mutableStateOf(false) }

    SoniqScaffold(
        modifier = modifier,
        topBar = {
            SoniqTopAppBar(
                title = uiState.activeFilter?.label ?: stringResource(R.string.destination_music),
                navigationIcon = {
                    if (uiState.activeFilter != null) {
                        IconButton(onClick = onClearFilter) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.library_action_clear_filter),
                            )
                        }
                    }
                },
                actions = {
                    if (hasAccess) {
                        IconButton(onClick = { onToggleSearch(!uiState.isSearchActive) }) {
                            Icon(
                                painter = painterResource(
                                    if (uiState.isSearchActive) R.drawable.ic_close else R.drawable.ic_search,
                                ),
                                contentDescription = stringResource(
                                    if (uiState.isSearchActive) {
                                        R.string.library_action_close_search
                                    } else {
                                        R.string.library_action_search
                                    },
                                ),
                            )
                        }
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sort),
                                    contentDescription = stringResource(R.string.library_action_sort),
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                MusicSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(order.labelRes())) },
                                        onClick = {
                                            sortMenuExpanded = false
                                            onSelectSortOrder(order)
                                        },
                                        trailingIcon = {
                                            if (order == uiState.sortOrder) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_check),
                                                    contentDescription = stringResource(R.string.cd_selected),
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = stringResource(R.string.library_action_refresh),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState.accessState) {
            MediaAccessState.NOT_GRANTED -> {
                SoniqEmptyState(
                    icon = painterResource(R.drawable.ic_lock),
                    title = stringResource(R.string.permission_music_title),
                    message = stringResource(R.string.permission_music_message),
                    actionLabel = stringResource(R.string.permission_grant),
                    onActionClick = onRequestPermission,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            MediaAccessState.DENIED_PERMANENTLY -> {
                SoniqEmptyState(
                    icon = painterResource(R.drawable.ic_lock),
                    title = stringResource(R.string.permission_music_title),
                    message = stringResource(R.string.permission_denied_permanently),
                    actionLabel = stringResource(R.string.permission_open_settings),
                    onActionClick = onOpenAppSettings,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            MediaAccessState.GRANTED,
            MediaAccessState.PARTIAL,
            -> {
                MusicLibraryContent(
                    uiState = uiState,
                    onSelectTab = onSelectTab,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onFolderClick = onFolderClick,
                    onRefresh = onRefresh,
                    onTrackClick = onTrackClick,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun MusicLibraryContent(
    uiState: MusicUiState,
    onSelectTab: (MusicLibraryTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
    onArtistClick: (ArtistSummary) -> Unit,
    onFolderClick: (MediaFolderSummary) -> Unit,
    onRefresh: () -> Unit,
    onTrackClick: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (uiState.isSearchActive) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text(text = stringResource(R.string.music_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
            )
        }

        if (uiState.activeFilter == null) {
            PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                MusicLibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == uiState.selectedTab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            Text(
                                text = stringResource(tab.labelRes()),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        if (uiState.hasSyncError) {
            SyncErrorBanner(onRetry = onRefresh)
        }

        val showTracks = uiState.activeFilter != null || uiState.selectedTab == MusicLibraryTab.SONGS
        when {
            showTracks -> TrackListSection(
                tracks = uiState.tracks,
                hasSearchQuery = uiState.searchQuery.isNotBlank(),
                onRefresh = onRefresh,
                onTrackClick = onTrackClick,
            )

            uiState.selectedTab == MusicLibraryTab.ALBUMS -> AlbumListSection(
                albums = uiState.albums,
                hasSearchQuery = uiState.searchQuery.isNotBlank(),
                onRefresh = onRefresh,
                onAlbumClick = onAlbumClick,
            )

            uiState.selectedTab == MusicLibraryTab.ARTISTS -> ArtistListSection(
                artists = uiState.artists,
                hasSearchQuery = uiState.searchQuery.isNotBlank(),
                onRefresh = onRefresh,
                onArtistClick = onArtistClick,
            )

            else -> MusicFolderListSection(
                folders = uiState.folders,
                hasSearchQuery = uiState.searchQuery.isNotBlank(),
                onRefresh = onRefresh,
                onFolderClick = onFolderClick,
            )
        }
    }
}

@Composable
private fun TrackListSection(
    tracks: List<AudioTrack>,
    hasSearchQuery: Boolean,
    onRefresh: () -> Unit,
    onTrackClick: (AudioTrack) -> Unit,
) {
    if (tracks.isEmpty()) {
        MusicEmptyView(hasSearchQuery = hasSearchQuery, onRefresh = onRefresh)
        return
    }
    val spacing = SoniqTheme.tokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenHorizontal,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(items = tracks, key = { it.id }) { track ->
            AudioTrackRow(track = track, onClick = { onTrackClick(track) })
        }
    }
}

@Composable
private fun AlbumListSection(
    albums: List<AlbumSummary>,
    hasSearchQuery: Boolean,
    onRefresh: () -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
) {
    if (albums.isEmpty()) {
        MusicEmptyView(hasSearchQuery = hasSearchQuery, onRefresh = onRefresh)
        return
    }
    val spacing = SoniqTheme.tokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenHorizontal,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(items = albums, key = { "${it.albumId}_${it.title}" }) { album ->
            MediaCollectionRow(
                title = album.title,
                subtitle = "${album.artist} • ${trackCountLabel(album.trackCount)}",
                trailing = MediaFormatters.formatDuration(album.totalDurationMs),
                artworkUri = album.albumArtUri,
                placeholderIconRes = R.drawable.ic_album,
                onClick = { onAlbumClick(album) },
            )
        }
    }
}

@Composable
private fun ArtistListSection(
    artists: List<ArtistSummary>,
    hasSearchQuery: Boolean,
    onRefresh: () -> Unit,
    onArtistClick: (ArtistSummary) -> Unit,
) {
    if (artists.isEmpty()) {
        MusicEmptyView(hasSearchQuery = hasSearchQuery, onRefresh = onRefresh)
        return
    }
    val spacing = SoniqTheme.tokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenHorizontal,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(items = artists, key = { it.name }) { artist ->
            MediaCollectionRow(
                title = artist.name,
                subtitle = "${albumCountLabel(artist.albumCount)} • ${trackCountLabel(artist.trackCount)}",
                trailing = null,
                artworkUri = null,
                placeholderIconRes = R.drawable.ic_person,
                onClick = { onArtistClick(artist) },
            )
        }
    }
}

@Composable
private fun MusicFolderListSection(
    folders: List<MediaFolderSummary>,
    hasSearchQuery: Boolean,
    onRefresh: () -> Unit,
    onFolderClick: (MediaFolderSummary) -> Unit,
) {
    if (folders.isEmpty()) {
        MusicEmptyView(hasSearchQuery = hasSearchQuery, onRefresh = onRefresh)
        return
    }
    val spacing = SoniqTheme.tokens.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = spacing.screenHorizontal,
            vertical = spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(items = folders, key = { it.folderPath }) { folder ->
            MediaCollectionRow(
                title = folder.folderName,
                subtitle = folder.folderPath.ifEmpty { folder.folderName },
                trailing = trackCountLabel(folder.itemCount),
                artworkUri = null,
                placeholderIconRes = R.drawable.ic_folder,
                onClick = { onFolderClick(folder) },
            )
        }
    }
}

@Composable
private fun AudioTrackRow(
    track: AudioTrack,
    onClick: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkThumbnail(
            artworkUri = track.albumArtUri,
            placeholderIconRes = R.drawable.ic_music_note,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artist} • ${track.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = MediaFormatters.formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MediaCollectionRow(
    title: String,
    subtitle: String,
    trailing: String?,
    artworkUri: String?,
    placeholderIconRes: Int,
    onClick: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkThumbnail(artworkUri = artworkUri, placeholderIconRes = placeholderIconRes)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ArtworkThumbnail(
    artworkUri: String?,
    placeholderIconRes: Int,
) {
    val sizes = SoniqTheme.tokens.sizes
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizes.trackArtwork)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Icon(
            painter = painterResource(placeholderIconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(sizes.icon),
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
private fun SyncErrorBanner(onRetry: () -> Unit) {
    val spacing = SoniqTheme.tokens.spacing
    SoniqSurfaceCard(
        modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.library_sync_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.library_action_retry))
            }
        }
    }
}

@Composable
private fun MusicEmptyView(
    hasSearchQuery: Boolean,
    onRefresh: () -> Unit,
) {
    if (hasSearchQuery) {
        SoniqEmptyState(
            icon = painterResource(R.drawable.ic_search),
            title = stringResource(R.string.library_no_search_matches_title),
            message = stringResource(R.string.library_no_search_matches_message),
        )
    } else {
        SoniqEmptyState(
            icon = painterResource(R.drawable.ic_music_note),
            title = stringResource(R.string.music_empty_title),
            message = stringResource(R.string.music_empty_message),
            actionLabel = stringResource(R.string.library_action_refresh),
            onActionClick = onRefresh,
        )
    }
}

@Composable
private fun trackCountLabel(count: Int): String =
    pluralStringResource(R.plurals.library_track_count, count, count)

@Composable
private fun albumCountLabel(count: Int): String =
    pluralStringResource(R.plurals.library_album_count, count, count)

private fun MusicLibraryTab.labelRes(): Int = when (this) {
    MusicLibraryTab.SONGS -> R.string.music_tab_songs
    MusicLibraryTab.ALBUMS -> R.string.music_tab_albums
    MusicLibraryTab.ARTISTS -> R.string.music_tab_artists
    MusicLibraryTab.FOLDERS -> R.string.music_tab_folders
}

private fun MusicSortOrder.labelRes(): Int = when (this) {
    MusicSortOrder.TITLE_ASC -> R.string.music_sort_title
    MusicSortOrder.ARTIST_ASC -> R.string.music_sort_artist
    MusicSortOrder.DATE_ADDED_DESC -> R.string.music_sort_date_added
    MusicSortOrder.DURATION_DESC -> R.string.music_sort_duration
}
