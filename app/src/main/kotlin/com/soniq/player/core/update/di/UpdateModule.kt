package com.soniq.player.core.update.di

import com.soniq.player.BuildConfig
import com.soniq.player.core.update.AndroidApkInstaller
import com.soniq.player.core.update.AndroidApkVerifier
import com.soniq.player.core.update.ApkInstaller
import com.soniq.player.core.update.ApkVerifier
import com.soniq.player.core.update.DefaultUpdateRepository
import com.soniq.player.core.update.HttpsUpdateRemoteDataSource
import com.soniq.player.core.update.UpdateConfig
import com.soniq.player.core.update.UpdateRemoteDataSource
import com.soniq.player.core.update.UpdateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {

    @Binds
    @Singleton
    abstract fun bindUpdateRemoteDataSource(impl: HttpsUpdateRemoteDataSource): UpdateRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindApkVerifier(impl: AndroidApkVerifier): ApkVerifier

    @Binds
    @Singleton
    abstract fun bindApkInstaller(impl: AndroidApkInstaller): ApkInstaller

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: DefaultUpdateRepository): UpdateRepository

    companion object {
        @Provides
        @Singleton
        fun provideUpdateConfig(): UpdateConfig = UpdateConfig(
            manifestUrl = BuildConfig.UPDATE_MANIFEST_URL,
            installedVersionCode = BuildConfig.VERSION_CODE,
            installedVersionName = BuildConfig.VERSION_NAME,
        )
    }
}
