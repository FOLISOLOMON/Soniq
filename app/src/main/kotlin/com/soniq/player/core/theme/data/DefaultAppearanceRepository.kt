package com.soniq.player.core.theme.data

import com.soniq.player.core.database.dao.ThemePresetDao
import com.soniq.player.core.datastore.AppearancePreferencesDataSource
import com.soniq.player.core.model.appearance.AppearanceConfig
import com.soniq.player.core.model.appearance.ThemePreset
import com.soniq.player.core.theme.domain.AppearanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Combines the DataStore-backed *current* appearance with Room-backed *saved presets*.
 * Both underlying APIs are main-safe, so no dispatcher switching is required here.
 */
@Singleton
class DefaultAppearanceRepository @Inject constructor(
    private val preferences: AppearancePreferencesDataSource,
    private val presetDao: ThemePresetDao,
    private val clock: Clock,
) : AppearanceRepository {

    override val appearance: Flow<AppearanceConfig> = preferences.appearance

    override val presets: Flow<List<ThemePreset>> =
        presetDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun updateAppearance(transform: (AppearanceConfig) -> AppearanceConfig) {
        preferences.update(transform)
    }

    override suspend fun saveCurrentAsPreset(name: String): Long {
        val trimmed = validatePresetName(name)
        val current = preferences.appearance.first()
        return presetDao.insert(current.toPresetEntity(trimmed, clock.millis()))
    }

    override suspend fun applyPreset(id: Long): Boolean {
        val preset = presetDao.getById(id) ?: return false
        preferences.replace(preset.toDomain().config)
        return true
    }

    override suspend fun renamePreset(id: Long, newName: String): Boolean {
        val trimmed = validatePresetName(newName)
        return presetDao.renameById(id, trimmed) > 0
    }

    override suspend fun duplicatePreset(id: Long): Long? {
        val existing = presetDao.getById(id) ?: return null
        val suffix = " Copy"
        val baseMax = (MAX_PRESET_NAME_LENGTH - suffix.length).coerceAtLeast(1)
        val copyName = existing.name.take(baseMax).trimEnd() + suffix
        return presetDao.insert(existing.toDomain().config.toPresetEntity(copyName, clock.millis()))
    }

    override suspend fun deletePreset(id: Long) {
        presetDao.deleteById(id)
    }

    private fun validatePresetName(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Preset name must not be blank" }
        require(trimmed.length <= MAX_PRESET_NAME_LENGTH) {
            "Preset name must be at most $MAX_PRESET_NAME_LENGTH characters"
        }
        return trimmed
    }

    companion object {
        const val MAX_PRESET_NAME_LENGTH = 40
    }
}
