package com.soniq.player.core.database.di

import android.content.Context
import androidx.room.Room
import com.soniq.player.core.database.SoniqDatabase
import com.soniq.player.core.database.dao.ThemePresetDao
import com.soniq.player.core.database.media.SoniqMediaDatabase
import com.soniq.player.core.database.media.dao.AudioTrackDao
import com.soniq.player.core.database.media.dao.VideoItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SoniqDatabase =
        Room.databaseBuilder(context, SoniqDatabase::class.java, SoniqDatabase.NAME)
            // No destructive fallback: user data (presets, later playlists) must survive upgrades.
            // Every schema change ships with an explicit, tested Migration.
            .build()

    @Provides
    fun provideThemePresetDao(database: SoniqDatabase): ThemePresetDao = database.themePresetDao()

    @Provides
    @Singleton
    fun provideMediaDatabase(@ApplicationContext context: Context): SoniqMediaDatabase =
        Room.databaseBuilder(context, SoniqMediaDatabase::class.java, SoniqMediaDatabase.NAME)
            // Pure rebuildable MediaStore cache (excluded from backup): safe to rebuild on schema bump.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideAudioTrackDao(database: SoniqMediaDatabase): AudioTrackDao = database.audioTrackDao()

    @Provides
    fun provideVideoItemDao(database: SoniqMediaDatabase): VideoItemDao = database.videoItemDao()
}
