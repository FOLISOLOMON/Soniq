package com.soniq.player.core.theme.di

import com.soniq.player.core.theme.data.DefaultAppearanceRepository
import com.soniq.player.core.theme.domain.AppearanceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
abstract class ThemeModule {

    @Binds
    abstract fun bindAppearanceRepository(impl: DefaultAppearanceRepository): AppearanceRepository

    companion object {
        /** Injected so that time-dependent logic (preset timestamps) is deterministic in tests. */
        @Provides
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
