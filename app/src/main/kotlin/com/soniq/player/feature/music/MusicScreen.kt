package com.soniq.player.feature.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.soniq.player.R
import com.soniq.player.core.common.MediaFormatters
import com.soniq.player.core.designsystem.component.SoniqEmptyState
import com.soniq.player.core.designsystem.component.SoniqGlassCard
import com.soniq.player.core.designsystem.component.SoniqGlassIconButton
import com.soniq.player.core.designsystem.component.SoniqGlassPillChip
import com.soniq.player.core.designsystem.component.SoniqGlassSearchBar
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
import com.soniq.player.core.model.media.PlaylistSummary
import com.soniq.player.core.model.media.mediaKey
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.rememberMediaPermissionController

@Composable
fun MusicRoute(
    modifier: Modifier = Modifier,
    onPlayTrack: (tracks: List<AudioTrack>, startIndex: Int) -> Unit = { _, _ -> },
    onPlayNext: (AudioTrack) -> Unit = {},
    onAddToQueue: (AudioTrack) -> Unit = {},
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
        onNewPlaylistNameChanged = viewModel::onNewPlaylistNameChanged,
        onCreatePlaylist = viewModel::createPlaylist,
        onRenamePlaylist = viewModel::renamePlaylist,
        onDeletePlaylist = viewModel::deletePlaylist,
        onPlaylistClick = viewModel::selectPlaylist,
        onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
        onRemoveTrackFromPlaylist = viewModel::removeTrackFromActivePlaylist,
        onToggleFavorite = viewModel::toggleTrackFavorite,
        onAlbumClick = viewModel::selectAlbum,
        onArtistClick = viewModel::selectArtist,
        onFolderClick = viewModel::selectFolder,
        onClearFilter = viewModel::clearFilter,
        onRefresh = viewModel::refreshLibrary,
        onTrackClick = { clicked, list ->
            val index = list.indexOfFirst { it.id == clicked.id }.coerceAtLeast(0)
            onPlayTrack(list, index)
        },
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
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
    onNewPlaylistNameChanged: (String) -> Unit = {},
    onCreatePlaylist: () -> Unit = {},
    onRenamePlaylist: (Long, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (Long) -> Unit = {},
    onPlaylistClick: (PlaylistSummary) -> Unit = {},
    onAddTrackToPlaylist: (playlistId: Long, trackId: Long) -> Unit = { _, _ -> },
    onRemoveTrackFromPlaylist: (trackId: Long) -> Unit = {},
    onToggleFavorite: (trackId: Long) -> Unit = {},
    onAlbumClick: (AlbumSummary) -> Unit = {},
    onArtistClick: (ArtistSummary) -> Unit = {},
    onFolderClick: (MediaFolderSummary) -> Unit = {},
    onClearFilter: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onTrackClick: (AudioTrack, List<AudioTrack>) -> Unit = { _, _ -> },
    onPlayNext: (AudioTrack) -> Unit = {},
    onAddToQueue: (AudioTrack) -> Unit = {},
) {
    val hasAccess = uiState.accessState == MediaAccessState.GRANTED ||
        uiState.accessState == MediaAccessState.PARTIAL
    var sortMenuExpanded by remember { mutableStateOf(false) }

    SoniqScaffold(
        modifier = modifier,
        topBar = {
            SoniqTopAppBar(
                title = uiState.activeFilter?.label ?: stringResource(R.string.library_title),
                navigationIcon = {
                    if (uiState.activeFilter != null) {
                        SoniqGlassIconButton(
                            onClick = onClearFilter,
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.library_action_clear_filter),
                            size = 40.dp,
                            iconSize = SoniqTheme.tokens.sizes.iconSmall,
                            modifier = Modifier.padding(start = SoniqTheme.tokens.spacing.sm),
                        )
                    }
                },
                actions = {
                    if (hasAccess) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(SoniqTheme.tokens.spacing.xs),
                            modifier = Modifier.padding(end = SoniqTheme.tokens.spacing.sm),
                        ) {
                            SoniqGlassIconButton(
                                onClick = { onToggleSearch(!uiState.isSearchActive) },
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
                                highlighted = uiState.isSearchActive,
                                size = 40.dp,
                                iconSize = SoniqTheme.tokens.sizes.iconSmall,
                            )
                            Box {
                                SoniqGlassIconButton(
                                    onClick = { sortMenuExpanded = true },
                                    painter = painterResource(R.drawable.ic_sort),
                                    contentDescription = stringResource(R.string.library_action_sort),
                                    size = 40.dp,
                                    iconSize = SoniqTheme.tokens.sizes.iconSmall,
                                )
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
                            SoniqGlassIconButton(
                                onClick = onRefresh,
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = stringResource(R.string.library_action_refresh),
                                size = 40.dp,
                                iconSize = SoniqTheme.tokens.sizes.iconSmall,
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
                    onNewPlaylistNameChanged = onNewPlaylistNameChanged,
                    onCreatePlaylist = onCreatePlaylist,
                    onRenamePlaylist = onRenamePlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onPlaylistClick = onPlaylistClick,
                    onAddTrackToPlaylist = onAddTrackToPlaylist,
                    onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist,
                    onToggleFavorite = onToggleFavorite,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onFolderClick = onFolderClick,
                    onRefresh = onRefresh,
                    onTrackClick = onTrackClick,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
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
    onNewPlaylistNameChanged: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlaylistClick: (PlaylistSummary) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onRemoveTrackFromPlaylist: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
    onArtistClick: (ArtistSummary) -> Unit,
    onFolderClick: (MediaFolderSummary) -> Unit,
    onRefresh: () -> Unit,
    onTrackClick: (AudioTrack, List<AudioTrack>) -> Unit,
    onPlayNext: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SoniqTheme.tokens.spacing
    var playlistPickerTrack by remember { mutableStateOf<AudioTrack?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (uiState.isSearchActive) {
            SoniqGlassSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChanged,
                placeholder = stringResource(R.string.music_search_hint),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.sm),
            )
        }

        if (uiState.activeFilter == null) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = spacing.screenHorizontal, vertical = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(items = MusicLibraryTab.entries, key = { it.name }) { tab ->
                    SoniqGlassPillChip(
                        label = stringResource(tab.labelRes()),
                        selected = tab == uiState.selectedTab,
                        onClick = { onSelectTab(tab) },
                    )
                }
            }
        }

        if (uiState.hasSyncError) {
            SyncErrorBanner(onRetry = onRefresh)
        }

        val isPlaylistDetail = uiState.activeFilter is MusicFilter.Playlist
        val showTracks = uiState.activeFilter != null || uiState.selectedTab == MusicLibraryTab.SONGS
        when {
            showTracks -> TrackListSection(
                tracks = uiState.tracks,
                favoriteKeys = uiState.favoriteKeys,
                hasPlaylists = uiState.playlists.isNotEmpty(),
                isPlaylistDetail = isPlaylistDetail,
                hasSearchQuery = uiState.searchQuery.isNotBlank(),
                onRefresh = onRefresh,
                onTrackClick = { track -> onTrackClick(track, uiState.tracks) },
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onToggleFavorite = onToggleFavorite,
                onRequestAddToPlaylist = { playlistPickerTrack = it },
                onRemoveFromPlaylist = onRemoveTrackFromPlaylist,
            )

            uiState.selectedTab == MusicLibraryTab.PLAYLISTS -> PlaylistsSection(
                playlists = uiState.playlists,
                newPlaylistDraft = uiState.newPlaylistDraft,
                onNewPlaylistNameChanged = onNewPlaylistNameChanged,
                onCreatePlaylist = onCreatePlaylist,
                onRenamePlaylist = onRenamePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onPlaylistClick = onPlaylistClick,
            )

            uiState.selectedTab == MusicLibraryTab.FAVORITES -> {
                if (uiState.favoriteTracks.isEmpty()) {
                    SoniqEmptyState(
                        icon = painterResource(R.drawable.ic_favorite_border),
                        title = stringResource(R.string.favorites_empty_title),
                        message = stringResource(R.string.favorites_empty_message),
                    )
                } else {
                    TrackListSection(
                        tracks = uiState.favoriteTracks,
                        favoriteKeys = uiState.favoriteKeys,
                        hasPlaylists = uiState.playlists.isNotEmpty(),
                        isPlaylistDetail = false,
                        hasSearchQuery = uiState.searchQuery.isNotBlank(),
                        onRefresh = onRefresh,
                        onTrackClick = { track -> onTrackClick(track, uiState.favoriteTracks) },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onToggleFavorite = onToggleFavorite,
                        onRequestAddToPlaylist = { playlistPickerTrack = it },
                        onRemoveFromPlaylist = onRemoveTrackFromPlaylist,
                    )
                }
            }

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

    val targetTrack = playlistPickerTrack
    if (targetTrack != null) {
        AlertDialog(
            onDismissRequest = { playlistPickerTrack = null },
            title = { Text(text = stringResource(R.string.playlist_add_to)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    uiState.playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                onAddTrackToPlaylist(playlist.id, targetTrack.id)
                                playlistPickerTrack = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = playlist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { playlistPickerTrack = null }) {
                    Text(text = stringResource(R.string.playlist_cancel))
                }
            },
        )
    }
}

@Composable
private fun PlaylistsSection(
    playlists: List<PlaylistSummary>,
    newPlaylistDraft: String,
    onNewPlaylistNameChanged: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlaylistClick: (PlaylistSummary) -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    var renamingPlaylist by remember { mutableStateOf<PlaylistSummary?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = spacing.md,
            bottom = spacing.xxxl + spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "create_playlist") {
            SoniqGlassCard(elevated = true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(SoniqTheme.glassMetrics.thumbnailRadius))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        SoniqTheme.glass.activePillStart,
                                        SoniqTheme.glass.activePillEnd,
                                    ),
                                ),
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.library_create_playlist_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.library_create_playlist_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newPlaylistDraft,
                        onValueChange = onNewPlaylistNameChanged,
                        placeholder = { Text(text = stringResource(R.string.playlist_new_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onCreatePlaylist,
                        enabled = newPlaylistDraft.isNotBlank(),
                    ) {
                        Text(text = stringResource(R.string.playlist_create))
                    }
                }
            }
        }

        if (playlists.isEmpty()) {
            item(key = "empty_playlists") {
                SoniqEmptyState(
                    icon = painterResource(R.drawable.ic_queue_music),
                    title = stringResource(R.string.playlist_empty_title),
                    message = stringResource(R.string.playlist_empty_message),
                )
            }
        } else {
            items(items = playlists, key = { it.id }) { playlist ->
                SoniqGlassCard(
                    onClick = { onPlaylistClick(playlist) },
                    contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm + 2.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtworkThumbnail(artworkUri = null, placeholderIconRes = R.drawable.ic_queue_music)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = trackCountLabel(playlist.trackCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = {
                                renamingPlaylist = playlist
                                renameDraft = playlist.name
                            },
                        ) {
                            Text(text = stringResource(R.string.playlist_rename))
                        }
                        IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.playlist_delete, playlist.name),
                            )
                        }
                    }
                }
            }
        }
    }

    val activeRename = renamingPlaylist
    if (activeRename != null) {
        AlertDialog(
            onDismissRequest = { renamingPlaylist = null },
            title = { Text(text = stringResource(R.string.playlist_rename_title)) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenamePlaylist(activeRename.id, renameDraft)
                        renamingPlaylist = null
                    },
                    enabled = renameDraft.isNotBlank(),
                ) {
                    Text(text = stringResource(R.string.playlist_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingPlaylist = null }) {
                    Text(text = stringResource(R.string.playlist_cancel))
                }
            },
        )
    }
}

@Composable
private fun TrackListSection(
    tracks: List<AudioTrack>,
    favoriteKeys: Set<String>,
    hasPlaylists: Boolean,
    isPlaylistDetail: Boolean,
    hasSearchQuery: Boolean,
    onRefresh: () -> Unit,
    onTrackClick: (AudioTrack) -> Unit,
    onPlayNext: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onRequestAddToPlaylist: (AudioTrack) -> Unit,
    onRemoveFromPlaylist: (Long) -> Unit,
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
            AudioTrackRow(
                track = track,
                isFavorite = mediaKey(MediaKind.AUDIO, track.id) in favoriteKeys,
                hasPlaylists = hasPlaylists,
                isPlaylistDetail = isPlaylistDetail,
                onClick = { onTrackClick(track) },
                onPlayNext = { onPlayNext(track) },
                onAddToQueue = { onAddToQueue(track) },
                onToggleFavorite = { onToggleFavorite(track.id) },
                onRequestAddToPlaylist = { onRequestAddToPlaylist(track) },
                onRemoveFromPlaylist = { onRemoveFromPlaylist(track.id) },
            )
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
    isFavorite: Boolean,
    hasPlaylists: Boolean,
    isPlaylistDetail: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRequestAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
) {
    val spacing = SoniqTheme.tokens.spacing
    var menuExpanded by remember { mutableStateOf(false) }

    SoniqGlassCard(
        onClick = onClick,
        contentPadding = PaddingValues(start = spacing.md, top = spacing.sm + 2.dp, bottom = spacing.sm + 2.dp, end = spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.track_action_more),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.track_action_play_next)) },
                        onClick = {
                            menuExpanded = false
                            onPlayNext()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.track_action_add_queue)) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(
                                    if (isFavorite) R.string.track_action_unfavorite else R.string.track_action_favorite,
                                ),
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        },
                    )
                    if (hasPlaylists) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.playlist_add_to)) },
                            onClick = {
                                menuExpanded = false
                                onRequestAddToPlaylist()
                            },
                        )
                    }
                    if (isPlaylistDetail) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.playlist_remove_track)) },
                            onClick = {
                                menuExpanded = false
                                onRemoveFromPlaylist()
                            },
                        )
                    }
                }
            }
        }
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
    SoniqGlassCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.sm + 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun ArtworkThumbnail(
    artworkUri: String?,
    placeholderIconRes: Int,
) {
    val sizes = SoniqTheme.tokens.sizes
    val glass = SoniqTheme.glass
    val metrics = SoniqTheme.glassMetrics
    val shape = RoundedCornerShape(metrics.thumbnailRadius)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizes.trackArtwork)
            .clip(shape)
            .background(Brush.linearGradient(listOf(glass.surfaceElevated, glass.surfaceBottom)))
            .border(metrics.borderWidth, glass.borderSubtle, shape),
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
    MusicLibraryTab.PLAYLISTS -> R.string.music_tab_playlists
    MusicLibraryTab.FAVORITES -> R.string.music_tab_favorites
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
