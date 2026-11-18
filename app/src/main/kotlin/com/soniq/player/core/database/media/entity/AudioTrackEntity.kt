package com.soniq.player.core.database.media.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Rebuildable Room cache row for a local MediaStore audio file.
 * Stored in `soniq_media.db` (excluded from cloud backup rules).
 */
@Entity(
    tableName = "audio_tracks",
    indices = [
        Index(value = ["album_id"]),
        Index(value = ["artist"]),
        Index(value = ["folder_path"]),
        Index(value = ["title"]),
        Index(value = ["date_added"]),
    ],
)
data class AudioTrackEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "content_uri")
    val contentUri: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "artist")
    val artist: String,
    @ColumnInfo(name = "album")
    val album: String,
    @ColumnInfo(name = "album_id")
    val albumId: Long,
    @ColumnInfo(name = "album_art_uri")
    val albumArtUri: String?,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "folder_path")
    val folderPath: String,
    @ColumnInfo(name = "folder_name")
    val folderName: String,
    @ColumnInfo(name = "track_number")
    val trackNumber: Int,
    @ColumnInfo(name = "year")
    val year: Int,
    @ColumnInfo(name = "date_added")
    val dateAddedSeconds: Long,
    @ColumnInfo(name = "date_modified")
    val dateModifiedSeconds: Long,
)
