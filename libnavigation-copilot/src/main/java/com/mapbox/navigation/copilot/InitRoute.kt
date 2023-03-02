package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.api.directions.v5.models.DirectionsRoute

@Keep
internal data class InitRoute(
    val requestIdentifier: String?,
    val route: DirectionsRoute,
) : EventDTO
