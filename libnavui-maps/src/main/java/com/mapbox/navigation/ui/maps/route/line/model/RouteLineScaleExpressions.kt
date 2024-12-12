package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.Keep
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils

/**
 * A class containing information about custom scaling expressions.
 *
 * @param routeLineScaleExpression an expression governing the behavior of route line scaling
 * @param routeCasingLineScaleExpression an expression governing the behavior of route casing line
 * scaling
 * @param routeTrafficLineScaleExpression an expression governing the behavior of route traffic line
 * scaling
 * @param alternativeRouteLineScaleExpression an expression governing the behavior of
 * alternative route line scaling
 * @param alternativeRouteCasingLineScaleExpression an expression governing the behavior of
 * alternative route casing line scaling
 * @param alternativeRouteTrafficLineScaleExpression an expression governing the behavior of
 * alternative route traffic line scaling */
@Keep
class RouteLineScaleExpressions private constructor(
    val routeLineScaleExpression: Expression,
    val routeCasingLineScaleExpression: Expression,
    val routeTrafficLineScaleExpression: Expression,
    val alternativeRouteLineScaleExpression: Expression,
    val alternativeRouteCasingLineScaleExpression: Expression,
    val alternativeRouteTrafficLineScaleExpression: Expression,
    val routeBlurScaleExpression: Expression,
) {

    /**
     * A builder class for [RouteLineScaleExpressions].
     */
    class Builder {

        private var routeLineBlurScaleExpression: Expression =
            MapboxRouteLineUtils.buildScalingExpression(
                listOf(
                    RouteLineScaleValue(10f, 7f, 2.0f),
                    RouteLineScaleValue(14f, 10.5f, 2.0f),
                    RouteLineScaleValue(16.5f, 15.5f, 2.0f),
                    RouteLineScaleValue(19f, 24f, 2.0f),
                    RouteLineScaleValue(22f, 29f, 2.0f),
                ),
            )

        private var routeLineScaleExpression: Expression =
            MapboxRouteLineUtils.buildScalingExpression(
                listOf(
                    RouteLineScaleValue(4f, 3f, 1f),
                    RouteLineScaleValue(10f, 4f, 1f),
                    RouteLineScaleValue(13f, 6f, 1f),
                    RouteLineScaleValue(16f, 10f, 1f),
                    RouteLineScaleValue(19f, 14f, 1f),
                    RouteLineScaleValue(22f, 18f, 1f),
                ),
            )
        private var routeCasingLineScaleExpression: Expression =
            MapboxRouteLineUtils.buildScalingExpression(
                listOf(
                    RouteLineScaleValue(10f, 7f, 1f),
                    RouteLineScaleValue(14f, 10.5f, 1f),
                    RouteLineScaleValue(16.5f, 15.5f, 1f),
                    RouteLineScaleValue(19f, 24f, 1f),
                    RouteLineScaleValue(22f, 29f, 1f),
                ),
            )
        private var routeTrafficLineScaleExpression: Expression =
            MapboxRouteLineUtils.buildScalingExpression(
                listOf(
                    RouteLineScaleValue(4f, 3f, 1f),
                    RouteLineScaleValue(10f, 4f, 1f),
                    RouteLineScaleValue(13f, 6f, 1f),
                    RouteLineScaleValue(16f, 10f, 1f),
                    RouteLineScaleValue(19f, 14f, 1f),
                    RouteLineScaleValue(22f, 18f, 1f),
                ),
            )
        private var alternativeRouteLineScaleExpression: Expression =
            MapboxRouteLineUtils.buildScalingExpression(
                listOf(
                    RouteLineScaleValue(4f, 3f, 1f),
                    RouteLineScaleValue(10f, 4f, 1f),
                    RouteLineScaleValue(13f, 6f, 1f),
                    RouteLineScaleValue(16f, 10f, 1f),
                    RouteLineScaleValue(19f, 14f, 1f),
                    RouteLineScaleValue(22f, 18f, 1f),
                ),
            )
        private var alternativeRouteCasingLineScaleExpression: Expression =
            MapboxRouteLineUtils.buildScalingExpression(
                listOf(
                    RouteLineScaleValue(10f, 7f, 1f),
                    RouteLineScaleValue(14f, 10.5f, 1f),
                    RouteLineScaleValue(16.5f, 15.5f, 1f),
                    RouteLineScaleValue(19f, 24f, 1f),
                    RouteLineScaleValue(22f, 29f, 1f),
                ),
            )
        private var alternativeRouteTrafficLineScaleExpression: Expression =
            MapboxRouteLineUtils.buildScalingExpression(
                listOf(
                    RouteLineScaleValue(4f, 3f, 1f),
                    RouteLineScaleValue(10f, 4f, 1f),
                    RouteLineScaleValue(13f, 6f, 1f),
                    RouteLineScaleValue(16f, 10f, 1f),
                    RouteLineScaleValue(19f, 14f, 1f),
                    RouteLineScaleValue(22f, 18f, 1f),
                ),
            )

        /**
         * An expression that will define the scaling behavior of the route line.
         *
         * @param expression the expression governing the scaling of the route line
         *
         * @return the builder
         */
        fun routeLineScaleExpression(expression: Expression): Builder = apply {
            this.routeLineScaleExpression = expression
        }

        /**
         * An expression that will define the scaling behavior of the route casing line.
         *
         * @param expression the expression governing the scaling of the route line
         *
         * @return the builder
         */
        fun routeCasingLineScaleExpression(expression: Expression): Builder = apply {
            this.routeCasingLineScaleExpression = expression
        }

        /**
         * An expression that will define the scaling behavior of the route traffic line.
         *
         * @param expression the expression governing the scaling of the route line
         *
         * @return the builder
         */
        fun routeTrafficLineScaleExpression(expression: Expression): Builder = apply {
            this.routeTrafficLineScaleExpression = expression
        }

        /**
         * An expression that will define the scaling behavior of the alternative route line.
         *
         * @param expression the expression governing the scaling of the alternative route line
         *
         * @return the builder
         */
        fun alternativeRouteLineScaleExpression(expression: Expression): Builder = apply {
            this.alternativeRouteLineScaleExpression = expression
        }

        /**
         * An expression that will define the scaling behavior of the alternative route casing line.
         *
         * @param expression the expression governing the behavior of alternative route casing
         * line scaling
         *
         * @return the builder
         */
        fun alternativeRouteCasingLineScaleExpression(expression: Expression): Builder = apply {
            this.alternativeRouteCasingLineScaleExpression = expression
        }

        /**
         * An expression that will define the scaling behavior of the alternative route traffic line.
         *
         * @param expression the expression governing the behavior of alternative route traffic
         * line scaling
         *
         * @return the builder
         */
        fun alternativeRouteTrafficLineScaleExpression(expression: Expression): Builder = apply {
            this.alternativeRouteTrafficLineScaleExpression = expression
        }

        /**
         * An expression that will define the scaling behavior of the blur applied to the route line.
         *
         * @param expression the expression to use for the blur effect of the route line
         *
         * @return the builder
         */
        fun routeLineBlurExpression(expression: Expression): Builder = apply {
            this.routeLineBlurScaleExpression = expression
        }

        /**
         * Build a [RouteLineScaleExpressions] object.
         */
        fun build(): RouteLineScaleExpressions {
            return RouteLineScaleExpressions(
                routeLineScaleExpression,
                routeCasingLineScaleExpression,
                routeTrafficLineScaleExpression,
                alternativeRouteLineScaleExpression,
                alternativeRouteCasingLineScaleExpression,
                alternativeRouteTrafficLineScaleExpression,
                routeLineBlurScaleExpression,
            )
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineScaleExpressions

        if (routeLineScaleExpression != other.routeLineScaleExpression) return false
        if (routeCasingLineScaleExpression != other.routeCasingLineScaleExpression) return false
        if (routeTrafficLineScaleExpression != other.routeTrafficLineScaleExpression) return false
        if (alternativeRouteLineScaleExpression != other.alternativeRouteLineScaleExpression) {
            return false
        }
        if (
            alternativeRouteCasingLineScaleExpression !=
            other.alternativeRouteCasingLineScaleExpression
        ) {
            return false
        }
        if (
            alternativeRouteTrafficLineScaleExpression !=
            other.alternativeRouteTrafficLineScaleExpression
        ) {
            return false
        }
        if (routeBlurScaleExpression != other.routeBlurScaleExpression) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = routeLineScaleExpression.hashCode()
        result = 31 * result + routeCasingLineScaleExpression.hashCode()
        result = 31 * result + routeTrafficLineScaleExpression.hashCode()
        result = 31 * result + alternativeRouteLineScaleExpression.hashCode()
        result = 31 * result + alternativeRouteCasingLineScaleExpression.hashCode()
        result = 31 * result + alternativeRouteTrafficLineScaleExpression.hashCode()
        result = 31 * result + routeBlurScaleExpression.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineScaleExpressions(" +
            "routeLineScaleExpression=$routeLineScaleExpression, " +
            "routeCasingLineScaleExpression=$routeCasingLineScaleExpression, " +
            "routeTrafficLineScaleExpression=$routeTrafficLineScaleExpression, " +
            "alternativeRouteLineScaleExpression=$alternativeRouteLineScaleExpression, " +
            "alternativeRouteCasingLineScaleExpression=" +
            "$alternativeRouteCasingLineScaleExpression, " +
            "alternativeRouteTrafficLineScaleExpression=" +
            "$alternativeRouteTrafficLineScaleExpression," +
            "routeGlowScaleExpression=$routeBlurScaleExpression" +
            ")"
    }
}
