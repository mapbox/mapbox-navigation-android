package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.maps.extension.style.expressions.generated.Expression

internal data class VanishingRouteLineExpressions(
    val trafficLineExpression: Expression,
    val routeLineExpression: Expression,
    val routeLineCasingExpression: Expression
)
