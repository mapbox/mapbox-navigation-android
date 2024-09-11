package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.ColorInt
import androidx.annotation.Keep
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants

/**
 * Contains colors an other values used to determine the appearance of the route line.
 *
 * The congestion range is to be used if when making a route request you use
 * `DirectionCriteria.ANNOTATION_CONGESTION_NUMERIC` annotation. The congestion values obtained
 * with this annotation in the route response would be in the range 0..100, 0 being the minimum and
 * 100 being the maximum congestion values. `Unknown` congestion values will be represented as `null`
 * [RouteLineColorResources] defines 4 ranges:
 * - lowCongestionRange: default value spans from 0..39
 * - moderateCongestionRange: default value spans from 40..59
 * - heavyCongestionRange: default value spans from 60..79
 * - severeCongestionRange: default value spans from 80..100
 * You can specify your own ranges for all of the above using the [RouteLineColorResources.Builder].
 * If the ranges overlap, the [RouteLineColorResources.Builder.build] would fail and throw
 * [IllegalStateException]. You also need to make sure that all values from 0..100 are covered using
 * these four ranges, else any missing number would be treated as unknown congestion. For ex, if you
 * define the range as follows:
 * - lowCongestionRange:       0..39
 * - moderateCongestionRange: 50..59
 * - heavyCongestionRange:    60..79
 * - severeCongestionRange:   80..100
 * If the route response contains a congestion number 45, it would be treated as `unknown` because
 * it doesn't exist in any of the ranges specified.
 *
 * A note on visualizing route line colors: The route line is made up of several stacked line layers.
 * The top most line layer is the traffic line followed by the main route line beneath it. By default both lines
 * have the same width so the traffic line obscures the main line. Also the default route line color
 * and the color used for unknown and low traffic congestion is the same.
 *
 * Be mindful of this if you change the default route line color because if the traffic data in the route
 * is unknown the route line will appear as having the unknown traffic color rather than the default
 * route line color. In this case consider also changing the unknown/low traffic color to match the
 * default route line color or setting the traffic congestion color(s) to something like
 * Color.Transparent if it fits your use case.
 *
 * @param routeDefaultColor the default color of the route line
 * @param routeLowCongestionColor the color used for representing low traffic congestion
 * @param routeModerateCongestionColor the color used for representing moderate traffic congestion
 * @param routeHeavyCongestionColor the color used for representing heavy traffic congestion
 * @param routeSevereCongestionColor the color used for representing severe traffic congestion
 * @param routeUnknownCongestionColor the color used for representing unknown traffic congestion
 * @param inactiveRouteLegLowCongestionColor the color used for representing low traffic congestion on inactive legs of the route
 * @param inactiveRouteLegModerateCongestionColor the color used for representing moderate traffic congestion on inactive legs of the route
 * @param inactiveRouteLegHeavyCongestionColor the color used for representing heavy traffic congestion on inactive legs of the route
 * @param inactiveRouteLegSevereCongestionColor the color used for representing severe traffic congestion on inactive legs of the route
 * @param inactiveRouteLegUnknownCongestionColor the color used for representing unknown traffic congestion on inactive legs of the route
 * @param alternativeRouteDefaultColor the default color used for alternative route lines
 * @param alternativeRouteLowCongestionColor the color used for representing low traffic congestion on
 * alternative routes
 * @param alternativeRouteModerateCongestionColor the color used for representing moderate traffic congestion
 * on alternative routes
 * @param alternativeRouteHeavyCongestionColor the color used for representing heavy traffic congestion on
 * alternative routes
 * @param alternativeRouteSevereCongestionColor the color used for representing severe traffic congestion
 * on alternative routes
 * @param alternativeRouteUnknownCongestionColor the color used for representing unknown traffic
 * congestion on alternative routes
 * @param restrictedRoadColor the color for the restricted road indicator(s)
 * @param routeClosureColor the color used for the route closure line
 * @param inactiveRouteLegRestrictedRoadColor the color for the restricted road indicator(s) on inactive legs of the route
 * @param inactiveRouteLegClosureColor the color used for the route closure line on inactive legs of the route
 * @param alternativeRouteRestrictedRoadColor the color for the restricted road indicator(s) for
 * alternative routes.
 * @param alternativeRouteClosureColor the color used for the alternative route closure line(s)
 * @param routeLineTraveledColor the color of the section of route line behind the puck
 * representing the section of the route traveled
 * @param routeLineTraveledCasingColor the color of the casing section of route line behind the
 * puck representing the section of the route traveled. By default the casing line is beneath
 * the route line and gives the appearance of a border
 * @param routeCasingColor the color used for the route casing line which is positioned below
 * the route line giving the line the appearance of a boarder
 * @param alternativeRouteCasingColor the color used for the alternative route casing line(s) which
 * is positioned below the route line giving the line the appearance of a boarder
 * @param inactiveRouteLegCasingColor the color used for casing of route legs hat aren't currently
 * being navigated.
 * @param inActiveRouteLegsColor the color used for route legs that aren't currently
 * being navigated.
 */
@Keep
class RouteLineColorResources private constructor(
    @ColorInt val routeDefaultColor: Int,
    @ColorInt val routeLowCongestionColor: Int,
    @ColorInt val routeModerateCongestionColor: Int,
    @ColorInt val routeHeavyCongestionColor: Int,
    @ColorInt val routeSevereCongestionColor: Int,
    @ColorInt val routeUnknownCongestionColor: Int,
    @ColorInt val inactiveRouteLegLowCongestionColor: Int,
    @ColorInt val inactiveRouteLegModerateCongestionColor: Int,
    @ColorInt val inactiveRouteLegHeavyCongestionColor: Int,
    @ColorInt val inactiveRouteLegSevereCongestionColor: Int,
    @ColorInt val inactiveRouteLegUnknownCongestionColor: Int,
    @ColorInt val alternativeRouteDefaultColor: Int,
    @ColorInt val alternativeRouteLowCongestionColor: Int,
    @ColorInt val alternativeRouteModerateCongestionColor: Int,
    @ColorInt val alternativeRouteHeavyCongestionColor: Int,
    @ColorInt val alternativeRouteSevereCongestionColor: Int,
    @ColorInt val alternativeRouteUnknownCongestionColor: Int,
    @ColorInt val restrictedRoadColor: Int,
    @ColorInt val routeClosureColor: Int,
    @ColorInt val inactiveRouteLegRestrictedRoadColor: Int,
    @ColorInt val inactiveRouteLegClosureColor: Int,
    @ColorInt val alternativeRouteRestrictedRoadColor: Int,
    @ColorInt val alternativeRouteClosureColor: Int,
    @ColorInt val routeLineTraveledColor: Int,
    @ColorInt val routeLineTraveledCasingColor: Int,
    @ColorInt val routeCasingColor: Int,
    @ColorInt val alternativeRouteCasingColor: Int,
    @ColorInt val inactiveRouteLegCasingColor: Int,
    @ColorInt val inActiveRouteLegsColor: Int,
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .routeDefaultColor(routeDefaultColor)
            .routeLowCongestionColor(routeLowCongestionColor)
            .routeModerateCongestionColor(routeModerateCongestionColor)
            .routeHeavyCongestionColor(routeHeavyCongestionColor)
            .routeSevereCongestionColor(routeSevereCongestionColor)
            .routeUnknownCongestionColor(routeUnknownCongestionColor)
            .inactiveRouteLegLowCongestionColor(inactiveRouteLegLowCongestionColor)
            .inactiveRouteLegModerateCongestionColor(inactiveRouteLegModerateCongestionColor)
            .inactiveRouteLegHeavyCongestionColor(inactiveRouteLegHeavyCongestionColor)
            .inactiveRouteLegSevereCongestionColor(inactiveRouteLegSevereCongestionColor)
            .inactiveRouteLegUnknownCongestionColor(inactiveRouteLegUnknownCongestionColor)
            .routeClosureColor(routeClosureColor)
            .restrictedRoadColor(restrictedRoadColor)
            .inactiveRouteLegClosureColor(inactiveRouteLegClosureColor)
            .inactiveRouteLegRestrictedRoadColor(inactiveRouteLegRestrictedRoadColor)
            .alternativeRouteDefaultColor(alternativeRouteDefaultColor)
            .alternativeRouteLowCongestionColor(alternativeRouteLowCongestionColor)
            .alternativeRouteModerateCongestionColor(alternativeRouteModerateCongestionColor)
            .alternativeRouteHeavyCongestionColor(alternativeRouteHeavyCongestionColor)
            .alternativeRouteSevereCongestionColor(alternativeRouteSevereCongestionColor)
            .alternativeRouteUnknownCongestionColor(alternativeRouteUnknownCongestionColor)
            .alternativeRouteClosureColor(alternativeRouteClosureColor)
            .alternativeRouteRestrictedRoadColor(alternativeRouteRestrictedRoadColor)
            .routeLineTraveledColor(routeLineTraveledColor)
            .routeLineTraveledCasingColor(routeLineTraveledCasingColor)
            .routeCasingColor(routeCasingColor)
            .alternativeRouteCasingColor(alternativeRouteCasingColor)
            .inactiveRouteLegCasingColor(inactiveRouteLegCasingColor)
            .inActiveRouteLegsColor(inActiveRouteLegsColor)
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineColorResources(" +
            "routeDefaultColor=$routeDefaultColor, " +
            "routeLowCongestionColor=$routeLowCongestionColor, " +
            "routeModerateCongestionColor=$routeModerateCongestionColor, " +
            "routeHeavyCongestionColor=$routeHeavyCongestionColor, " +
            "routeSevereCongestionColor=$routeSevereCongestionColor, " +
            "routeUnknownCongestionColor=$routeUnknownCongestionColor, " +
            "inactiveRouteLegLowCongestionColor=$inactiveRouteLegLowCongestionColor, " +
            "inactiveRouteLegModerateCongestionColor=$inactiveRouteLegModerateCongestionColor, " +
            "inactiveRouteLegHeavyCongestionColor=$inactiveRouteLegHeavyCongestionColor, " +
            "inactiveRouteLegSevereCongestionColor=$inactiveRouteLegSevereCongestionColor, " +
            "inactiveRouteLegUnknownCongestionColor=$inactiveRouteLegUnknownCongestionColor, " +
            "routeClosureColor=$routeClosureColor, " +
            "inactiveRouteLegClosureColor=$inactiveRouteLegClosureColor, " +
            "restrictedRoadColor=$restrictedRoadColor, " +
            "inactiveRouteLegRestrictedRoadColor=$inactiveRouteLegRestrictedRoadColor, " +
            "alternativeRouteDefaultColor=$alternativeRouteDefaultColor, " +
            "alternativeRouteLowCongestionColor=$alternativeRouteLowCongestionColor, " +
            "alternativeRouteModerateCongestionColor=$alternativeRouteModerateCongestionColor, " +
            "alternativeRouteHeavyCongestionColor=$alternativeRouteHeavyCongestionColor, " +
            "alternativeRouteSevereCongestionColor=$alternativeRouteSevereCongestionColor, " +
            "alternativeRouteUnknownCongestionColor=$alternativeRouteUnknownCongestionColor, " +
            "alternativeRouteRestrictedRoadColor=$alternativeRouteRestrictedRoadColor, " +
            "alternativeRouteClosureColor=$alternativeRouteClosureColor, " +
            "routeLineTraveledColor=$routeLineTraveledColor, " +
            "routeLineTraveledCasingColor=$routeLineTraveledCasingColor, " +
            "routeCasingColor=$routeCasingColor, " +
            "alternativeRouteCasingColor=$alternativeRouteCasingColor, " +
            "inactiveRouteLegCasingColor=$inactiveRouteLegCasingColor, " +
            "inActiveRouteLegsColor=$inActiveRouteLegsColor" +
            ")"
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = routeDefaultColor
        result = 31 * result + routeLowCongestionColor
        result = 31 * result + routeModerateCongestionColor
        result = 31 * result + routeHeavyCongestionColor
        result = 31 * result + routeSevereCongestionColor
        result = 31 * result + routeUnknownCongestionColor
        result = 31 * result + routeClosureColor
        result = 31 * result + inactiveRouteLegLowCongestionColor
        result = 31 * result + inactiveRouteLegModerateCongestionColor
        result = 31 * result + inactiveRouteLegHeavyCongestionColor
        result = 31 * result + inactiveRouteLegSevereCongestionColor
        result = 31 * result + inactiveRouteLegUnknownCongestionColor
        result = 31 * result + inactiveRouteLegClosureColor
        result = 31 * result + restrictedRoadColor
        result = 31 * result + inactiveRouteLegRestrictedRoadColor
        result = 31 * result + alternativeRouteDefaultColor
        result = 31 * result + alternativeRouteLowCongestionColor
        result = 31 * result + alternativeRouteModerateCongestionColor
        result = 31 * result + alternativeRouteHeavyCongestionColor
        result = 31 * result + alternativeRouteSevereCongestionColor
        result = 31 * result + alternativeRouteUnknownCongestionColor
        result = 31 * result + alternativeRouteRestrictedRoadColor
        result = 31 * result + alternativeRouteClosureColor
        result = 31 * result + routeLineTraveledColor
        result = 31 * result + routeLineTraveledCasingColor
        result = 31 * result + routeCasingColor
        result = 31 * result + alternativeRouteCasingColor
        result = 31 * result + inactiveRouteLegCasingColor
        result = 31 * result + inActiveRouteLegsColor
        return result
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteLineColorResources

        if (routeDefaultColor != other.routeDefaultColor) return false
        if (routeLowCongestionColor != other.routeLowCongestionColor) return false
        if (routeModerateCongestionColor != other.routeModerateCongestionColor) return false
        if (routeHeavyCongestionColor != other.routeHeavyCongestionColor) return false
        if (routeSevereCongestionColor != other.routeSevereCongestionColor) return false
        if (routeUnknownCongestionColor != other.routeUnknownCongestionColor) return false
        if (routeClosureColor != other.routeClosureColor) return false
        if (inactiveRouteLegLowCongestionColor != other.inactiveRouteLegLowCongestionColor) {
            return false
        }
        if (inactiveRouteLegModerateCongestionColor
            != other.inactiveRouteLegModerateCongestionColor
        ) {
            return false
        }
        if (inactiveRouteLegHeavyCongestionColor != other.inactiveRouteLegHeavyCongestionColor) {
            return false
        }
        if (inactiveRouteLegSevereCongestionColor != other.inactiveRouteLegSevereCongestionColor) {
            return false
        }
        if (inactiveRouteLegUnknownCongestionColor
            != other.inactiveRouteLegUnknownCongestionColor
        ) {
            return false
        }
        if (inactiveRouteLegClosureColor != other.inactiveRouteLegClosureColor) return false
        if (restrictedRoadColor != other.restrictedRoadColor) return false
        if (inactiveRouteLegRestrictedRoadColor != other.inactiveRouteLegRestrictedRoadColor) {
            return false
        }
        if (alternativeRouteDefaultColor != other.alternativeRouteDefaultColor) return false
        if (alternativeRouteLowCongestionColor != other.alternativeRouteLowCongestionColor) {
            return false
        }
        if (
            alternativeRouteModerateCongestionColor != other.alternativeRouteModerateCongestionColor
        ) {
            return false
        }
        if (alternativeRouteHeavyCongestionColor != other.alternativeRouteHeavyCongestionColor) {
            return false
        }
        if (alternativeRouteSevereCongestionColor != other.alternativeRouteSevereCongestionColor) {
            return false
        }
        if (
            alternativeRouteUnknownCongestionColor != other.alternativeRouteUnknownCongestionColor
        ) {
            return false
        }
        if (alternativeRouteRestrictedRoadColor != other.alternativeRouteRestrictedRoadColor) {
            return false
        }
        if (alternativeRouteClosureColor != other.alternativeRouteClosureColor) return false
        if (routeLineTraveledColor != other.routeLineTraveledColor) return false
        if (routeLineTraveledCasingColor != other.routeLineTraveledCasingColor) return false
        if (routeCasingColor != other.routeCasingColor) return false
        if (inActiveRouteLegsColor != other.inActiveRouteLegsColor) return false
        if (alternativeRouteCasingColor != other.alternativeRouteCasingColor) return false
        if (inactiveRouteLegCasingColor != other.inactiveRouteLegCasingColor) return false

        return true
    }

    /**
     * A builder for instantiating the RouteLineResources class
     */
    class Builder {
        private var routeDefaultColor: Int = RouteLayerConstants.ROUTE_DEFAULT_COLOR
        private var routeLowCongestionColor: Int = RouteLayerConstants.ROUTE_LOW_TRAFFIC_COLOR
        private var routeModerateCongestionColor: Int =
            RouteLayerConstants.ROUTE_MODERATE_TRAFFIC_COLOR
        private var routeHeavyCongestionColor: Int = RouteLayerConstants.ROUTE_HEAVY_TRAFFIC_COLOR
        private var routeSevereCongestionColor: Int = RouteLayerConstants.ROUTE_SEVERE_TRAFFIC_COLOR
        private var routeUnknownCongestionColor: Int =
            RouteLayerConstants.ROUTE_UNKNOWN_TRAFFIC_COLOR
        private var restrictedRoadColor: Int = RouteLayerConstants.RESTRICTED_ROAD_COLOR
        private var routeClosureColor: Int = RouteLayerConstants.ROUTE_CLOSURE_COLOR
        private var inactiveRouteLegLowCongestionColor: Int =
            RouteLayerConstants.ROUTE_LEG_INACTIVE_LOW_TRAFFIC_COLOR
        private var inactiveRouteLegModerateCongestionColor: Int =
            RouteLayerConstants.ROUTE_LEG_INACTIVE_MODERATE_TRAFFIC_COLOR
        private var inactiveRouteLegHeavyCongestionColor: Int =
            RouteLayerConstants.ROUTE_LEG_INACTIVE_HEAVY_TRAFFIC_COLOR
        private var inactiveRouteLegSevereCongestionColor: Int =
            RouteLayerConstants.ROUTE_LEG_INACTIVE_SEVERE_TRAFFIC_COLOR
        private var inactiveRouteLegUnknownCongestionColor: Int =
            RouteLayerConstants.ROUTE_LEG_INACTIVE_UNKNOWN_TRAFFIC_COLOR
        private var inactiveRouteLegRestrictedRoadColor: Int =
            RouteLayerConstants.ROUTE_LEG_INACTIVE_RESTRICTED_ROAD_COLOR
        private var inactiveRouteLegClosureColor: Int =
            RouteLayerConstants.ROUTE_LEG_INACTIVE_CLOSURE_COLOR
        private var alternativeRouteDefaultColor: Int =
            RouteLayerConstants.ALTERNATE_ROUTE_DEFAULT_COLOR
        private var alternativeRouteLowCongestionColor: Int =
            RouteLayerConstants.ALTERNATE_ROUTE_LOW_TRAFFIC_COLOR
        private var alternativeRouteModerateCongestionColor: Int =
            RouteLayerConstants.ALTERNATE_ROUTE_MODERATE_TRAFFIC_COLOR
        private var alternativeRouteHeavyCongestionColor: Int =
            RouteLayerConstants.ALTERNATE_ROUTE_HEAVY_TRAFFIC_COLOR
        private var alternativeRouteSevereCongestionColor: Int =
            RouteLayerConstants.ALTERNATE_ROUTE_SEVERE_TRAFFIC_COLOR
        private var alternativeRouteUnknownCongestionColor: Int =
            RouteLayerConstants.ALTERNATE_ROUTE_UNKNOWN_TRAFFIC_COLOR
        private var alternativeRouteRestrictedRoadColor: Int =
            RouteLayerConstants.ALTERNATE_RESTRICTED_ROAD_COLOR
        private var alternativeRouteClosureColor: Int =
            RouteLayerConstants.ALTERNATIVE_ROUTE_CLOSURE_COLOR
        private var routeLineTraveledColor: Int = RouteLayerConstants.ROUTE_LINE_TRAVELED_COLOR
        private var routeLineTraveledCasingColor: Int =
            RouteLayerConstants.ROUTE_LINE_TRAVELED_CASING_COLOR
        private var routeCasingColor: Int = RouteLayerConstants.ROUTE_CASING_COLOR
        private var alternativeRouteCasingColor: Int =
            RouteLayerConstants.ALTERNATE_ROUTE_CASING_COLOR
        private var inactiveRouteLegCasingColor: Int =
            RouteLayerConstants.INACTIVE_ROUTE_LEG_CASING_COLOR
        private var inActiveRouteLegsColor: Int =
            RouteLayerConstants.IN_ACTIVE_ROUTE_LEG_COLOR

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
        fun routeModerateCongestionColor(@ColorInt color: Int): Builder =
            apply { this.routeModerateCongestionColor = color }

        /**
         * The color used for representing heavy traffic congestion.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeHeavyCongestionColor(@ColorInt color: Int): Builder =
            apply { this.routeHeavyCongestionColor = color }

        /**
         * The color used for representing severe traffic congestion.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeSevereCongestionColor(@ColorInt color: Int): Builder =
            apply { this.routeSevereCongestionColor = color }

        /**
         * The color used for representing unknown traffic congestion.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeUnknownCongestionColor(@ColorInt color: Int): Builder =
            apply { this.routeUnknownCongestionColor = color }

        /**
         * The color used for the restricted road representation.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun restrictedRoadColor(@ColorInt color: Int): Builder =
            apply { this.restrictedRoadColor = color }

        /**
         * The color used for road closure sections of a route.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun routeClosureColor(@ColorInt color: Int): Builder =
            apply { this.routeClosureColor = color }

        /**
         * The color used for representing low traffic congestion on inactive legs of the route.
         *
         * Defaults to transparent. Also see [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently].
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun inactiveRouteLegLowCongestionColor(@ColorInt color: Int): Builder =
            apply { this.inactiveRouteLegLowCongestionColor = color }

        /**
         * The color used for representing moderate traffic congestion on inactive legs of the route.
         *
         * Defaults to transparent. Also see [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently].
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun inactiveRouteLegModerateCongestionColor(@ColorInt color: Int): Builder =
            apply { this.inactiveRouteLegModerateCongestionColor = color }

        /**
         * The color used for representing heavy traffic congestion on inactive legs of the route.
         *
         * Defaults to transparent. Also see [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently].
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun inactiveRouteLegHeavyCongestionColor(@ColorInt color: Int): Builder =
            apply { this.inactiveRouteLegHeavyCongestionColor = color }

        /**
         * The color used for representing severe traffic congestion on inactive legs of the route.
         *
         * Defaults to transparent. Also see [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently].
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun inactiveRouteLegSevereCongestionColor(@ColorInt color: Int): Builder =
            apply { this.inactiveRouteLegSevereCongestionColor = color }

        /**
         * The color used for representing unknown traffic congestion on inactive legs of the route.
         *
         * Defaults to transparent. Also see [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently].
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun inactiveRouteLegUnknownCongestionColor(@ColorInt color: Int): Builder =
            apply { this.inactiveRouteLegUnknownCongestionColor = color }

        /**
         * The color used for the restricted road representation on inactive legs of the route.
         *
         * Defaults to transparent. Also see [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently].
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun inactiveRouteLegRestrictedRoadColor(@ColorInt color: Int): Builder =
            apply { this.inactiveRouteLegRestrictedRoadColor = color }

        /**
         * The color used for road closure sections of a route on inactive legs of the route.
         *
         * Defaults to transparent. Also see [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently].
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun inactiveRouteLegClosureColor(@ColorInt color: Int): Builder =
            apply { this.inactiveRouteLegClosureColor = color }

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
        fun alternativeRouteLowCongestionColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteLowCongestionColor = color }

        /**
         * The color used for representing moderate traffic congestion on alternative routes.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteModerateCongestionColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteModerateCongestionColor = color }

        /**
         * The color used for representing heavy traffic congestion on alternative routes.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteHeavyCongestionColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteHeavyCongestionColor = color }

        /**
         * The color used for representing severe traffic congestion on alternative routes.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteSevereCongestionColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteSevereCongestionColor = color }

        /**
         * The color used for representing unknown traffic congestion on alternative routes.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteUnknownCongestionColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteUnknownCongestionColor = color }

        /**
         * The color used for the alternative route restricted road representation.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteRestrictedRoadColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteRestrictedRoadColor = color }

        /**
         * The color used for road closure sections of an alternative route(s).
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun alternativeRouteClosureColor(@ColorInt color: Int): Builder =
            apply { this.alternativeRouteClosureColor = color }

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
         * The color used for casing of route legs that aren't currently being navigated.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun inactiveRouteLegCasingColor(@ColorInt color: Int): Builder =
            apply { this.inactiveRouteLegCasingColor = color }

        /**
         * The color used for route legs that aren't currently being navigated.
         *
         * @param color the color to be used
         *
         * @return the builder
         */
        fun inActiveRouteLegsColor(@ColorInt color: Int): Builder =
            apply { this.inActiveRouteLegsColor = color }

        /**
         * Creates a instance of RouteLineResources
         *
         * @return the instance
         */
        fun build(): RouteLineColorResources {
            return RouteLineColorResources(
                routeDefaultColor = routeDefaultColor,
                routeLowCongestionColor = routeLowCongestionColor,
                routeModerateCongestionColor = routeModerateCongestionColor,
                routeHeavyCongestionColor = routeHeavyCongestionColor,
                routeSevereCongestionColor = routeSevereCongestionColor,
                routeUnknownCongestionColor = routeUnknownCongestionColor,
                inactiveRouteLegLowCongestionColor = inactiveRouteLegLowCongestionColor,
                inactiveRouteLegModerateCongestionColor = inactiveRouteLegModerateCongestionColor,
                inactiveRouteLegHeavyCongestionColor = inactiveRouteLegHeavyCongestionColor,
                inactiveRouteLegSevereCongestionColor = inactiveRouteLegSevereCongestionColor,
                inactiveRouteLegUnknownCongestionColor = inactiveRouteLegUnknownCongestionColor,
                alternativeRouteDefaultColor = alternativeRouteDefaultColor,
                alternativeRouteLowCongestionColor = alternativeRouteLowCongestionColor,
                alternativeRouteModerateCongestionColor = alternativeRouteModerateCongestionColor,
                alternativeRouteHeavyCongestionColor = alternativeRouteHeavyCongestionColor,
                alternativeRouteSevereCongestionColor = alternativeRouteSevereCongestionColor,
                alternativeRouteUnknownCongestionColor = alternativeRouteUnknownCongestionColor,
                restrictedRoadColor = restrictedRoadColor,
                routeClosureColor = routeClosureColor,
                inactiveRouteLegRestrictedRoadColor = inactiveRouteLegRestrictedRoadColor,
                inactiveRouteLegClosureColor = inactiveRouteLegClosureColor,
                alternativeRouteRestrictedRoadColor = alternativeRouteRestrictedRoadColor,
                alternativeRouteClosureColor = alternativeRouteClosureColor,
                routeLineTraveledColor = routeLineTraveledColor,
                routeLineTraveledCasingColor = routeLineTraveledCasingColor,
                routeCasingColor = routeCasingColor,
                alternativeRouteCasingColor = alternativeRouteCasingColor,
                inactiveRouteLegCasingColor = inactiveRouteLegCasingColor,
                inActiveRouteLegsColor = inActiveRouteLegsColor,
            )
        }
    }
}
