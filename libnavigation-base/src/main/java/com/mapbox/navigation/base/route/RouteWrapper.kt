package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Routes wrapper
 *
 * @param routes is a list of [DirectionsRoute]
 * @param origin indicate where routes were come from
 *
 * @see [RouteVariants.RouterOrigin]
 */
class RouteWrapper internal constructor(
    val routes: List<DirectionsRoute>,
    @RouteVariants.RouterOrigin val origin: String
)
