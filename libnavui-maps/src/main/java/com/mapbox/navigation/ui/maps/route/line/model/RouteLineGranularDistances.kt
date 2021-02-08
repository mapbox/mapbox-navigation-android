package com.mapbox.navigation.ui.maps.route.line.model

import android.util.SparseArray

/**
 * @param distance full distance of the route
 * @param distancesArray array where index is the index of the upcoming not yet visited point on the route
 */
data class RouteLineGranularDistances(
    val distance: Double,
    val distancesArray: SparseArray<RouteLineDistancesIndex>
)
