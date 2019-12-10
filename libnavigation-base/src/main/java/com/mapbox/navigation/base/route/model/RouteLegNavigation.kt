package com.mapbox.navigation.base.route.model

/**
 *
 * @property distance The distance traveled from one waypoint to another. Unit is meters
 * @since 1.0
 *
 * @property duration The estimated travel time from one waypoint to another. Unit is seconds
 * @since 1.0
 *
 * @property summary A short human-readable summary of major roads traversed. Useful to distinguish alternatives.
 * @since 1.0
 *
 * @property steps Gives a List including all the steps to get from one waypoint to another.
 * @since 1.0
 *
 * @property annotation Contains additional details about each line segment along the
 * route geometry. If you'd like to receiving this, you must request it inside your Directions
 * request before executing the call.
 * @since 1.0
 */
class RouteLegNavigation(
    val distance: Double?,
    val duration: Double?,
    val summary: String?,
    val steps: List<LegStepNavigation>?,
    val annotation: LegAnnotationNavigation?
)
