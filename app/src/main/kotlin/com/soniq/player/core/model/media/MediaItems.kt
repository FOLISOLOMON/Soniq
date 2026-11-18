package com.soniq.player.core.model.media

/** Pure domain representation of a locally indexed audio file from MediaStore. */
data class AudioTrack(
    val id: Long,
    val contentUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val albumArtUri: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val folderPath: String,
    val folderName: String,
    val trackNumber: Int,
    val year: Int,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
)

/** Pure domain representation of a locally indexed video file from MediaStore. */
data class VideoItem(
    val id: Long,
    val contentUri: String,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val folderPath: String,
    val folderName: String,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
)

/** Aggregated album summary derived from indexed [AudioTrack] rows. */
data class AlbumSummary(
    val albumId: Long,
    val title: String,
    val artist: String,
    val albumArtUri: String?,
    val trackCount: Int,
    val totalDurationMs: Long,
)

/** Aggregated artist summary derived from indexed [AudioTrack] rows. */
data class ArtistSummary(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
)

/** Aggregated folder summary shared by Music and Video libraries. */
data class MediaFolderSummary(
    val folderPath: String,
    val folderName: String,
    val itemCount: Int,
    val totalDurationMs: Long,
)

/** Sorting options for local audio tracks. */
enum class MusicSortOrder {
    TITLE_ASC,
    ARTIST_ASC,
    DATE_ADDED_DESC,
    DURATION_DESC,
}

/** Sorting options for local video items. */
enum class VideoSortOrder {
    DATE_ADDED_DESC,
    TITLE_ASC,
    DURATION_DESC,
    SIZE_DESC,
}

/** Sync lifecycle state reported by media library repositories. */
sealed interface LibrarySyncState {
    data object Idle : LibrarySyncState
    data object Scanning : LibrarySyncState
    data class Error(val cause: Throwable) : LibrarySyncState
}
