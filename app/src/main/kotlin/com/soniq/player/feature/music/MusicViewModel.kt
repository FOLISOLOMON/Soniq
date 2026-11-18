package com.soniq.player.feature.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniq.player.core.mediadiscovery.domain.MusicLibraryRepository
import com.soniq.player.core.model.media.AlbumSummary
import com.soniq.player.core.model.media.ArtistSummary
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.LibrarySyncState
import com.soniq.player.core.model.media.MediaFolderSummary
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.MusicSortOrder
import com.soniq.player.core.permissions.MediaAccessGrant
import com.soniq.player.core.permissions.MediaAccessState
import com.soniq.player.core.permissions.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Top-level library tabs inside the Music experience. */
enum class MusicLibraryTab { SONGS, ALBUMS, ARTISTS, FOLDERS }

/** Optional drill-down filter applied when the user selects an Album, Artist, or Folder. */
sealed interface MusicFilter {
    val label: String

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
    val albums: List<AlbumSummary> = emptyList(),
    val artists: List<ArtistSummary> = emptyList(),
    val folders: List<MediaFolderSummary> = emptyList(),
    val isScanning: Boolean = false,
    val hasSyncError: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val repository: MusicLibraryRepository,
) : ViewModel() {

    private data class ControlsState(
        val accessState: MediaAccessState,
        val selectedTab: MusicLibraryTab = MusicLibraryTab.SONGS,
        val sortOrder: MusicSortOrder = MusicSortOrder.TITLE_ASC,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val activeFilter: MusicFilter? = null,
    )

    private val controls = MutableStateFlow(ControlsState(accessState = initialAccessState()))
    private var changeObserverJob: Job? = null
    private var hasSyncedOnce = false

    val uiState: StateFlow<MusicUiState> = combine(
        controls,
        repository.tracks,
        repository.albums,
        repository.artists,
        repository.folders,
        repository.syncState,
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

        val query = ctrl.searchQuery.trim()
        val filteredTracks = allTracks
            .filterBy(ctrl.activeFilter)
            .filter { track ->
                query.isEmpty() ||
                    track.title.contains(query, ignoreCase = true) ||
                    track.artist.contains(query, ignoreCase = true) ||
                    track.album.contains(query, ignoreCase = true)
            }
            .sortedBy(ctrl.sortOrder)

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

    /** Re-checks permission state, e.g., when returning from system app settings. */
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
            null -> this
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
