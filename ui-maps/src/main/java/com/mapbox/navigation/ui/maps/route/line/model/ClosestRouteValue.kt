package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.base.route.NavigationRoute

/**
 * The nearest route to a touch point on the [Map].
 *
 * @param navigationRoute the route found
 */
class ClosestRouteValue internal constructor(
    val navigationRoute: NavigationRoute,
)
