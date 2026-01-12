package com.mapbox.navigation.core.trip

import androidx.annotation.MainThread
import com.mapbox.annotation.MapboxExperimental
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Observer that receives the relevant voice instructions available.
 *
 * This observer is designed for one-time fetching of voice instructions. After the callback
 * is invoked, the observer is **automatically unregistered** by the SDK. You do not need to
 * manually unregister it.
 *
 * Note: The observer will be called from the main thread when the voice instruction is retrieved.
 *
 * To fetch voice instructions again, simply register the observer again using
 * [MapboxNavigation.registerRelevantVoiceInstructionsCallback].
 *
 * @see MapboxNavigation.registerRelevantVoiceInstructionsCallback
 */
@MapboxExperimental
fun interface RelevantVoiceInstructionsCallback {
    /**
     * Called with the relevant voice instructions available.
     *
     * **Note:** This callback will only be triggered once. The observer is automatically
     * unregistered after this callback completes. To receive voice instructions again,
     * register the observer again.
     *
     * @param voiceInstructions list of latest voice instructions available, empty if none available
     */
    @MainThread
    fun onRelevantVoiceInstructionsReceived(voiceInstructions: List<VoiceInstructions>)
}
