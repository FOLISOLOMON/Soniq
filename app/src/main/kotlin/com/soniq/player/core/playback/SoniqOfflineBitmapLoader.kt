package com.soniq.player.core.playback

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.soniq.player.core.common.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strictly offline [BitmapLoader] for Media3 notifications and lock-screen artwork.
 *
 * Only local schemes (`content://`, `file://`, `android.resource://`) and embedded byte arrays
 * are accepted; remote URLs are rejected immediately so Media3 can never attempt network I/O.
 * Large album covers are downsampled to [MAX_ARTWORK_DIMENSION_PX] to avoid notification OOMs.
 */
@OptIn(UnstableApi::class)
@Singleton
class SoniqOfflineBitmapLoader @Inject constructor(
    private val contentResolver: ContentResolver,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BitmapLoader {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override fun supportsMimeType(mimeType: String): Boolean =
        mimeType.startsWith("image/", ignoreCase = true)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                val bitmap = decodeSampledBytes(data)
                    ?: throw IOException("Could not decode embedded artwork bitmap")
                future.set(bitmap)
            } catch (error: Throwable) {
                future.setException(error)
            }
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        if (!isAllowedLocalScheme(uri.scheme)) {
            future.setException(
                SecurityException("SONIQ is offline-first; non-local artwork URI rejected: ${uri.scheme}"),
            )
            return future
        }
        scope.launch {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("Unable to open local artwork URI: $uri")
                val bitmap = decodeSampledBytes(bytes)
                    ?: throw IOException("Unable to decode local artwork URI: $uri")
                future.set(bitmap)
            } catch (error: Throwable) {
                future.setException(error)
            }
        }
        return future
    }

    internal fun isAllowedLocalScheme(scheme: String?): Boolean =
        scheme != null && scheme.lowercase() in ALLOWED_SCHEMES

    private fun decodeSampledBytes(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                maxDimension = MAX_ARTWORK_DIMENSION_PX,
            )
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    internal companion object {
        const val MAX_ARTWORK_DIMENSION_PX = 512
        private val ALLOWED_SCHEMES = setOf(
            ContentResolver.SCHEME_CONTENT,
            ContentResolver.SCHEME_FILE,
            ContentResolver.SCHEME_ANDROID_RESOURCE,
        )

        fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
            var sampleSize = 1
            var longest = maxOf(width, height)
            while (longest / 2 >= maxDimension) {
                sampleSize *= 2
                longest /= 2
            }
            return sampleSize
        }
    }
}
