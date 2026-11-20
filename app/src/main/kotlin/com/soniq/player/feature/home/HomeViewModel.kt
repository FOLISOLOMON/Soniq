package com.soniq.player.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soniq.player.core.mediadiscovery.domain.MusicLibraryRepository
import com.soniq.player.core.mediadiscovery.domain.VideoLibraryRepository
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.userlibrary.domain.UserLibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Unidirectional UI state for SONIQ's Liquid Glass Home experience. */
data class HomeUiState(
    val searchQuery: String = "",
    val featuredTrack: AudioTrack? = null,
    val recentlyPlayed: List<AudioTrack> = emptyList(),
    val searchResults: List<AudioTrack> = emptyList(),
    val totalTracks: Int = 0,
    val totalAlbums: Int = 0,
    val totalPlaylists: Int = 0,
    val totalVideos: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    musicRepository: MusicLibraryRepository,
    videoRepository: VideoLibraryRepository,
    userLibraryRepository: UserLibraryRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        musicRepository.tracks,
        musicRepository.albums,
        videoRepository.videos,
        userLibraryRepository.playlists,
        userLibraryRepository.recentHistory,
        searchQuery,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val tracks = values[0] as List<AudioTrack>
        @Suppress("UNCHECKED_CAST")
        val albums = values[1] as List<*>
        @Suppress("UNCHECKED_CAST")
        val videos = values[2] as List<*>
        @Suppress("UNCHECKED_CAST")
        val playlists = values[3] as List<*>
        @Suppress("UNCHECKED_CAST")
        val history = values[4] as List<com.soniq.player.core.model.media.PlaybackHistoryEntry>
        val query = values[5] as String

        val trackById = tracks.associateBy { it.id }
        val recentTracks = history
            .asSequence()
            .filter { it.kind == MediaKind.AUDIO }
            .mapNotNull { entry -> trackById[entry.mediaId] }
            .distinctBy { it.id }
            .take(MAX_RECENT_TRACKS)
            .toList()

        val featured = recentTracks.firstOrNull() ?: tracks.firstOrNull()
        val trimmed = query.trim()
        val searchMatches = if (trimmed.isEmpty()) {
            emptyList()
        } else {
            tracks.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.artist.contains(trimmed, ignoreCase = true) ||
                    it.album.contains(trimmed, ignoreCase = true)
            }.take(MAX_SEARCH_RESULTS)
        }

        HomeUiState(
            searchQuery = query,
            featuredTrack = featured,
            recentlyPlayed = recentTracks,
            searchResults = searchMatches,
            totalTracks = tracks.size,
            totalAlbums = albums.size,
            totalPlaylists = playlists.size,
            totalVideos = videos.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_RECENT_TRACKS = 10
        const val MAX_SEARCH_RESULTS = 25
    }
}
