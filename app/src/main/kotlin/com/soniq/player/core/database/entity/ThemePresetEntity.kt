package com.soniq.player.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistence representation of a saved theme preset. Enum-backed fields are stored as their
 * constant names (TEXT) so that reordering enum constants can never corrupt stored data.
 */
@Entity(tableName = "theme_presets")
data class ThemePresetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "theme_mode")
    val themeMode: String,
    @ColumnInfo(name = "amoled_black")
    val amoledBlack: Boolean,
    @ColumnInfo(name = "dynamic_color")
    val dynamicColor: Boolean,
    @ColumnInfo(name = "accent_argb")
    val accentArgb: Int,
    @ColumnInfo(name = "font_family")
    val fontFamily: String,
    @ColumnInfo(name = "text_scale")
    val textScale: String,
    @ColumnInfo(name = "background_style")
    val backgroundStyle: String,
    @ColumnInfo(name = "background_image_uri")
    val backgroundImageUri: String?,
    @ColumnInfo(name = "player_layout")
    val playerLayout: String,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMillis: Long,
)
