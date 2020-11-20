package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATE_ROUTE_CASING_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATE_ROUTE_DEFAULT_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATE_ROUTE_HEAVY_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATE_ROUTE_LOW_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATE_ROUTE_MODERATE_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATE_ROUTE_SEVERE_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATE_ROUTE_UNKNOWN_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.DESTINATION_WAYPOINT_ICON
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ORIGIN_WAYPOINT_ICON
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUNDED_LINE_CAP
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUTE_CASING_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUTE_DEFAULT_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUTE_HEAVY_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUTE_LINE_TRAVELED_CASING_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUTE_LINE_TRAVELED_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUTE_LOW_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUTE_MODERATE_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUTE_SEVERE_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ROUTE_UNKNOWN_TRAFFIC_COLOR
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.TRAFFIC_BACKFILL_ROAD_CLASSES
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.buildScalingExpression

/**
 * Contains colors an other values used to determine the appearance of the route line.
 *
 * @param routeLineTraveledColor the color of the section of route line behind the puck
 * representing the section of the route traveled
 * @param routeLineTraveledCasingColor the color of the casing section of route line behind the
 * puck representing the section of the route traveled. By default the casing line is beneath
 * the route line and gives the appearance of a border
 * @param routeUnknownTrafficColor the color used for representing unknown traffic congestion
 * @param routeDefaultColor the default color of the route line
 * @param routeLowCongestionColor the color used for representing low traffic congestion
 * @param routeModerateColor the color used for representing moderate traffic congestion
 * @param routeHeavyColor the color used for representing heavy traffic congestion
 * @param routeSevereColor the color used for representing severe traffic congestion
 * @param routeCasingColor the color used for the route casing line which is positioned below
 * the route line giving the line the appearance of a boarder
 * @param roundedLineCap indicates if the endpoints of the route line have rounded line cap
 * @param alternativeRouteUnknownTrafficColor the color used for representing unknown traffic
 * congestion on alternative routes
 * @param alternativeRouteDefaultColor the default color used for alternative route lines
 * @param alternativeRouteLowColor the color used for representing low traffic congestion on
 * alternative routes
 * @param alternativeRouteModerateColor the color used for representing moderate traffic congestion
 * on alternative routes
 * @param alternativeRouteHeavyColor the color used for representing heavy traffic congestion on
 * alternative routes
 * @param alternativeRouteSevereColor the color used for representing severe traffic congestion
 * on alternative routes
 * @param alternativeRouteCasingColor the color used for the alternative route casing line(s) which
 * is positioned below the route line giving the line the appearance of a boarder
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
 */
class RouteLineResources private constructor(
    @ColorInt val routeLineTraveledColor: Int,
    @ColorInt val routeLineTraveledCasingColor: Int,
    @ColorInt val routeUnknownTrafficColor: Int,
    @ColorInt val routeDefaultColor: Int,
    @ColorInt val routeLowCongestionColor: Int,
    @ColorInt val routeModerateColor: Int,
    @ColorInt val routeHeavyColor: Int,
    @ColorInt val routeSevereColor: Int,
    @ColorInt val routeCasingColor: Int,
    val roundedLineCap: Boolean,
    @ColorInt val alternativeRouteUnknownTrafficColor: Int,
    @ColorInt val alternativeRouteDefaultColor: Int,
    @ColorInt val alternativeRouteLowColor: Int,
    @ColorInt val alternativeRouteModerateColor: Int,
    @ColorInt val alternativeRouteHeavyColor: Int,
    @ColorInt val alternativeRouteSevereColor: Int,
    @ColorInt val alternativeRouteCasingColor: Int,
    @DrawableRes val originWaypointIcon: Int,
    @DrawableRes val destinationWaypointIcon: Int,
    val trafficBackfillRoadClasses: List<String>,
    val routeLineScaleExpression: Expression,
    val routeCasingLineScaleExpression: Expression,
    val routeTrafficLineScaleExpression: Expression
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .routeLineTraveledColor(routeLineTraveledColor)
            .routeLineTraveledCasingColor(routeLineTraveledCasingColor)
            .routeUnknownTrafficColor(routeUnknownTrafficColor)
            .routeDefaultColor(routeDefaultColor)
            .routeLowCongestionColor(routeLowCongestionColor)
            .routeModerateColor(routeModerateColor)
            .routeHeavyColor(routeHeavyColor)
            .routeSevereColor(routeSevereColor)
            .routeCasingColor(routeCasingColor)
            .roundedLineCap(roundedLineCap)
            .alternativeRouteUnknownTrafficColor(alternativeRouteUnknownTrafficColor)
            .alternativeRouteDefaultColor(alternativeRouteDefaultColor)
            .alternativeRouteLowColor(alternativeRouteLowColor)
            .alternativeRouteModerateColor(alternativeRouteModerateColor)
            .alternativeRouteHeavyColor(alternativeRouteHeavyColor)
            .alternativeRouteSevereColor(alternativeRouteSevereColor)
            .alternativeRouteCasingColor(alternativeRouteCasingColor)
            .originWaypointIcon(originWaypointIcon)
            .destinationWaypointIcon(destinationWaypointIcon)
            .trafficBackfillRoadClasses(trafficBackfillRoadClasses)
            .routeLineScaleExpression(routeLineScaleExpression)
            .routeCasingLineScaleExpression(routeCasingLineScaleExpression)
            .routeTrafficLineScaleExpression(routeTrafficLineScaleExpression)
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineResources(" +
            "routeLineTraveledColor=$routeLineTraveledColor)" +
            "routeLineTraveledCasingColor=$routeLineTraveledCasingColor" +
            "routeUnknownTrafficColor=$routeUnknownTrafficColor" +
            "routeDefaultColor=$routeDefaultColor" +
            "routeLowCongestionColor=$routeLowCongestionColor" +
            "routeModerateColor=$routeModerateColor" +
            "routeHeavyColor=$routeHeavyColor" +
            "routeSevereColor=$routeSevereColor" +
            "routeCasingColor=$routeCasingColor" +
            "roundedLineCap=$roundedLineCap" +
            "alternativeRouteUnknownTrafficColor=$alternativeRouteUnknownTrafficColor" +
            "alternativeRouteDefaultColor=$alternativeRouteDefaultColor" +
            "alternativeRouteLowColor=$alternativeRouteLowColor" +
            "alternativeRouteModerateColor=$alternativeRouteModerateColor" +
            "alternativeRouteHeavyColor=$alternativeRouteHeavyColor" +
            "alternativeRouteSevereColor=$alternativeRouteSevereColor" +
            "alternativeRouteCasingColor=$alternativeRouteCasingColor" +
            "originWaypointIcon=$originWaypointIcon" +
            "destinationWaypointIcon=$destinationWaypointIcon" +
            "routeLineScaleExpression=$routeLineScaleExpression" +
            "routeCasingLineScaleExpression=$routeCasingLineScaleExpression" +
            "routeTrafficLineScaleExpression=$routeTrafficLineScaleExpression" +
            "trafficBackfillRoadClasses=$trafficBackfillRoadClasses"
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineResources

        if (routeLineTraveledColor != other.routeLineTraveledColor) return false
        if (routeLineTraveledCasingColor != other.routeLineTraveledCasingColor) return false
        if (routeUnknownTrafficColor != other.routeUnknownTrafficColor) return false
        if (routeDefaultColor != other.routeDefaultColor) return false
        if (routeLowCongestionColor != other.routeLowCongestionColor) return false
        if (routeModerateColor != other.routeModerateColor) return false
        if (routeHeavyColor != other.routeHeavyColor) return false
        if (routeSevereColor != other.routeSevereColor) return false
        if (routeCasingColor != other.routeCasingColor) return false
        if (roundedLineCap != other.roundedLineCap) return false
        if (alternativeRouteDefaultColor != other.alternativeRouteDefaultColor) return false
        if (alternativeRouteLowColor != other.alternativeRouteLowColor) return false
        if (alternativeRouteModerateColor != other.alternativeRouteModerateColor) return false
        if (alternativeRouteHeavyColor != other.alternativeRouteHeavyColor) return false
        if (alternativeRouteSevereColor != other.alternativeRouteSevereColor) return false
        if (alternativeRouteCasingColor != other.alternativeRouteCasingColor) return false
        if (originWaypointIcon != other.originWaypointIcon) return false
        if (destinationWaypointIcon != other.destinationWaypointIcon) return false
        if (trafficBackfillRoadClasses != other.trafficBackfillRoadClasses) return false
        if (routeLineScaleExpression != other.routeLineScaleExpression) return false
        if (routeCasingLineScaleExpression != other.routeCasingLineScaleExpression) return false
        if (routeTrafficLineScaleExpression != other.routeTrafficLineScaleExpression) return false
        if (alternativeRouteUnknownTrafficColor != other.alternativeRouteUnknownTrafficColor)
            return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = routeLineTraveledColor
        result = 31 * result + routeLineTraveledCasingColor
        result = 31 * result + routeUnknownTrafficColor
        result = 31 * result + routeDefaultColor
        result = 31 * result + routeLowCongestionColor
        result = 31 * result + routeModerateColor
        result = 31 * result + routeHeavyColor
        result = 31 * result + routeSevereColor
        result = 31 * result + routeCasingColor
        result = 31 * result + roundedLineCap.hashCode()
        result = 31 * result + alternativeRouteUnknownTrafficColor
        result = 31 * result + alternativeRouteDefaultColor
        result = 31 * result + alternativeRouteLowColor
        result = 31 * result + alternativeRouteModerateColor
        result = 31 * result + alternativeRouteHeavyColor
        result = 31 * result + alternativeRouteSevereColor
        result = 31 * result + alternativeRouteCasingColor
        result = 31 * result + originWaypointIcon
        result = 31 * result + destinationWaypointIcon
        result = 31 * result + trafficBackfillRoadClasses.hashCode()
        result = 31 * result + routeLineScaleExpression.hashCode()
        result = 31 * result + routeCasingLineScaleExpression.hashCode()
        result = 31 * result + routeTrafficLineScaleExpression.hashCode()
        return result
    }

    /**
     * A builder for instantiating the RouteLineResources class
     */
    class Builder {
        private var routeLineTraveledColor: Int = ROUTE_LINE_TRAVELED_COLOR
        private var routeLineTraveledCasingColor: Int = ROUTE_LINE_TRAVELED_CASING_COLOR
        private var routeUnknownTrafficColor: Int = ROUTE_UNKNOWN_TRAFFIC_COLOR
        private var routeDefaultColor: Int = ROUTE_DEFAULT_COLOR
        private var routeLowCongestionColor: Int = ROUTE_LOW_TRAFFIC_COLOR
        private var routeModerateColor: Int = ROUTE_MODERATE_TRAFFIC_COLOR
        private var routeHeavyColor: Int = ROUTE_HEAVY_TRAFFIC_COLOR
        private var routeSevereColor: Int = ROUTE_SEVERE_TRAFFIC_COLOR
        private var routeCasingColor: Int = ROUTE_CASING_COLOR
        private var roundedLineCap: Boolean = ROUNDED_LINE_CAP
        private var alternativeRouteUnknownTrafficColor: Int = ALTERNATE_ROUTE_UNKNOWN_TRAFFIC_COLOR
        private var alternativeRouteDefaultColor: Int = ALTERNATE_ROUTE_DEFAULT_COLOR
        private var alternativeRouteLowColor: Int = ALTERNATE_ROUTE_LOW_TRAFFIC_COLOR
        private var alternativeRouteModerateColor: Int = ALTERNATE_ROUTE_MODERATE_TRAFFIC_COLOR
        private var alternativeRouteHeavyColor: Int = ALTERNATE_ROUTE_HEAVY_TRAFFIC_COLOR
        private var alternativeRouteSevereColor: Int = ALTERNATE_ROUTE_SEVERE_TRAFFIC_COLOR
        private var alternativeRouteCasingColor: Int = ALTERNATE_ROUTE_CASING_COLOR
        private var originWaypointIcon: Int = ORIGIN_WAYPOINT_ICON
        private var destinationWaypointIcon: Int = DESTINATION_WAYPOINT_ICON
        private var trafficBackfillRoadClasses: List<String> = TRAFFIC_BACKFILL_ROAD_CLASSES
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

        /**
         * The color of the section of route line behind the puck representing the section
         * of the route traveled.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeLineTraveledColor(@ColorInt color: Int): Builder =
            apply { this.routeLineTraveledColor = color }

        /**
         * The color of the casing section of route line behind the puck representing the section
         * of the route traveled. By default the casing line is beneath the route line and
         * gives the appearance of a border.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeLineTraveledCasingColor(@ColorInt color: Int): Builder =
            apply { this.routeLineTraveledCasingColor = color }

        /**
         * The color used for representing unknown traffic congestion.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeUnknownTrafficColor(@ColorInt color: Int): Builder =
            apply { this.routeUnknownTrafficColor = color }

        /**
         * The default color of the route line
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeDefaultColor(@ColorInt color: Int): Builder =
            apply { this.routeDefaultColor = color }

        /**
         * The color used for representing low traffic congestion.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeLowCongestionColor(@ColorInt color: Int): Builder =
            apply { this.routeLowCongestionColor = color }

        /**
         * The color used for representing moderate traffic congestion.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeModerateColor(@ColorInt color: Int): Builder =
            apply { this.routeModerateColor = color }

        /**
         * The color used for representing heavy traffic congestion.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeHeavyColor(@ColorInt color: Int): Builder =
            apply { this.routeHeavyColor = color }

        /**
         * The color used for representing severe traffic congestion.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeSevereColor(@ColorInt color: Int): Builder =
            apply { this.routeSevereColor = color }

        /**
         * The color used for the route casing line which is positioned below the route line
         * giving the line the appearance of a boarder.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeCasingColor(@ColorInt color: Int): Builder =
            apply { this.routeCasingColor = color }

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
         * The color used for representing unknown traffic congestion on alternative routes.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteUnknownTrafficColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteUnknownTrafficColor = color }

        /**
         * The default color used for alternative route lines.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteDefaultColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteDefaultColor = color }

        /**
         * The color used for representing low traffic congestion on alternative routes.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteLowColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteLowColor = color }

        /**
         * The color used for representing moderate traffic congestion on alternative routes.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteModerateColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteModerateColor = color }

        /**
         * The color used for representing heavy traffic congestion on alternative routes.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteHeavyColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteHeavyColor = color }

        /**
         * The color used for representing severe traffic congestion on alternative routes.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteSevereColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteSevereColor = color }

        /**
         * The color used for the alternative route casing line(s) which is positioned below the route line
         * giving the line the appearance of a boarder.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteCasingColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteCasingColor = color }

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
         * Creates a instance of RouteLineResources
         *
         * @return the instance
         */
        fun build(): RouteLineResources {
            return RouteLineResources(
                routeLineTraveledColor,
                routeLineTraveledCasingColor,
                routeUnknownTrafficColor,
                routeDefaultColor,
                routeLowCongestionColor,
                routeModerateColor,
                routeHeavyColor,
                routeSevereColor,
                routeCasingColor,
                roundedLineCap,
                alternativeRouteUnknownTrafficColor,
                alternativeRouteDefaultColor,
                alternativeRouteLowColor,
                alternativeRouteModerateColor,
                alternativeRouteHeavyColor,
                alternativeRouteSevereColor,
                alternativeRouteCasingColor,
                originWaypointIcon,
                destinationWaypointIcon,
                trafficBackfillRoadClasses,
                routeLineScaleExpression,
                routeCasingLineScaleExpression,
                routeTrafficLineScaleExpression
            )
        }
    }
}
