package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Represents an error value for route line related updates.
 *
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class RouteLineError internal constructor(
    val errorMessage: String,
    val throwable: Throwable?
)
