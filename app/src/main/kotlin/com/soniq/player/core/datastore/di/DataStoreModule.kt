package com.soniq.player.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.soniq.player.core.datastore.AppearanceDataStore
import com.soniq.player.core.datastore.PlaybackDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /** File name is referenced by the backup rules in res/xml; keep them in sync. */
    private const val APPEARANCE_FILE = "appearance"

    /** Transient playback preferences & queue snapshot (excluded from cloud backup). */
    private const val PLAYBACK_FILE = "playback_state"

    @Provides
    @Singleton
    @AppearanceDataStore
    fun provideAppearanceDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            // A corrupted file resets appearance to defaults instead of crashing on every launch.
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile(APPEARANCE_FILE) },
        )

    @Provides
    @Singleton
    @PlaybackDataStore
    fun providePlaybackDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile(PLAYBACK_FILE) },
        )
}
