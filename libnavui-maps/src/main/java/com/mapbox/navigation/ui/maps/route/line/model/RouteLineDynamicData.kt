package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.ui.maps.route.line.api.RouteLineExpressionCommandHolder

/**
 * Provides information needed to draw a route.
 *
 * @param baseExpressionCommandHolder expression used to style the base of the line
 * @param casingExpressionCommandHolder expression used to style the case of the line
 * @param trafficExpressionCommandHolder expression used to style the congestion colors on the line
 * @param restrictedSectionExpressionCommandHolder expression used to style the restricted sections on the line
 * @param trimOffset a value representing the section of the line that should be trimmed and made transparent. Null by default
 * @param trailExpressionCommandHolder expression used to style the trail layer
 * @param trailCasingExpressionCommandHolder expression used to style the trail casing layer
 * @param blurExpressionCommandHolder expression used to style the blur layer
 */
internal data class RouteLineDynamicData(
    val baseExpressionCommandHolder: RouteLineExpressionCommandHolder,
    val casingExpressionCommandHolder: RouteLineExpressionCommandHolder,
    val trafficExpressionCommandHolder: RouteLineExpressionCommandHolder?,
    val restrictedSectionExpressionCommandHolder: RouteLineExpressionCommandHolder?,
    val trimOffset: RouteLineTrimOffset? = null,
    val trailExpressionCommandHolder: RouteLineExpressionCommandHolder? = null,
    val trailCasingExpressionCommandHolder: RouteLineExpressionCommandHolder? = null,
    val blurExpressionCommandHolder: RouteLineExpressionCommandHolder? = null,
)
