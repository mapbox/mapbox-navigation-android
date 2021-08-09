package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.ExpressionBuilder

/**
 * Represents the values used for the [ExpressionBuilder.stop] elements in an [Expression] for
 * controlling the scaling of the route line at different zoom levels. See the Mapbox Map
 * documentation for more information regarding Expressions.
 *
 * @param scaleStop the stop value for the [ExpressionBuilder.stop]
 * @param scaleMultiplier a value multiplied by the scale
 * @param scale represents the scale value used in the [Expression]
 */
data class RouteLineScaleValue(
    val scaleStop: Float,
    val scaleMultiplier: Float,
    val scale: Float
)
