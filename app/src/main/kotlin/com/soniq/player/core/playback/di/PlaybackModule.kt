package com.soniq.player.core.playback.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.soniq.player.core.playback.AndroidAudioEffectsBackend
import com.soniq.player.core.playback.AudioEffectsBackend
import com.soniq.player.core.playback.Media3PlaybackController
import com.soniq.player.core.playback.PlaybackController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(ServiceComponent::class)
object PlaybackServiceModule {

    @Provides
    @ServiceScoped
    fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    @Provides
    @ServiceScoped
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
    ): ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
        .setHandleAudioBecomingNoisy(true)
        // Offline-first: WAKE_MODE_LOCAL keeps CPU awake without requesting network wake locks.
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackControllerModule {

    @Binds
    abstract fun bindPlaybackController(impl: Media3PlaybackController): PlaybackController

    @Binds
    abstract fun bindAudioEffectsBackend(impl: AndroidAudioEffectsBackend): AudioEffectsBackend
}
