package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.ui.maps.route.line.api.RouteLineValueCommandHolder

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
    val baseExpressionCommandHolder: RouteLineValueCommandHolder,
    val casingExpressionCommandHolder: RouteLineValueCommandHolder,
    val trafficExpressionCommandHolder: RouteLineValueCommandHolder?,
    val restrictedSectionExpressionCommandHolder: RouteLineValueCommandHolder?,
    val trimOffset: RouteLineTrimOffset? = null,
    val trailExpressionCommandHolder: RouteLineValueCommandHolder? = null,
    val trailCasingExpressionCommandHolder: RouteLineValueCommandHolder? = null,
    val blurExpressionCommandHolder: RouteLineValueCommandHolder? = null,
)
