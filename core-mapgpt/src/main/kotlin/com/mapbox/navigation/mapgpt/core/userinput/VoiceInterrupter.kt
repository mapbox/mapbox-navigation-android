package com.mapbox.navigation.mapgpt.core.userinput

import kotlinx.coroutines.flow.StateFlow

/**
 * This may be deleted in favor of supporting client side capabilities.
 * TODO https://mapbox.atlassian.net/browse/NAVAND-3305
 */
interface VoiceInterrupter {
    val isAvailable: StateFlow<Boolean>
}
