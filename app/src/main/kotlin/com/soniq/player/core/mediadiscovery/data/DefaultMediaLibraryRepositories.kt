package com.soniq.player.core.mediadiscovery.data

import com.soniq.player.core.common.suspendRunCatching
import com.soniq.player.core.database.media.dao.AudioTrackDao
import com.soniq.player.core.database.media.dao.VideoItemDao
import com.soniq.player.core.mediadiscovery.LocalAudioScanner
import com.soniq.player.core.mediadiscovery.LocalVideoScanner
import com.soniq.player.core.mediadiscovery.MediaStoreChangeObserver
import com.soniq.player.core.mediadiscovery.domain.MusicLibraryRepository
import com.soniq.player.core.mediadiscovery.domain.VideoLibraryRepository
import com.soniq.player.core.model.media.AlbumSummary
import com.soniq.player.core.model.media.ArtistSummary
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.LibrarySyncState
import com.soniq.player.core.model.media.MediaFolderSummary
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMusicLibraryRepository @Inject constructor(
    private val scanner: LocalAudioScanner,
    private val audioTrackDao: AudioTrackDao,
    private val changeObserver: MediaStoreChangeObserver,
) : MusicLibraryRepository {

    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow<LibrarySyncState>(LibrarySyncState.Idle)
    override val syncState: StateFlow<LibrarySyncState> = _syncState.asStateFlow()

    override val tracks: Flow<List<AudioTrack>> = audioTrackDao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }

    override val albums: Flow<List<AlbumSummary>> = tracks.map { list ->
        list.groupBy { it.albumId to it.album }
            .map { (key, albumTracks) ->
                val first = albumTracks.first()
                AlbumSummary(
                    albumId = key.first,
                    title = key.second,
                    artist = first.artist,
                    albumArtUri = albumTracks.firstNotNullOfOrNull { it.albumArtUri },
                    trackCount = albumTracks.size,
                    totalDurationMs = albumTracks.sumOf { it.durationMs },
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    override val artists: Flow<List<ArtistSummary>> = tracks.map { list ->
        list.groupBy { it.artist }
            .map { (artistName, artistTracks) ->
                ArtistSummary(
                    name = artistName,
                    trackCount = artistTracks.size,
                    albumCount = artistTracks.map { it.albumId to it.album }.distinct().size,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    override val folders: Flow<List<MediaFolderSummary>> = tracks.map { list ->
        list.groupBy { it.folderPath }
            .map { (path, folderTracks) ->
                MediaFolderSummary(
                    folderPath = path,
                    folderName = folderTracks.first().folderName,
                    itemCount = folderTracks.size,
                    totalDurationMs = folderTracks.sumOf { it.durationMs },
                )
            }
            .sortedBy { it.folderName.lowercase() }
    }

    override suspend fun sync(): Result<Int> = syncMutex.withLock {
        _syncState.value = LibrarySyncState.Scanning
        suspendRunCatching {
            val scanned = scanner.scan()
            audioTrackDao.syncWith(scanned.map { it.toEntity() })
            scanned.size
        }.onSuccess {
            _syncState.value = LibrarySyncState.Idle
        }.onFailure { error ->
            _syncState.value = LibrarySyncState.Error(error)
        }
    }

    override fun observeExternalChanges(): Flow<Unit> = changeObserver.observeChanges(MediaKind.AUDIO)
}

@Singleton
class DefaultVideoLibraryRepository @Inject constructor(
    private val scanner: LocalVideoScanner,
    private val videoItemDao: VideoItemDao,
    private val changeObserver: MediaStoreChangeObserver,
) : VideoLibraryRepository {

    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow<LibrarySyncState>(LibrarySyncState.Idle)
    override val syncState: StateFlow<LibrarySyncState> = _syncState.asStateFlow()

    override val videos: Flow<List<VideoItem>> = videoItemDao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }

    override val folders: Flow<List<MediaFolderSummary>> = videos.map { list ->
        list.groupBy { it.folderPath }
            .map { (path, folderVideos) ->
                MediaFolderSummary(
                    folderPath = path,
                    folderName = folderVideos.first().folderName,
                    itemCount = folderVideos.size,
                    totalDurationMs = folderVideos.sumOf { it.durationMs },
                )
            }
            .sortedBy { it.folderName.lowercase() }
    }

    override suspend fun sync(): Result<Int> = syncMutex.withLock {
        _syncState.value = LibrarySyncState.Scanning
        suspendRunCatching {
            val scanned = scanner.scan()
            videoItemDao.syncWith(scanned.map { it.toEntity() })
            scanned.size
        }.onSuccess {
            _syncState.value = LibrarySyncState.Idle
        }.onFailure { error ->
            _syncState.value = LibrarySyncState.Error(error)
        }
    }

    override fun observeExternalChanges(): Flow<Unit> = changeObserver.observeChanges(MediaKind.VIDEO)
}
