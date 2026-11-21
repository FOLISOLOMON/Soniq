package com.soniq.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-scoped gate ensuring SONIQ's branded launch entrance plays once per cold process
 * launch and never replays across activity recreation, configuration changes, background/resume,
 * or in-app navigation.
 */
@Singleton
class LaunchEntranceCoordinator @Inject constructor() {

    private val _shouldShowEntrance = MutableStateFlow(true)
    val shouldShowEntrance: StateFlow<Boolean> = _shouldShowEntrance.asStateFlow()

    fun markCompleted() {
        _shouldShowEntrance.value = false
    }
}
