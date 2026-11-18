package com.soniq.player

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point initializing Hilt's singleton dependency graph.
 *
 * No third-party analytics, crash-reporting network SDKs, or advertising SDKs are initialized.
 */
@HiltAndroidApp
class SoniqApplication : Application()
