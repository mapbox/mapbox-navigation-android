package com.mapbox.navigation.base.route

import com.mapbox.navigator.RouterOrigin

/**
 * Map to Nav SDK [RouteVariants.RouterOrigin]
 */
@RouteVariants.RouterOrigin
fun RouterOrigin.mapToRouterOrigin(): String =
    when (this) {
        RouterOrigin.ONLINE -> RouteVariants.OFF_BOARD
        RouterOrigin.ONBOARD -> RouteVariants.ON_BOARD
    }
