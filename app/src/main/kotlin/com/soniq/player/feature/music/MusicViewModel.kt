package com.soniq.player.feature.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniq.player.core.common.suspendRunCatching
import com.soniq.player.core.mediadiscovery.domain.MusicLibraryRepository
import com.soniq.player.core.model.media.AlbumSummary
import com.soniq.player.core.model.media.ArtistSummary
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.LibrarySyncState
import com.soniq.player.core.model.media.MediaFolderSummary
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.MusicSortOrder
import com.soniq.player.core.model.media.PlaylistSummary
import com.soniq.player.core.model.media.mediaKey
import com.soniq.player.core.permissions.MediaAccessGrant
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.PermissionChecker
import com.soniq.player.core.userlibrary.data.DefaultUserLibraryRepository
import com.soniq.player.core.userlibrary.domain.UserLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Top-level library tabs inside the Music experience. */
enum class MusicLibraryTab { SONGS, PLAYLISTS, FAVORITES, ALBUMS, ARTISTS, FOLDERS }

/** Optional drill-down filter applied when the user selects a Playlist, Album, Artist, or Folder. */
sealed interface MusicFilter {
    val label: String

    data class Playlist(val playlistId: Long, val name: String) : MusicFilter {
        override val label: String get() = name
    }

    data class Album(val albumId: Long, val title: String) : MusicFilter {
        override val label: String get() = title
    }

    data class Artist(val artistName: String) : MusicFilter {
        override val label: String get() = artistName
    }

    data class Folder(val folderPath: String, val folderName: String) : MusicFilter {
        override val label: String get() = folderName
    }
}

/** Unidirectional UI state for the Music destination. */
data class MusicUiState(
    val accessState: MediaAccessState = MediaAccessState.NOT_GRANTED,
    val selectedTab: MusicLibraryTab = MusicLibraryTab.SONGS,
    val sortOrder: MusicSortOrder = MusicSortOrder.TITLE_ASC,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val activeFilter: MusicFilter? = null,
    val tracks: List<AudioTrack> = emptyList(),
    val favoriteTracks: List<AudioTrack> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val newPlaylistDraft: String = "",
    val albums: List<AlbumSummary> = emptyList(),
    val artists: List<ArtistSummary> = emptyList(),
    val folders: List<MediaFolderSummary> = emptyList(),
    val isScanning: Boolean = false,
    val hasSyncError: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val repository: MusicLibraryRepository,
    private val userLibraryRepository: UserLibraryRepository,
) : ViewModel() {

    private data class ControlsState(
        val accessState: MediaAccessState,
        val selectedTab: MusicLibraryTab = MusicLibraryTab.SONGS,
        val sortOrder: MusicSortOrder = MusicSortOrder.TITLE_ASC,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val activeFilter: MusicFilter? = null,
        val newPlaylistDraft: String = "",
    )

    private val controls = MutableStateFlow(ControlsState(accessState = initialAccessState()))
    private var changeObserverJob: Job? = null
    private var hasSyncedOnce = false

    private val activePlaylistTracksFlow = controls
        .map { (it.activeFilter as? MusicFilter.Playlist)?.playlistId }
        .distinctUntilChanged()
        .flatMapLatest { playlistId ->
            if (playlistId == null) {
                flowOf<List<AudioTrack>?>(null)
            } else {
                userLibraryRepository.observePlaylistTracks(playlistId)
            }
        }

    private val userLibrarySnapshotFlow = combine(
        userLibraryRepository.playlists,
        userLibraryRepository.favoriteKeys,
        activePlaylistTracksFlow,
    ) { playlists, favorites, playlistTracks ->
        Triple(playlists, favorites, playlistTracks)
    }

    val uiState: StateFlow<MusicUiState> = combine(
        controls,
        repository.tracks,
        repository.albums,
        repository.artists,
        repository.folders,
        repository.syncState,
        userLibrarySnapshotFlow,
    ) { values ->
        val ctrl = values[0] as ControlsState
        @Suppress("UNCHECKED_CAST")
        val allTracks = values[1] as List<AudioTrack>
        @Suppress("UNCHECKED_CAST")
        val allAlbums = values[2] as List<AlbumSummary>
        @Suppress("UNCHECKED_CAST")
        val allArtists = values[3] as List<ArtistSummary>
        @Suppress("UNCHECKED_CAST")
        val allFolders = values[4] as List<MediaFolderSummary>
        val syncState = values[5] as LibrarySyncState
        @Suppress("UNCHECKED_CAST")
        val (allPlaylists, favoriteKeys, playlistTracks) =
            values[6] as Triple<List<PlaylistSummary>, Set<String>, List<AudioTrack>?>

        val query = ctrl.searchQuery.trim()
        val baseTracks = if (ctrl.activeFilter is MusicFilter.Playlist) {
            playlistTracks.orEmpty()
        } else {
            allTracks.filterBy(ctrl.activeFilter)
        }

        val filteredTracks = baseTracks
            .filter { track ->
                query.isEmpty() ||
                    track.title.contains(query, ignoreCase = true) ||
                    track.artist.contains(query, ignoreCase = true) ||
                    track.album.contains(query, ignoreCase = true)
            }
            .let { list ->
                // Preserve user playlist order inside a playlist filter.
                if (ctrl.activeFilter is MusicFilter.Playlist) list else list.sortedBy(ctrl.sortOrder)
            }

        val favoriteTracks = allTracks
            .filter { mediaKey(MediaKind.AUDIO, it.id) in favoriteKeys }
            .filter { track ->
                query.isEmpty() ||
                    track.title.contains(query, ignoreCase = true) ||
                    track.artist.contains(query, ignoreCase = true) ||
                    track.album.contains(query, ignoreCase = true)
            }
            .sortedBy(ctrl.sortOrder)

        val filteredPlaylists = if (query.isEmpty()) {
            allPlaylists
        } else {
            allPlaylists.filter { it.name.contains(query, ignoreCase = true) }
        }
        val filteredAlbums = if (query.isEmpty()) {
            allAlbums
        } else {
            allAlbums.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true)
            }
        }
        val filteredArtists = if (query.isEmpty()) {
            allArtists
        } else {
            allArtists.filter { it.name.contains(query, ignoreCase = true) }
        }
        val filteredFolders = if (query.isEmpty()) {
            allFolders
        } else {
            allFolders.filter {
                it.folderName.contains(query, ignoreCase = true) ||
                    it.folderPath.contains(query, ignoreCase = true)
            }
        }

        MusicUiState(
            accessState = ctrl.accessState,
            selectedTab = ctrl.selectedTab,
            sortOrder = ctrl.sortOrder,
            searchQuery = ctrl.searchQuery,
            isSearchActive = ctrl.isSearchActive,
            activeFilter = ctrl.activeFilter,
            tracks = filteredTracks,
            favoriteTracks = favoriteTracks,
            favoriteKeys = favoriteKeys,
            playlists = filteredPlaylists,
            newPlaylistDraft = ctrl.newPlaylistDraft,
            albums = filteredAlbums,
            artists = filteredArtists,
            folders = filteredFolders,
            isScanning = syncState is LibrarySyncState.Scanning,
            hasSyncError = syncState is LibrarySyncState.Error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MusicUiState(accessState = controls.value.accessState),
    )

    init {
        if (controls.value.accessState.hasAccess()) {
            startObservingAndSyncIfNeeded()
        }
    }

    fun refreshPermission() {
        val grant = permissionChecker.checkMediaAccess(MediaKind.AUDIO)
        val newAccess = when (grant) {
            MediaAccessGrant.FULL -> MediaAccessState.GRANTED
            MediaAccessGrant.PARTIAL -> MediaAccessState.PARTIAL
            MediaAccessGrant.NONE -> if (controls.value.accessState == MediaAccessState.DENIED_PERMANENTLY) {
                MediaAccessState.DENIED_PERMANENTLY
            } else {
                MediaAccessState.NOT_GRANTED
            }
        }
        controls.update { it.copy(accessState = newAccess) }
        if (newAccess.hasAccess()) {
            startObservingAndSyncIfNeeded()
        } else {
            stopObserving()
        }
    }

    fun onPermissionResult(grant: MediaAccessGrant, permanentlyDenied: Boolean) {
        val newAccess = when (grant) {
            MediaAccessGrant.FULL -> MediaAccessState.GRANTED
            MediaAccessGrant.PARTIAL -> MediaAccessState.PARTIAL
            MediaAccessGrant.NONE -> if (permanentlyDenied) {
                MediaAccessState.DENIED_PERMANENTLY
            } else {
                MediaAccessState.NOT_GRANTED
            }
        }
        controls.update { it.copy(accessState = newAccess) }
        if (newAccess.hasAccess()) {
            startObservingAndSyncIfNeeded(forceSync = true)
        } else {
            stopObserving()
        }
    }

    fun selectTab(tab: MusicLibraryTab) {
        controls.update { it.copy(selectedTab = tab, activeFilter = null) }
    }

    fun selectSortOrder(order: MusicSortOrder) {
        controls.update { it.copy(sortOrder = order) }
    }

    fun setSearchActive(active: Boolean) {
        controls.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (active) it.searchQuery else "",
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        controls.update { it.copy(searchQuery = query) }
    }

    fun onNewPlaylistNameChanged(name: String) {
        controls.update {
            it.copy(newPlaylistDraft = name.take(DefaultUserLibraryRepository.MAX_PLAYLIST_NAME_LENGTH))
        }
    }

    fun createPlaylist() {
        val name = controls.value.newPlaylistDraft.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            suspendRunCatching { userLibraryRepository.createPlaylist(name) }
                .onSuccess { controls.update { it.copy(newPlaylistDraft = "") } }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            suspendRunCatching { userLibraryRepository.renamePlaylist(playlistId, trimmed) }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            suspendRunCatching { userLibraryRepository.deletePlaylist(playlistId) }
                .onSuccess {
                    controls.update { current ->
                        val active = current.activeFilter as? MusicFilter.Playlist
                        if (active?.playlistId == playlistId) current.copy(activeFilter = null) else current
                    }
                }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            suspendRunCatching { userLibraryRepository.addTrackToPlaylist(playlistId, trackId) }
        }
    }

    fun removeTrackFromActivePlaylist(trackId: Long) {
        val activePlaylist = controls.value.activeFilter as? MusicFilter.Playlist ?: return
        viewModelScope.launch {
            suspendRunCatching {
                userLibraryRepository.removeTrackFromPlaylist(activePlaylist.playlistId, trackId)
            }
        }
    }

    fun toggleTrackFavorite(trackId: Long) {
        viewModelScope.launch {
            suspendRunCatching { userLibraryRepository.toggleFavorite(MediaKind.AUDIO, trackId) }
        }
    }

    fun selectPlaylist(playlist: PlaylistSummary) {
        controls.update {
            it.copy(activeFilter = MusicFilter.Playlist(playlist.id, playlist.name))
        }
    }

    fun selectAlbum(album: AlbumSummary) {
        controls.update {
            it.copy(activeFilter = MusicFilter.Album(album.albumId, album.title))
        }
    }

    fun selectArtist(artist: ArtistSummary) {
        controls.update {
            it.copy(activeFilter = MusicFilter.Artist(artist.name))
        }
    }

    fun selectFolder(folder: MediaFolderSummary) {
        controls.update {
            it.copy(activeFilter = MusicFilter.Folder(folder.folderPath, folder.folderName))
        }
    }

    fun clearFilter() {
        controls.update { it.copy(activeFilter = null) }
    }

    fun refreshLibrary() {
        if (!controls.value.accessState.hasAccess()) return
        viewModelScope.launch {
            hasSyncedOnce = true
            repository.sync()
        }
    }

    private fun startObservingAndSyncIfNeeded(forceSync: Boolean = false) {
        if (forceSync || !hasSyncedOnce) {
            refreshLibrary()
        }
        if (changeObserverJob == null) {
            changeObserverJob = viewModelScope.launch {
                repository.observeExternalChanges()
                    .debounce(MEDIASTORE_DEBOUNCE_MILLIS)
                    .collect { repository.sync() }
            }
        }
    }

    private fun stopObserving() {
        changeObserverJob?.cancel()
        changeObserverJob = null
    }

    private fun initialAccessState(): MediaAccessState =
        when (permissionChecker.checkMediaAccess(MediaKind.AUDIO)) {
            MediaAccessGrant.FULL -> MediaAccessState.GRANTED
            MediaAccessGrant.PARTIAL -> MediaAccessState.PARTIAL
            MediaAccessGrant.NONE -> MediaAccessState.NOT_GRANTED
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MEDIASTORE_DEBOUNCE_MILLIS = 750L

        fun MediaAccessState.hasAccess(): Boolean =
            this == MediaAccessState.GRANTED || this == MediaAccessState.PARTIAL

        fun List<AudioTrack>.filterBy(filter: MusicFilter?): List<AudioTrack> = when (filter) {
            null, is MusicFilter.Playlist -> this
            is MusicFilter.Album -> filter { it.albumId == filter.albumId && it.album == filter.title }
            is MusicFilter.Artist -> filter { it.artist == filter.artistName }
            is MusicFilter.Folder -> filter { it.folderPath == filter.folderPath }
        }

        fun List<AudioTrack>.sortedBy(order: MusicSortOrder): List<AudioTrack> = when (order) {
            MusicSortOrder.TITLE_ASC -> sortedBy { it.title.lowercase() }
            MusicSortOrder.ARTIST_ASC -> sortedWith(
                compareBy<AudioTrack> { it.artist.lowercase() }
                    .thenBy { it.album.lowercase() }
                    .thenBy { it.trackNumber }
                    .thenBy { it.title.lowercase() },
            )
            MusicSortOrder.DATE_ADDED_DESC -> sortedByDescending { it.dateAddedSeconds }
            MusicSortOrder.DURATION_DESC -> sortedByDescending { it.durationMs }
        }
    }
}
