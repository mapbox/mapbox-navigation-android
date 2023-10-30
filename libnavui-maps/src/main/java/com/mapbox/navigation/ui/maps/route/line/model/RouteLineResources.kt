package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.DrawableRes
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.buildScalingExpression
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.DESTINATION_WAYPOINT_ICON
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ORIGIN_WAYPOINT_ICON
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_ROAD_DASH_ARRAY
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_ROAD_LINE_OPACITY
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_ROAD_LINE_WIDTH
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ROUNDED_LINE_CAP
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ROUTE_LINE_BLUR
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ROUTE_LINE_BLUR_OPACITY
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TRAFFIC_BACKFILL_ROAD_CLASSES

/**
 * Contains colors an other values used to determine the appearance of the route line.
 *
 * @param routeLineColorResources an instance of [RouteLineColorResources]
 * @param roundedLineCap indicates if the endpoints of the route line have rounded line cap
 * @param originWaypointIcon an icon representing the origin point of a route
 * @param destinationWaypointIcon an icon representing the destination point of a route
 * @param trafficBackfillRoadClasses for map styles that have been configured to substitute the low
 * traffic congestion color for unknown traffic conditions for specified road classes, the same
 * road classes can be specified here to make the same substitution on the route line
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
 * alternative route traffic line scaling
 * @param restrictedRoadDashArray the dash array for the [LineLayer] used for displaying restricted roads
 * @param restrictedRoadOpacity the opacity of the restricted road [LineLayer]
 * @param restrictedRoadLineWidth the width of the restricted road [LineLayer]
 * @param routeLineBlur blur applied to the line, in pixels
 * @param routeLineBlurWidth the width of the blurred route line.
 * Blur line should be wider than route line itself to be visible
 * @param routeLineBlurOpacity the opacity of the blurred route line
 */
class RouteLineResources private constructor(
    val routeLineColorResources: RouteLineColorResources,
    val roundedLineCap: Boolean,
    @DrawableRes val originWaypointIcon: Int,
    @DrawableRes val destinationWaypointIcon: Int,
    val trafficBackfillRoadClasses: List<String>,
    val routeLineScaleExpression: Expression,
    val routeCasingLineScaleExpression: Expression,
    val routeTrafficLineScaleExpression: Expression,
    val alternativeRouteLineScaleExpression: Expression,
    val alternativeRouteCasingLineScaleExpression: Expression,
    val alternativeRouteTrafficLineScaleExpression: Expression,
    val restrictedRoadDashArray: List<Double>,
    val restrictedRoadOpacity: Double,
    val restrictedRoadLineWidth: Double,
    val routeLineBlur: Double,
    val routeLineBlurWidth: Expression,
    val routeLineBlurOpacity: Double,
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .routeLineColorResources(routeLineColorResources)
            .roundedLineCap(roundedLineCap)
            .originWaypointIcon(originWaypointIcon)
            .destinationWaypointIcon(destinationWaypointIcon)
            .trafficBackfillRoadClasses(trafficBackfillRoadClasses)
            .routeLineScaleExpression(routeLineScaleExpression)
            .routeCasingLineScaleExpression(routeCasingLineScaleExpression)
            .routeTrafficLineScaleExpression(routeTrafficLineScaleExpression)
            .alternativeRouteLineScaleExpression(alternativeRouteLineScaleExpression)
            .alternativeRouteCasingLineScaleExpression(alternativeRouteCasingLineScaleExpression)
            .alternativeRouteTrafficLineScaleExpression(alternativeRouteTrafficLineScaleExpression)
            .restrictedRoadDashArray(restrictedRoadDashArray)
            .restrictedRoadOpacity(restrictedRoadOpacity)
            .restrictedRoadLineWidth(restrictedRoadLineWidth)
            .routeLineBlur(routeLineBlur)
            .routeLineBlurWidth(routeLineBlurWidth)
            .routeLineBlurOpacity(routeLineBlurOpacity)
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineResources(" +
            "routeLineColorResources=$routeLineColorResources, " +
            "roundedLineCap=$roundedLineCap, " +
            "originWaypointIcon=$originWaypointIcon, " +
            "destinationWaypointIcon=$destinationWaypointIcon, " +
            "routeLineScaleExpression=$routeLineScaleExpression, " +
            "routeCasingLineScaleExpression=$routeCasingLineScaleExpression, " +
            "routeTrafficLineScaleExpression=$routeTrafficLineScaleExpression, " +
            "trafficBackfillRoadClasses=$trafficBackfillRoadClasses, " +
            "alternativeRouteLineScaleExpression=$alternativeRouteLineScaleExpression, " +
            "alternativeRouteCasingLineScaleExpression=" +
            "$alternativeRouteCasingLineScaleExpression, " +
            "alternativeRouteTrafficLineScaleExpression=" +
            "$alternativeRouteTrafficLineScaleExpression, " +
            "routeLineColorResources=$routeLineColorResources, " +
            "roundedLineCap=$roundedLineCap, " +
            "originWaypointIcon=$originWaypointIcon, " +
            "destinationWaypointIcon=$destinationWaypointIcon, " +
            "routeLineScaleExpression=$routeLineScaleExpression, " +
            "routeCasingLineScaleExpression=$routeCasingLineScaleExpression, " +
            "routeTrafficLineScaleExpression=$routeTrafficLineScaleExpression, " +
            "trafficBackfillRoadClasses=$trafficBackfillRoadClasses, " +
            "restrictedRoadDashArray=$restrictedRoadDashArray, " +
            "restrictedRoadOpacity=$restrictedRoadOpacity, " +
            "restrictedRoadLineWidth=$restrictedRoadLineWidth, " +
            "routeLineBlur=$routeLineBlur, " +
            "routeLineBlurWidth=$routeLineBlurWidth, " +
            "routeLineBlurOpacity=$routeLineBlurOpacity" +
            ")"
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineResources

        if (routeLineColorResources != other.routeLineColorResources) return false
        if (roundedLineCap != other.roundedLineCap) return false
        if (originWaypointIcon != other.originWaypointIcon) return false
        if (destinationWaypointIcon != other.destinationWaypointIcon) return false
        if (trafficBackfillRoadClasses != other.trafficBackfillRoadClasses) return false
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
        if (restrictedRoadDashArray != other.restrictedRoadDashArray) return false
        if (restrictedRoadOpacity != other.restrictedRoadOpacity) return false
        if (restrictedRoadLineWidth != other.restrictedRoadLineWidth) return false

        if (routeLineBlur != other.routeLineBlur) return false
        if (routeLineBlurWidth != other.routeLineBlurWidth) return false
        if (routeLineBlurOpacity != other.routeLineBlurOpacity) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = 31 * routeLineColorResources.hashCode()
        result = 31 * result + roundedLineCap.hashCode()
        result = 31 * result + originWaypointIcon
        result = 31 * result + destinationWaypointIcon
        result = 31 * result + trafficBackfillRoadClasses.hashCode()
        result = 31 * result + routeLineScaleExpression.hashCode()
        result = 31 * result + routeCasingLineScaleExpression.hashCode()
        result = 31 * result + routeTrafficLineScaleExpression.hashCode()
        result = 31 * result + alternativeRouteLineScaleExpression.hashCode()
        result = 31 * result + alternativeRouteCasingLineScaleExpression.hashCode()
        result = 31 * result + alternativeRouteTrafficLineScaleExpression.hashCode()
        result = 31 * result + restrictedRoadDashArray.hashCode()
        result = 31 * result + restrictedRoadOpacity.hashCode()
        result = 31 * result + restrictedRoadLineWidth.hashCode()
        result = 31 * result + routeLineBlur.hashCode()
        result = 31 * result + routeLineBlurWidth.hashCode()
        result = 31 * result + routeLineBlurOpacity.hashCode()
        return result
    }

    /**
     * A builder for instantiating the RouteLineResources class
     */
    class Builder {
        private var routeLineColorResources: RouteLineColorResources? = null
        private var roundedLineCap: Boolean = ROUNDED_LINE_CAP
        private var originWaypointIcon: Int = ORIGIN_WAYPOINT_ICON
        private var destinationWaypointIcon: Int = DESTINATION_WAYPOINT_ICON
        private var trafficBackfillRoadClasses: List<String> = TRAFFIC_BACKFILL_ROAD_CLASSES
        private var restrictedRoadDashArray: List<Double> = RESTRICTED_ROAD_DASH_ARRAY
        private var restrictedRoadOpacity: Double = RESTRICTED_ROAD_LINE_OPACITY
        private var restrictedRoadLineWidth: Double = RESTRICTED_ROAD_LINE_WIDTH

        private var routeLineBlur: Double = ROUTE_LINE_BLUR
        private var routeLineBlurOpacity: Double = ROUTE_LINE_BLUR_OPACITY

        private var routeLineScaleExpression: Expression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 1f),
                RouteLineScaleValue(10f, 4f, 1f),
                RouteLineScaleValue(13f, 6f, 1f),
                RouteLineScaleValue(16f, 10f, 1f),
                RouteLineScaleValue(19f, 14f, 1f),
                RouteLineScaleValue(22f, 18f, 1f)
            )
        )
        private var routeCasingLineScaleExpression: Expression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(10f, 7f, 1f),
                RouteLineScaleValue(14f, 10.5f, 1f),
                RouteLineScaleValue(16.5f, 15.5f, 1f),
                RouteLineScaleValue(19f, 24f, 1f),
                RouteLineScaleValue(22f, 29f, 1f)
            )
        )
        private var routeTrafficLineScaleExpression: Expression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 1f),
                RouteLineScaleValue(10f, 4f, 1f),
                RouteLineScaleValue(13f, 6f, 1f),
                RouteLineScaleValue(16f, 10f, 1f),
                RouteLineScaleValue(19f, 14f, 1f),
                RouteLineScaleValue(22f, 18f, 1f)
            )
        )
        private var alternativeRouteLineScaleExpression: Expression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 1f),
                RouteLineScaleValue(10f, 4f, 1f),
                RouteLineScaleValue(13f, 6f, 1f),
                RouteLineScaleValue(16f, 10f, 1f),
                RouteLineScaleValue(19f, 14f, 1f),
                RouteLineScaleValue(22f, 18f, 1f)
            )
        )
        private var alternativeRouteCasingLineScaleExpression: Expression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(10f, 7f, 1f),
                RouteLineScaleValue(14f, 10.5f, 1f),
                RouteLineScaleValue(16.5f, 15.5f, 1f),
                RouteLineScaleValue(19f, 24f, 1f),
                RouteLineScaleValue(22f, 29f, 1f)
            )
        )
        private var alternativeRouteTrafficLineScaleExpression: Expression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, 1f),
                RouteLineScaleValue(10f, 4f, 1f),
                RouteLineScaleValue(13f, 6f, 1f),
                RouteLineScaleValue(16f, 10f, 1f),
                RouteLineScaleValue(19f, 14f, 1f),
                RouteLineScaleValue(22f, 18f, 1f)
            )
        )

        private var routeLineBlurWidth: Expression = buildScalingExpression(
            listOf(
                RouteLineScaleValue(10f, 14f, 1f),
                RouteLineScaleValue(14f, 21f, 1f),
                RouteLineScaleValue(16.5f, 31f, 1f),
                RouteLineScaleValue(19f, 48f, 1f),
                RouteLineScaleValue(22f, 58f, 1f)
            )
        )

        /**
         * The route line color resources to use.
         *
         * @param routeLineColorResources an instance of [RouteLineColorResources]
         *
         * @return the builder
         */
        fun routeLineColorResources(routeLineColorResources: RouteLineColorResources): Builder =
            apply { this.routeLineColorResources = routeLineColorResources }

        /**
         * Indicates if the endpoints of the route line have rounded line cap.
         *
         * @param roundLineCap true if the end points should be rounded
         *
         * @return the builder
         */
        fun roundedLineCap(roundLineCap: Boolean): Builder =
            apply { this.roundedLineCap = roundLineCap }

        /**
         * An icon representing the origin point of a route.
         *
         * @param resource the drawable resource to be used
         *
         * @return the builder
         */
        fun originWaypointIcon(@DrawableRes resource: Int): Builder =
            apply { this.originWaypointIcon = resource }

        /**
         * An icon representing the destination point of a route.
         *
         * @param resource the drawable resource to be used
         *
         * @return the builder
         */
        fun destinationWaypointIcon(@DrawableRes resource: Int): Builder =
            apply { this.destinationWaypointIcon = resource }

        /**
         * For map styles that have been configured to substitute the low traffic congestion color
         * for unknown traffic conditions for specified road classes, the same road classes
         * can be specified here to make the same substitution on the route line.
         *
         * @param roadClasses a collection road classes
         *
         * @return the builder
         */
        fun trafficBackfillRoadClasses(roadClasses: List<String>): Builder =
            apply { this.trafficBackfillRoadClasses = roadClasses }

        /**
         * An expression that will define the scaling behavior of the route line.
         *
         * @param expression the expression governing the scaling of the route line
         *
         * @return the builder
         */
        fun routeLineScaleExpression(expression: Expression): Builder =
            apply { this.routeLineScaleExpression = expression }

        /**
         * An expression that will define the scaling behavior of the route casing line.
         *
         * @param expression the expression governing the scaling of the route line
         *
         * @return the builder
         */
        fun routeCasingLineScaleExpression(expression: Expression): Builder =
            apply { this.routeCasingLineScaleExpression = expression }

        /**
         * An expression that will define the scaling behavior of the route traffic line.
         *
         * @param expression the expression governing the scaling of the route line
         *
         * @return the builder
         */
        fun routeTrafficLineScaleExpression(expression: Expression): Builder =
            apply { this.routeTrafficLineScaleExpression = expression }

        /**
         * An expression that will define the scaling behavior of the alternative route line.
         *
         * @param expression the expression governing the scaling of the alternative route line
         *
         * @return the builder
         */
        fun alternativeRouteLineScaleExpression(expression: Expression): Builder =
            apply { this.alternativeRouteLineScaleExpression = expression }

        /**
         * An expression that will define the scaling behavior of the alternative route casing line.
         *
         * @param expression the expression governing the behavior of alternative route casing
         * line scaling
         *
         * @return the builder
         */
        fun alternativeRouteCasingLineScaleExpression(expression: Expression): Builder =
            apply { this.alternativeRouteCasingLineScaleExpression = expression }

        /**
         * An expression that will define the scaling behavior of the alternative route traffic line.
         *
         * @param expression the expression governing the behavior of alternative route traffic
         * line scaling
         *
         * @return the builder
         */
        fun alternativeRouteTrafficLineScaleExpression(expression: Expression): Builder =
            apply { this.alternativeRouteTrafficLineScaleExpression = expression }

        /**
         * The dash array parameter for the restricted road layer.
         * See [LineLayer] LineDashArray for more information. An empty list will show a
         * solid line.  The default is [.5, 2.0]
         *
         * @param dashArray value of the LineDash array
         */
        fun restrictedRoadDashArray(dashArray: List<Double>): Builder =
            apply { this.restrictedRoadDashArray = dashArray }

        /**
         * The opacity for the restricted road line. The default is 1.0.
         *
         * @param opacity the opacity value
         */
        fun restrictedRoadOpacity(opacity: Double): Builder =
            apply { this.restrictedRoadOpacity = opacity }

        /**
         * The width of the restricted road line
         *
         * @param width the width of the line
         */
        fun restrictedRoadLineWidth(width: Double): Builder =
            apply { this.restrictedRoadLineWidth = width }

        /**
         * Blur applied to the route line, in pixels.
         *
         * @param blur the blur value
         */
        fun routeLineBlur(blur: Double): Builder =
            apply { this.routeLineBlur = blur }

        /**
         * The width of the blurred route line.
         * Blur line should be wider than route line itself to be visible.
         *
         * @param width the width of the blurred route line
         */
        fun routeLineBlurWidth(width: Expression): Builder =
            apply { this.routeLineBlurWidth = width }

        /**
         * The opacity of the blurred route line
         *
         * @param opacity the opacity value
         */
        fun routeLineBlurOpacity(opacity: Double): Builder =
            apply { this.routeLineBlurOpacity = opacity }

        /**
         * Creates a instance of RouteLineResources
         *
         * @return the instance
         */
        fun build(): RouteLineResources {
            val theRouteLineColorResources: RouteLineColorResources =
                routeLineColorResources ?: RouteLineColorResources.Builder().build()
            return RouteLineResources(
                theRouteLineColorResources,
                roundedLineCap,
                originWaypointIcon,
                destinationWaypointIcon,
                trafficBackfillRoadClasses,
                routeLineScaleExpression,
                routeCasingLineScaleExpression,
                routeTrafficLineScaleExpression,
                alternativeRouteLineScaleExpression,
                alternativeRouteCasingLineScaleExpression,
                alternativeRouteTrafficLineScaleExpression,
                restrictedRoadDashArray,
                restrictedRoadOpacity,
                restrictedRoadLineWidth,
                routeLineBlur,
                routeLineBlurWidth,
                routeLineBlurOpacity,
            )
        }
    }
}
