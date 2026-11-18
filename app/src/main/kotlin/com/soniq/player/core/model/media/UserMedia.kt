package com.soniq.player.core.model.media

/** Builds the canonical persistent media key shared across Favorites, History, and Resume tables. */
fun mediaKey(kind: MediaKind, mediaId: Long): String = "${kind.name.lowercase()}:$mediaId"

/** Summary of a user-authored custom playlist. */
data class PlaylistSummary(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

/** A recorded local playback history entry. */
data class PlaybackHistoryEntry(
    val id: Long,
    val mediaKey: String,
    val kind: MediaKind,
    val mediaId: Long,
    val title: String,
    val subtitle: String?,
    val playedAtEpochMillis: Long,
)

/** Persisted playback resume timestamp for a media item. */
data class MediaResumePosition(
    val mediaKey: String,
    val kind: MediaKind,
    val mediaId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtEpochMillis: Long,
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
