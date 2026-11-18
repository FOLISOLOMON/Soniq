package com.soniq.player.core.userlibrary.di

import com.soniq.player.core.userlibrary.data.DefaultUserLibraryRepository
import com.soniq.player.core.userlibrary.domain.UserLibraryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UserLibraryModule {

    @Binds
    abstract fun bindUserLibraryRepository(
        impl: DefaultUserLibraryRepository,
    ): UserLibraryRepository
}
