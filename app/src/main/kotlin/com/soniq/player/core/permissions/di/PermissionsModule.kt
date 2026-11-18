package com.soniq.player.core.permissions.di

import com.soniq.player.core.permissions.AndroidPermissionChecker
import com.soniq.player.core.permissions.PermissionChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionsModule {

    @Binds
    abstract fun bindPermissionChecker(impl: AndroidPermissionChecker): PermissionChecker
}
