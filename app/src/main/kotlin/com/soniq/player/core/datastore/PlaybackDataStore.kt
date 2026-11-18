package com.soniq.player.core.datastore

import javax.inject.Qualifier

/** Qualifies the Preferences DataStore that holds playback preferences and resumption snapshots. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackDataStore
