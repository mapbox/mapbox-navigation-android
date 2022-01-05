package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldFactory

private const val SVG_EXTENSION = ".svg"

/**
 * Extension function to convert [RoadShield] to [RouteShield]
 */
@OptIn(ExperimentalMapboxNavigationAPI::class)
fun RoadShield.toRouteShield(): RouteShield {
    return RouteShieldFactory.buildRouteShield(
        downloadUrl = this.shieldUrl,
        byteArray = this.shieldIcon,
        initialUrl = this.shieldUrl.dropLast(SVG_EXTENSION.length)
    )
}
