package com.soniq.player.core.common

import java.util.Locale

/** Pure-Kotlin media metadata formatters used by Music and Video library views. */
object MediaFormatters {

    /** Formats milliseconds as `m:ss` (under 1 hour) or `h:mm:ss` (1 hour and longer). */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs.coerceAtLeast(0L)) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /** Formats a byte count into human-readable B / KB / MB / GB text. */
    fun formatFileSize(sizeBytes: Long): String {
        val safe = sizeBytes.coerceAtLeast(0L)
        return when {
            safe >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", safe / 1_073_741_824.0)
            safe >= 1_048_576L -> String.format(Locale.US, "%.1f MB", safe / 1_048_576.0)
            safe >= 1_024L -> String.format(Locale.US, "%d KB", safe / 1_024L)
            else -> "$safe B"
        }
    }

    /** Returns a compact resolution label (e.g., `4K`, `1080p`, `720p`) or `null` when unknown. */
    fun formatResolution(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        val shortSide = minOf(width, height)
        val longSide = maxOf(width, height)
        return when {
            longSide >= 3840 || shortSide >= 2160 -> "4K"
            shortSide >= 1440 -> "1440p"
            shortSide >= 1080 -> "1080p"
            shortSide >= 720 -> "720p"
            shortSide >= 480 -> "480p"
            else -> "${width}×${height}"
        }
    }
}
