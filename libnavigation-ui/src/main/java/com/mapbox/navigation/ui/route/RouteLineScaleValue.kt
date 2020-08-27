package com.mapbox.navigation.ui.route

/**
 * Represents the values used for the [Expression.Stop] elements in an [Expression] for
 * controlling the scaling of the route line at different zoom levels. See the Mapbox Map
 * documentation for more information regarding Expressions.
 *
 * @param scaleStop the stop value for the [Expression.Stop]
 * @param scaleMultiplier a value multiplied by the scale
 * @param scale represents the scale value used in the [Expression]
 */
internal data class RouteLineScaleValue(
    val scaleStop: Float,
    val scaleMultiplier: Float,
    val scale: Float
)
