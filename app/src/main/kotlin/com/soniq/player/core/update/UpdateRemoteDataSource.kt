package com.soniq.player.core.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.soniq.player.core.common.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

interface UpdateRemoteDataSource {
    suspend fun fetchManifest(manifestUrl: String): Result<UpdateManifestDto>
    suspend fun downloadApk(
        apkUrl: String,
        expectedSizeBytes: Long,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<File>
}

@Singleton
class HttpsUpdateRemoteDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UpdateRemoteDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    override suspend fun fetchManifest(manifestUrl: String): Result<UpdateManifestDto> =
        withContext(ioDispatcher) {
            runCatching {
                requireHttpsUrl(manifestUrl, "manifest")
                requireOnline()

                val connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                try {
                    val code = connection.responseCode
                    if (code !in 200..299) {
                        error("Update server returned HTTP $code.")
                    }
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    if (body.isBlank()) {
                        error("Update manifest response was empty.")
                    }
                    json.decodeFromString<UpdateManifestDto>(body)
                } finally {
                    connection.disconnect()
                }
            }
        }

    override suspend fun downloadApk(
        apkUrl: String,
        expectedSizeBytes: Long,
        destinationFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<File> = withContext(ioDispatcher) {
        val parentDir = destinationFile.parentFile ?: error("Invalid download destination.")
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        val tempFile = File(parentDir, "${destinationFile.name}.part")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            requireHttpsUrl(apkUrl, "APK")
            requireOnline()

            val requiredBytes = if (expectedSizeBytes > 0L) {
                expectedSizeBytes + STORAGE_HEADROOM_BYTES
            } else {
                DEFAULT_MIN_STORAGE_BYTES
            }
            if (parentDir.usableSpace < requiredBytes) {
                error("Insufficient storage space to download update.")
            }

            val connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                instanceFollowRedirects = true
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    error("APK download failed with HTTP $code.")
                }
                val finalUrl = connection.url?.toString().orEmpty()
                if (!finalUrl.startsWith("https://", ignoreCase = true)) {
                    error("Redirected APK URL is not HTTPS.")
                }

                val reportedLength = connection.contentLengthLong
                val totalBytes = if (reportedLength > 0L) reportedLength else expectedSizeBytes

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var totalRead = 0L
                        var lastReportedBytes = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            totalRead += read
                            if (totalRead - lastReportedBytes >= PROGRESS_STEP_BYTES || totalRead == totalBytes) {
                                lastReportedBytes = totalRead
                                onProgress(totalRead, totalBytes)
                            }
                        }
                        if (totalRead != lastReportedBytes) {
                            onProgress(totalRead, totalBytes)
                        }
                        output.flush()
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            if (!tempFile.renameTo(destinationFile)) {
                error("Failed to finalize downloaded update file.")
            }
            Result.success(destinationFile)
        } catch (t: Throwable) {
            tempFile.delete()
            if (t is kotlinx.coroutines.CancellationException) {
                throw t
            }
            Result.failure(t)
        }
    }

    private fun requireHttpsUrl(url: String, label: String) {
        val trimmed = url.trim()
        require(trimmed.startsWith("https://", ignoreCase = true)) {
            "Invalid $label URL: only HTTPS endpoints are allowed."
        }
    }

    private fun requireOnline() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val active = cm?.activeNetwork
        val caps = if (active != null) cm.getNetworkCapabilities(active) else null
        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!connected) {
            error("No internet connection available.")
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val DOWNLOAD_READ_TIMEOUT_MS = 30_000
        private const val BUFFER_SIZE = 8192
        private const val PROGRESS_STEP_BYTES = 64L * 1024L
        private const val STORAGE_HEADROOM_BYTES = 10L * 1024L * 1024L
        private const val DEFAULT_MIN_STORAGE_BYTES = 32L * 1024L * 1024L
    }
}
