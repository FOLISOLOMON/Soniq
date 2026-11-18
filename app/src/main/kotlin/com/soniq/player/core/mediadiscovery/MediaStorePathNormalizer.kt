package com.soniq.player.core.mediadiscovery

/**
 * Normalizes MediaStore folder paths across API 29+ (`RELATIVE_PATH`, e.g. `Music/Jazz/`)
 * and API 26–28 (`DATA`, e.g. `/storage/emulated/0/Music/Jazz/track.flac`).
 */
object MediaStorePathNormalizer {

    data class FolderInfo(
        val path: String,
        val name: String,
    )

    const val ROOT_FOLDER_NAME = "Internal Storage"

    /** Extracts a clean folder path and leaf display name from `RELATIVE_PATH`. */
    fun fromRelativePath(relativePath: String?): FolderInfo {
        val cleaned = relativePath
            ?.replace('\\', '/')
            ?.trim()
            ?.trim('/')
            .orEmpty()
        if (cleaned.isEmpty()) {
            return FolderInfo(path = "", name = ROOT_FOLDER_NAME)
        }
        val leaf = cleaned.substringAfterLast('/')
        return FolderInfo(path = cleaned, name = leaf.ifBlank { ROOT_FOLDER_NAME })
    }

    /** Extracts a clean folder path and leaf display name from a legacy absolute file path. */
    fun fromLegacyDataPath(dataPath: String?): FolderInfo {
        val normalized = dataPath?.replace('\\', '/')?.trim().orEmpty()
        if (normalized.isEmpty()) {
            return FolderInfo(path = "", name = ROOT_FOLDER_NAME)
        }
        val parent = normalized.substringBeforeLast('/', missingDelimiterValue = "").trimEnd('/')
        if (parent.isEmpty()) {
            return FolderInfo(path = "", name = ROOT_FOLDER_NAME)
        }
        val leaf = parent.substringAfterLast('/')
        return FolderInfo(path = parent, name = leaf.ifBlank { ROOT_FOLDER_NAME })
    }

    /** Strips the filename extension for fallback display titles. */
    fun stripExtension(displayName: String?): String {
        val trimmed = displayName?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""
        val lastDot = trimmed.lastIndexOf('.')
        return if (lastDot > 0) trimmed.substring(0, lastDot) else trimmed
    }

    /** Replaces blank strings or Android's `<unknown>` MediaStore sentinel with [fallback]. */
    fun normalizeTag(value: String?, fallback: String): String {
        val trimmed = value?.trim().orEmpty()
        return if (trimmed.isEmpty() || trimmed.equals("<unknown>", ignoreCase = true)) {
            fallback
        } else {
            trimmed
        }
    }
}
