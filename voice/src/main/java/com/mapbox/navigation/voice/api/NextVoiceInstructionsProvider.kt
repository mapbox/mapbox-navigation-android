package com.mapbox.navigation.voice.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions

internal data class RouteProgressData(
    val route: DirectionsRoute,
    val legIndex: Int,
    val stepIndex: Int,
    val stepDurationRemaining: Double,
    val stepDistanceRemaining: Double,
)

internal interface NextVoiceInstructionsProvider {

    fun getNextVoiceInstructions(progress: RouteProgressData): List<VoiceInstructions>
}
