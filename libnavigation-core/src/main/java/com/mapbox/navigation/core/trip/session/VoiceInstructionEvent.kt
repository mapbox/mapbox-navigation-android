package com.mapbox.navigation.core.trip.session

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.extensions.ifNonNull

class VoiceInstructionEvent {

    var voiceInstructions: VoiceInstructions = VoiceInstructions.builder().build()
        private set

    fun isOccurring(routeProgress: RouteProgress): Boolean =
        updateCurrentAnnouncement(routeProgress)

    private fun updateCurrentAnnouncement(routeProgress: RouteProgress): Boolean =
        ifNonNull(routeProgress.voiceInstructions()) {
            voiceInstructions = it
            true
        } ?: false
}
