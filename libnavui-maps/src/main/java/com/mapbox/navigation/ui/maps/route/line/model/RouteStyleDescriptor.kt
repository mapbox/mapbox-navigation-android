package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.ColorInt

/**
 * This class is used for describing the route line color(s) at runtime.
 *
 * @param routeIdentifier a string that identifies routes which should have their color overridden
 * @param lineColor the color of the route line
 * @param lineCasingColor the color of the shield line which appears below the route line
 * and is normally wider providing a visual border for the route line.
 */
// todo make a builder?
data class RouteStyleDescriptor(
    val routeIdentifier: String,
    @ColorInt val lineColor: Int,
    @ColorInt val lineCasingColor: Int
)
