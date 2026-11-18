package com.soniq.player.core.database.media.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Rebuildable Room cache row for a local MediaStore video file.
 * Stored in `soniq_media.db` (excluded from cloud backup rules).
 */
@Entity(
    tableName = "video_items",
    indices = [
        Index(value = ["folder_path"]),
        Index(value = ["title"]),
        Index(value = ["date_added"]),
    ],
)
data class VideoItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "content_uri")
    val contentUri: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "width")
    val width: Int,
    @ColumnInfo(name = "height")
    val height: Int,
    @ColumnInfo(name = "folder_path")
    val folderPath: String,
    @ColumnInfo(name = "folder_name")
    val folderName: String,
    @ColumnInfo(name = "date_added")
    val dateAddedSeconds: Long,
    @ColumnInfo(name = "date_modified")
    val dateModifiedSeconds: Long,
)
