package com.soniq.player.core.theme.domain

import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.ThemePreset
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for the user's appearance settings and saved theme presets.
 *
 * Write operations are main-safe suspending functions. They may throw [java.io.IOException]
 * (storage failures) or [IllegalArgumentException] (invalid input); callers are expected to
 * surface failures to the user.
 */
interface AppearanceRepository {

    /** The current appearance. Emits defaults when nothing has been stored yet. */
    val appearance: Flow<AppearanceConfig>

    /** Saved presets, newest first. */
    val presets: Flow<List<ThemePreset>>

    /** Atomically reads, transforms and stores the current appearance. */
    suspend fun updateAppearance(transform: (AppearanceConfig) -> AppearanceConfig)

    /**
     * Saves the current appearance as a preset named [name] (trimmed, must not be blank).
     * @return the id of the new preset.
     */
    suspend fun saveCurrentAsPreset(name: String): Long

    /**
     * Replaces the current appearance with the preset identified by [id].
     * @return `false` if no such preset exists.
     */
    suspend fun applyPreset(id: Long): Boolean

    /** Renames an existing preset; returns `false` if not found. */
    suspend fun renamePreset(id: Long, newName: String): Boolean

    /** Duplicates an existing preset; returns the new id or `null` if not found. */
    suspend fun duplicatePreset(id: Long): Long?

    /** Installs missing curated starter presets without overwriting user presets; returns count added. */
    suspend fun installStarterPresets(): Int

    /** Deletes the preset identified by [id]; deleting a missing preset is a no-op. */
    suspend fun deletePreset(id: Long)
}
