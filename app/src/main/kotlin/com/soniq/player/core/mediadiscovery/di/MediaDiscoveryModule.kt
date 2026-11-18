package com.soniq.player.core.mediadiscovery.di

import android.content.ContentResolver
import android.content.Context
import com.soniq.player.core.mediadiscovery.AndroidMediaStoreChangeObserver
import com.soniq.player.core.mediadiscovery.MediaStoreChangeObserver
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

    companion object {
        @Provides
        @Singleton
        fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
            context.contentResolver
    }
}
