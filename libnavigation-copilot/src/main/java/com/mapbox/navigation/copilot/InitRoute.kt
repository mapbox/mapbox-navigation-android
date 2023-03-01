package com.mapbox.navigation.copilot

import androidx.annotation.Keep

@Keep
internal data class InitRoute(
    val requestIdentifier: String?,
    val directionRouteJson: String,
) : EventDTO
