package com.soniq.player.core.mediadiscovery.di

import android.content.ContentResolver
import android.content.Context
import com.soniq.player.core.mediadiscovery.AndroidMediaStoreChangeObserver
import com.soniq.player.core.mediadiscovery.LocalAudioScanner
import com.soniq.player.core.mediadiscovery.LocalVideoScanner
import com.soniq.player.core.mediadiscovery.MediaStoreAudioScanner
import com.soniq.player.core.mediadiscovery.MediaStoreChangeObserver
import com.soniq.player.core.mediadiscovery.MediaStoreVideoScanner
import com.soniq.player.core.mediadiscovery.data.DefaultMusicLibraryRepository
import com.soniq.player.core.mediadiscovery.data.DefaultVideoLibraryRepository
import com.soniq.player.core.mediadiscovery.domain.MusicLibraryRepository
import com.soniq.player.core.mediadiscovery.domain.VideoLibraryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaDiscoveryModule {

    @Binds
    abstract fun bindMediaStoreChangeObserver(
        impl: AndroidMediaStoreChangeObserver,
    ): MediaStoreChangeObserver

    @Binds
    abstract fun bindLocalAudioScanner(
        impl: MediaStoreAudioScanner,
    ): LocalAudioScanner

    @Binds
    abstract fun bindLocalVideoScanner(
        impl: MediaStoreVideoScanner,
    ): LocalVideoScanner

    @Binds
    abstract fun bindMusicLibraryRepository(
        impl: DefaultMusicLibraryRepository,
    ): MusicLibraryRepository

    @Binds
    abstract fun bindVideoLibraryRepository(
        impl: DefaultVideoLibraryRepository,
    ): VideoLibraryRepository

    companion object {
        @Provides
        @Singleton
        fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
            context.contentResolver
    }
}
