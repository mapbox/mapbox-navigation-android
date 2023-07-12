package com.mapbox.navigation.ui.maps.route.line.model

internal data class VanishingRouteLineExpressions(
    val trafficLineExpression: RouteLineExpressionProvider,
    val routeLineExpression: RouteLineExpressionProvider,
    val routeLineCasingExpression: RouteLineExpressionProvider,
    val restrictedRoadExpression: RouteLineExpressionProvider?,
    val trailExpression: RouteLineExpressionProvider? = null,
    val trailCasingExpression: RouteLineExpressionProvider? = null,
)
