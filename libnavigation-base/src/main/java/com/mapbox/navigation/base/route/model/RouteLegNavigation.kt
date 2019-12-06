package com.mapbox.navigation.base.route.model

/**
 *
 * @param distance The distance traveled from one waypoint to another. Unit is meters
 * @since 1.0
 *
 * @param duration The estimated travel time from one waypoint to another. Unit is seconds
 * @since 1.0
 *
 * @param summary A short human-readable summary of major roads traversed. Useful to distinguish alternatives.
 * @since 1.0
 *
 * @param steps Gives a List including all the steps to get from one waypoint to another.
 * @since 1.0
 *
 * @param annotation Contains additional details about each line segment along the
 * route geometry. If you'd like to receiving this, you must request it inside your Directions
 * request before executing the call.
 * @since 1.0
 */
class RouteLegNavigation(
    val distance: Double?,
    val duration: Double?,
    val summary: String?
    // val steps: List<LegStep?>?,
    // val annotation: LegAnnotation?
)