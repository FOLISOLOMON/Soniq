package com.soniq.player.core.mediadiscovery

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.soniq.player.core.common.di.IoDispatcher
import com.soniq.player.core.model.media.MediaKind
import com.soniq.player.core.model.media.VideoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import javax.inject.Inject
import javax.inject.Singleton

/** Scans local video files from Android's MediaStore. */
interface LocalVideoScanner {
    suspend fun scan(): List<VideoItem>
}

@Singleton
class MediaStoreVideoScanner @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LocalVideoScanner {

    override suspend fun scan(): List<VideoItem> = withContext(ioDispatcher) {
        val collectionUri = MediaStoreCollections.collectionUri(MediaKind.VIDEO)
        val sdkInt = Build.VERSION.SDK_INT
        val projection = buildProjection(sdkInt)
        val selection = "${MediaStore.Video.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        contentResolver.query(collectionUri, projection, selection, null, sortOrder)?.use { cursor ->
            mapCursor(cursor, collectionUri, sdkInt) {
                coroutineContext.ensureActive()
            }
        }.orEmpty()
    }

    @SuppressLint("InlinedApi")
    internal fun buildProjection(sdkInt: Int): Array<String> {
        val pathColumn = if (sdkInt >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.MediaColumns.DATA
        }
        return arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            pathColumn,
        )
    }

    @SuppressLint("InlinedApi")
    internal fun mapCursor(
        cursor: Cursor,
        collectionUri: Uri,
        sdkInt: Int = Build.VERSION.SDK_INT,
        onBatchCheck: () -> Unit = {},
    ): List<VideoItem> {
        val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
        if (idCol < 0) return emptyList()

        val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
        val displayNameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
        val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
        val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
        val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
        val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
        val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
        val dateAddedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
        val dateModifiedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
        val pathCol = if (sdkInt >= Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
        }

        val videos = ArrayList<VideoItem>(cursor.count.coerceAtLeast(0))
        var rowIndex = 0
        while (cursor.moveToNext()) {
            if (rowIndex++ % 250 == 0) onBatchCheck()
            val id = cursor.getLong(idCol)
            val durationMs = cursor.longOrZero(durationCol)
            if (durationMs <= 0L) continue

            val displayName = cursor.stringOrNull(displayNameCol)?.trim().orEmpty()
            val rawTitle = cursor.stringOrNull(titleCol)
            val title = rawTitle?.trim()?.takeIf { it.isNotEmpty() }
                ?: MediaStorePathNormalizer.stripExtension(displayName).ifEmpty { UNKNOWN_TITLE }
            val rawPath = cursor.stringOrNull(pathCol)
            val folder = if (sdkInt >= Build.VERSION_CODES.Q) {
                MediaStorePathNormalizer.fromRelativePath(rawPath)
            } else {
                MediaStorePathNormalizer.fromLegacyDataPath(rawPath)
            }

            videos += VideoItem(
                id = id,
                contentUri = ContentUris.withAppendedId(collectionUri, id).toString(),
                title = title,
                displayName = displayName.ifEmpty { title },
                durationMs = durationMs,
                sizeBytes = cursor.longOrZero(sizeCol),
                mimeType = cursor.stringOrNull(mimeCol).orEmpty().ifBlank { DEFAULT_VIDEO_MIME },
                width = cursor.intOrZero(widthCol),
                height = cursor.intOrZero(heightCol),
                folderPath = folder.path,
                folderName = folder.name,
                dateAddedSeconds = cursor.longOrZero(dateAddedCol),
                dateModifiedSeconds = cursor.longOrZero(dateModifiedCol),
            )
        }
        return videos
    }

    private companion object {
        const val UNKNOWN_TITLE = "Untitled Video"
        const val DEFAULT_VIDEO_MIME = "video/*"
    }
}
