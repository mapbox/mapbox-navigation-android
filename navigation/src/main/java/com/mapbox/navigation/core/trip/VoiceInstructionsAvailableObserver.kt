package com.mapbox.navigation.core.trip

import androidx.annotation.MainThread
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Observer that notifies about relevant voice instructions availability changes.
 *
 * Note: The observer will be called from the main thread when voice instruction availability
 * changes.
 *
 * @see [MapboxNavigation.registerRelevantVoiceInstructionsCallback]
 */
@MapboxExperimental
fun interface VoiceInstructionsAvailableObserver {
    @MainThread
    fun onChanged(available: Boolean)
}
