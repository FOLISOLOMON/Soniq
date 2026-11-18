package com.soniq.player.core.mediadiscovery

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.soniq.player.core.common.di.IoDispatcher
import com.soniq.player.core.model.media.AudioTrack
import com.soniq.player.core.model.media.MediaKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import javax.inject.Inject
import javax.inject.Singleton

/** Scans local audio files from Android's MediaStore. */
interface LocalAudioScanner {
    suspend fun scan(): List<AudioTrack>
}

@Singleton
class MediaStoreAudioScanner @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LocalAudioScanner {

    override suspend fun scan(): List<AudioTrack> = withContext(ioDispatcher) {
        val collectionUri = MediaStoreCollections.collectionUri(MediaKind.AUDIO)
        val sdkInt = Build.VERSION.SDK_INT
        val projection = buildProjection(sdkInt)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

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
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            pathColumn,
        )
    }

    @SuppressLint("InlinedApi")
    internal fun mapCursor(
        cursor: Cursor,
        collectionUri: Uri,
        sdkInt: Int = Build.VERSION.SDK_INT,
        onBatchCheck: () -> Unit = {},
    ): List<AudioTrack> {
        val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        if (idCol < 0) return emptyList()

        val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
        val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
        val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
        val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
        val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
        val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
        val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val dateModifiedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
        val pathCol = if (sdkInt >= Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
        }

        val tracks = ArrayList<AudioTrack>(cursor.count.coerceAtLeast(0))
        var rowIndex = 0
        while (cursor.moveToNext()) {
            if (rowIndex++ % 250 == 0) onBatchCheck()
            val id = cursor.getLong(idCol)
            val durationMs = cursor.longOrZero(durationCol)
            if (durationMs <= 0L) continue

            val displayName = cursor.stringOrNull(displayNameCol)
            val rawTitle = cursor.stringOrNull(titleCol)
            val title = rawTitle?.trim()?.takeIf { it.isNotEmpty() }
                ?: MediaStorePathNormalizer.stripExtension(displayName).ifEmpty { UNKNOWN_TITLE }
            val artist = MediaStorePathNormalizer.normalizeTag(cursor.stringOrNull(artistCol), UNKNOWN_ARTIST)
            val album = MediaStorePathNormalizer.normalizeTag(cursor.stringOrNull(albumCol), UNKNOWN_ALBUM)
            val albumId = cursor.longOrZero(albumIdCol)
            val albumArtUri = if (albumId > 0L) {
                ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId).toString()
            } else {
                null
            }
            val rawPath = cursor.stringOrNull(pathCol)
            val folder = if (sdkInt >= Build.VERSION_CODES.Q) {
                MediaStorePathNormalizer.fromRelativePath(rawPath)
            } else {
                MediaStorePathNormalizer.fromLegacyDataPath(rawPath)
            }

            tracks += AudioTrack(
                id = id,
                contentUri = ContentUris.withAppendedId(collectionUri, id).toString(),
                title = title,
                artist = artist,
                album = album,
                albumId = albumId,
                albumArtUri = albumArtUri,
                durationMs = durationMs,
                sizeBytes = cursor.longOrZero(sizeCol),
                mimeType = cursor.stringOrNull(mimeCol).orEmpty().ifBlank { DEFAULT_AUDIO_MIME },
                folderPath = folder.path,
                folderName = folder.name,
                trackNumber = cursor.intOrZero(trackCol),
                year = cursor.intOrZero(yearCol),
                dateAddedSeconds = cursor.longOrZero(dateAddedCol),
                dateModifiedSeconds = cursor.longOrZero(dateModifiedCol),
            )
        }
        return tracks
    }

    private companion object {
        val ALBUM_ART_BASE_URI: Uri = "content://media/external/audio/albumart".toUri()
        const val UNKNOWN_TITLE = "Untitled"
        const val UNKNOWN_ARTIST = "Unknown Artist"
        const val UNKNOWN_ALBUM = "Unknown Album"
        const val DEFAULT_AUDIO_MIME = "audio/*"
    }
}

internal fun Cursor.stringOrNull(columnIndex: Int): String? =
    if (columnIndex >= 0 && !isNull(columnIndex)) getString(columnIndex) else null

internal fun Cursor.longOrZero(columnIndex: Int): Long =
    if (columnIndex >= 0 && !isNull(columnIndex)) getLong(columnIndex).coerceAtLeast(0L) else 0L

internal fun Cursor.intOrZero(columnIndex: Int): Int =
    if (columnIndex >= 0 && !isNull(columnIndex)) getInt(columnIndex).coerceAtLeast(0) else 0
