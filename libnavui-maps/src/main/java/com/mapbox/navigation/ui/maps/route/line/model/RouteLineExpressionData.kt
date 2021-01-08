package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.ColorInt

/**
 * Contains data related to a route distance traveled and a traffic congestion color.
 *
 * @param offset
 * @param segmentColor
 */
data class RouteLineExpressionData(val offset: Double, @ColorInt val segmentColor: Int)
