package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.ui.maps.route.line.api.RouteLineExpressionCommandHolder

internal data class VanishingRouteLineExpressions(
    val trafficLineExpressionCommandHolder: RouteLineExpressionCommandHolder,
    val routeLineExpressionCommandHolder: RouteLineExpressionCommandHolder,
    val routeLineCasingExpressionCommandHolder: RouteLineExpressionCommandHolder,
    val restrictedRoadExpressionCommandHolder: RouteLineExpressionCommandHolder?,
    val trailExpressionCommandHolder: RouteLineExpressionCommandHolder? = null,
    val trailCasingExpressionCommandHolder: RouteLineExpressionCommandHolder? = null,
)
