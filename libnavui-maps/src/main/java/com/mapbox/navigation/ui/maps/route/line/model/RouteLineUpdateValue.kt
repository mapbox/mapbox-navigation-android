package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.maps.extension.style.expressions.generated.Expression

/**
 * Represents data for updating the appearance of the route line.
 *
 * @param trafficLineExpression the expression for the primary route traffic line
 * @param routeLineExpression the expression for the primary route line
 * @param casingLineExpression the expression for the primary route casing line
 */
class RouteLineUpdateValue internal constructor(
    val trafficLineExpression: Expression,
    val routeLineExpression: Expression,
    val casingLineExpression: Expression
)
