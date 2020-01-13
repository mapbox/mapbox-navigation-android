package com.mapbox.navigation.base.route.model

/**
 *
 * @property distance The distance, in meters, between each pair of coordinates.
 * @since 1.0
 *
 * @property duration The speed, in meters per second, between each pair of coordinates.
 * @since 1.0
 *
 * @property speed The speed, in meters per second, between each pair of coordinates.
 * @since 1.0
 *
 * @property maxspeed The posted speed limit, between each pair of coordinates.
 * Maxspeed is only available for the `mapbox/driving` and `mapbox/driving-traffic`
 * profiles, other profiles will return `unknown`s only.
 * @since 1.0
 *
 * @property congestion The congestion between each pair of coordinates.
 * @since 1.0
 */
data class LegAnnotationNavigation(
    val distance: List<Double>?,
    val duration: List<Double>?,
    val speed: List<Double>?,
    val maxspeed: List<MaxSpeedNavigation>?,
    val congestion: List<String>?
)
