package com.soniq.player.feature.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniq.player.core.mediadiscovery.domain.VideoLibraryRepository
import com.soniq.player.core.model.media.LibrarySyncState
import com.soniq.player.core.model.media.MediaFolderSummary
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import com.soniq.player.core.model.media.VideoSortOrder
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

/** Top-level library tabs inside the Video experience. */
enum class VideoLibraryTab { VIDEOS, FOLDERS }

/** Optional folder drill-down filter for the Video library. */
data class VideoFolderFilter(
    val folderPath: String,
    val folderName: String,
)

/** Unidirectional UI state for the Video destination. */
data class VideoUiState(
    val accessState: MediaAccessState = MediaAccessState.NOT_GRANTED,
    val selectedTab: VideoLibraryTab = VideoLibraryTab.VIDEOS,
    val sortOrder: VideoSortOrder = VideoSortOrder.DATE_ADDED_DESC,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val activeFolder: VideoFolderFilter? = null,
    val videos: List<VideoItem> = emptyList(),
    val folders: List<MediaFolderSummary> = emptyList(),
    val isScanning: Boolean = false,
    val hasSyncError: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class VideoViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val repository: VideoLibraryRepository,
) : ViewModel() {

    private data class ControlsState(
        val accessState: MediaAccessState,
        val selectedTab: VideoLibraryTab = VideoLibraryTab.VIDEOS,
        val sortOrder: VideoSortOrder = VideoSortOrder.DATE_ADDED_DESC,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val activeFolder: VideoFolderFilter? = null,
    )

    private val controls = MutableStateFlow(ControlsState(accessState = initialAccessState()))
    private var changeObserverJob: Job? = null
    private var hasSyncedOnce = false

    val uiState: StateFlow<VideoUiState> = combine(
        controls,
        repository.videos,
        repository.folders,
        repository.syncState,
    ) { ctrl, allVideos, allFolders, syncState ->
        val query = ctrl.searchQuery.trim()
        val filteredVideos = allVideos
            .let { list ->
                val folder = ctrl.activeFolder
                if (folder == null) list else list.filter { it.folderPath == folder.folderPath }
            }
            .filter { video ->
                query.isEmpty() ||
                    video.title.contains(query, ignoreCase = true) ||
                    video.folderName.contains(query, ignoreCase = true)
            }
            .sortedBy(ctrl.sortOrder)

        val filteredFolders = if (query.isEmpty()) {
            allFolders
        } else {
            allFolders.filter {
                it.folderName.contains(query, ignoreCase = true) ||
                    it.folderPath.contains(query, ignoreCase = true)
            }
        }

        VideoUiState(
            accessState = ctrl.accessState,
            selectedTab = ctrl.selectedTab,
            sortOrder = ctrl.sortOrder,
            searchQuery = ctrl.searchQuery,
            isSearchActive = ctrl.isSearchActive,
            activeFolder = ctrl.activeFolder,
            videos = filteredVideos,
            folders = filteredFolders,
            isScanning = syncState is LibrarySyncState.Scanning,
            hasSyncError = syncState is LibrarySyncState.Error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = VideoUiState(accessState = controls.value.accessState),
    )

    init {
        if (controls.value.accessState.hasAccess()) {
            startObservingAndSyncIfNeeded()
        }
    }

    /** Re-checks permission state, e.g., when returning from system app settings. */
    fun refreshPermission() {
        val grant = permissionChecker.checkMediaAccess(MediaKind.VIDEO)
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
            // Re-scan whenever full or updated partial video selection is granted.
            startObservingAndSyncIfNeeded(forceSync = true)
        } else {
            stopObserving()
        }
    }

    fun selectTab(tab: VideoLibraryTab) {
        controls.update { it.copy(selectedTab = tab, activeFolder = null) }
    }

    fun selectSortOrder(order: VideoSortOrder) {
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

    fun selectFolder(folder: MediaFolderSummary) {
        controls.update {
            it.copy(activeFolder = VideoFolderFilter(folder.folderPath, folder.folderName))
        }
    }

    fun clearFilter() {
        controls.update { it.copy(activeFolder = null) }
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
        when (permissionChecker.checkMediaAccess(MediaKind.VIDEO)) {
            MediaAccessGrant.FULL -> MediaAccessState.GRANTED
            MediaAccessGrant.PARTIAL -> MediaAccessState.PARTIAL
            MediaAccessGrant.NONE -> MediaAccessState.NOT_GRANTED
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MEDIASTORE_DEBOUNCE_MILLIS = 750L

        fun MediaAccessState.hasAccess(): Boolean =
            this == MediaAccessState.GRANTED || this == MediaAccessState.PARTIAL

        fun List<VideoItem>.sortedBy(order: VideoSortOrder): List<VideoItem> = when (order) {
            VideoSortOrder.DATE_ADDED_DESC -> sortedByDescending { it.dateAddedSeconds }
            VideoSortOrder.TITLE_ASC -> sortedBy { it.title.lowercase() }
            VideoSortOrder.DURATION_DESC -> sortedByDescending { it.durationMs }
            VideoSortOrder.SIZE_DESC -> sortedByDescending { it.sizeBytes }
        }
    }
}
