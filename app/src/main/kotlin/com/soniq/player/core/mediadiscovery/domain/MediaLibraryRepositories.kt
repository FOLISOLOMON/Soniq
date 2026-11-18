package com.soniq.player.core.mediadiscovery.domain

import com.soniq.player.core.model.media.AlbumSummary
import com.soniq.player.core.model.media.ArtistSummary
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.LibrarySyncState
import com.soniq.player.core.model.media.MediaFolderSummary
import com.soniq.player.core.model.media.VideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Repository for the local Music library, logically separate from Video. */
interface MusicLibraryRepository {
    val tracks: Flow<List<AudioTrack>>
    val albums: Flow<List<AlbumSummary>>
    val artists: Flow<List<ArtistSummary>>
    val folders: Flow<List<MediaFolderSummary>>
    val syncState: StateFlow<LibrarySyncState>

    /** Scans MediaStore and updates the local Room cache. Returns the number of indexed tracks. */
    suspend fun sync(): Result<Int>

    /** Emits whenever the OS MediaStore audio collection changes. */
    fun observeExternalChanges(): Flow<Unit>
}

/** Repository for the local Video library, logically separate from Music. */
interface VideoLibraryRepository {
    val videos: Flow<List<VideoItem>>
    val folders: Flow<List<MediaFolderSummary>>
    val syncState: StateFlow<LibrarySyncState>

    /** Scans MediaStore and updates the local Room cache. Returns the number of indexed videos. */
    suspend fun sync(): Result<Int>

    /** Emits whenever the OS MediaStore video collection changes. */
    fun observeExternalChanges(): Flow<Unit>
}
