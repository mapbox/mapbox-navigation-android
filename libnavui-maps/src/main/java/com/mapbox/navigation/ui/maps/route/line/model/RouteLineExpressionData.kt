package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.ColorInt

/**
 * Contains data related to a route distance traveled and a traffic congestion color.
 *
 * @param offset a distance offset value
 * @param segmentColor a color for a segment of a route line
 * @param legIndex the route leg index for this data
 */
data class RouteLineExpressionData(
    val offset: Double,
    @ColorInt val segmentColor: Int,
    val legIndex: Int
)
