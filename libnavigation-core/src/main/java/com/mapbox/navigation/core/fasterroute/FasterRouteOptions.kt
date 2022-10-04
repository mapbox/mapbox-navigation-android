package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

@ExperimentalMapboxNavigationAPI
data class FasterRouteOptions(
    val maxSimilarityToExistingRoute: Double
)
