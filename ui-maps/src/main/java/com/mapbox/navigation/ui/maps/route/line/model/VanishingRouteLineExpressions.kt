package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.ui.maps.route.line.api.RouteLineValueCommandHolder

internal data class VanishingRouteLineExpressions(
    val trafficLineExpressionCommandHolder: RouteLineValueCommandHolder,
    val routeLineValueCommandHolder: RouteLineValueCommandHolder,
    val routeLineCasingExpressionCommandHolder: RouteLineValueCommandHolder,
    val restrictedRoadExpressionCommandHolder: RouteLineValueCommandHolder?,
    val trailExpressionCommandHolder: RouteLineValueCommandHolder? = null,
    val trailCasingExpressionCommandHolder: RouteLineValueCommandHolder? = null,
)
