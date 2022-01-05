package com.mapbox.navigation.ui.maneuver.model

/**
 * Data structure that holds information about any error fetching a road shield.
 * @property url String
 * @property message String
 */
@Deprecated(
    message = "The data class is incapable of delivering Mapbox designed route shields."
)
data class RoadShieldError internal constructor(
    val url: String,
    val message: String
)
