package com.mapbox.navigation.ui.voice.api

import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.core.internal.LegacyMapboxNavigationInstanceHolder
import com.mapbox.navigation.core.internal.MapboxNavigationCreateObserver

private data class TriggerAndObserver(
    val trigger: VoiceInstructionsDownloadTrigger,
    val observer: MapboxNavigationCreateObserver,
)

internal object VoiceInstructionsPredownloadHub {

    private const val DEFAULT_OBSERVABLE_TIME_SECONDS = 3 * 60 // 3 minutes
    private const val DEFAULT_TIME_PERCENTAGE_TO_TRIGGER_AFTER = 0.5

    private val registrants = hashMapOf<MapboxSpeechLoader, TriggerAndObserver>()

    fun register(loader: MapboxSpeechLoader) {
        if (loader !in registrants) {
            val trigger = VoiceInstructionsDownloadTrigger(
                DEFAULT_OBSERVABLE_TIME_SECONDS,
                DEFAULT_TIME_PERCENTAGE_TO_TRIGGER_AFTER
            )
            val observer = MapboxNavigationCreateObserver { mapboxNavigation ->
                mapboxNavigation.registerRoutesObserver(trigger)
                mapboxNavigation.registerRouteProgressObserver(trigger)
            }
            trigger.registerObserver(loader)
            LegacyMapboxNavigationInstanceHolder.registerCreateObserver(observer)
            registrants[loader] = TriggerAndObserver(trigger, observer)
        }
    }

    fun unregister(loader: MapboxSpeechLoader) {
        registrants.remove(loader)?.let { (trigger, observer) ->
            trigger.unregisterObserver(loader)
            LegacyMapboxNavigationInstanceHolder.peek()?.let {
                it.unregisterRoutesObserver(trigger)
                it.unregisterRouteProgressObserver(trigger)
            }
            LegacyMapboxNavigationInstanceHolder.unregisterCreateObserver(observer)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun unregisterAll() {
        registrants.clear()
    }
}
