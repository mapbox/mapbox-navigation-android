package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Indicates no route was found when searching the map for a route.
 *
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class RouteNotFound internal constructor(
    val errorMessage: String,
    val throwable: Throwable?,
)
