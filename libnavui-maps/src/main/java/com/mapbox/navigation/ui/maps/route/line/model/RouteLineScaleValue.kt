package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.ExpressionBuilder
import com.mapbox.navigation.base.internal.utils.safeCompareTo

/**
 * Represents the values used for the [ExpressionBuilder.stop] elements in an [Expression] for
 * controlling the scaling of the route line at different zoom levels. See the Mapbox Map
 * documentation for more information regarding Expressions.
 *
 * @param scaleStop the stop value for the [ExpressionBuilder.stop]
 * @param scaleMultiplier a value multiplied by the scale
 * @param scale represents the scale value used in the [Expression]
 */
class RouteLineScaleValue(
    val scaleStop: Float,
    val scaleMultiplier: Float,
    val scale: Float,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineScaleValue

        if (!scaleStop.safeCompareTo(other.scaleStop)) return false
        if (!scaleMultiplier.safeCompareTo(other.scaleMultiplier)) return false
        return scale.safeCompareTo(other.scale)
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = scaleStop.hashCode()
        result = 31 * result + scaleMultiplier.hashCode()
        result = 31 * result + scale.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineScaleValue(" +
            "scaleStop=$scaleStop, " +
            "scaleMultiplier=$scaleMultiplier, " +
            "scale=$scale" +
            ")"
    }
}
