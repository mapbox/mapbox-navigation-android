package com.mapbox.navigation.ui.maps.route.line.model

import android.util.Log
import androidx.annotation.ColorInt
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants

/**
 * Contains colors an other values used to determine the appearance of the route line.
 *
 * @param lowCongestionRange the range for low congestion traffic.
 * @param moderateCongestionRange the range for low congestion traffic.
 * @param heavyCongestionRange the range for low congestion traffic.
 * @param severeCongestionRange the range for low congestion traffic.
 * @param routeDefaultColor the default color of the route line
 * @param routeLowCongestionColor the color used for representing low traffic congestion
 * @param routeModerateCongestionColor the color used for representing moderate traffic congestion
 * @param routeHeavyCongestionColor the color used for representing heavy traffic congestion
 * @param routeSevereCongestionColor the color used for representing severe traffic congestion
 * @param routeUnknownCongestionColor the color used for representing unknown traffic congestion
 * @param routeClosureColor the color used for the route closure line
 * @param restrictedRoadColor the color for the restricted road indicator(s)
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
 * @param inActiveRouteLegsColor the color used for route legs that aren't currently
 * being navigated.
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
 */
class RouteLineColorResources private constructor(
    val lowCongestionRange: IntRange,
    val moderateCongestionRange: IntRange,
    val heavyCongestionRange: IntRange,
    val severeCongestionRange: IntRange,
    @ColorInt val routeDefaultColor: Int,
    @ColorInt val routeLowCongestionColor: Int,
    @ColorInt val routeModerateCongestionColor: Int,
    @ColorInt val routeHeavyCongestionColor: Int,
    @ColorInt val routeSevereCongestionColor: Int,
    @ColorInt val routeUnknownCongestionColor: Int,
    @ColorInt val alternativeRouteDefaultColor: Int,
    @ColorInt val alternativeRouteLowCongestionColor: Int,
    @ColorInt val alternativeRouteModerateCongestionColor: Int,
    @ColorInt val alternativeRouteHeavyCongestionColor: Int,
    @ColorInt val alternativeRouteSevereCongestionColor: Int,
    @ColorInt val alternativeRouteUnknownCongestionColor: Int,
    @ColorInt val restrictedRoadColor: Int,
    @ColorInt val routeClosureColor: Int,
    @ColorInt val alternativeRouteRestrictedRoadColor: Int,
    @ColorInt val alternativeRouteClosureColor: Int,
    @ColorInt val routeLineTraveledColor: Int,
    @ColorInt val routeLineTraveledCasingColor: Int,
    @ColorInt val routeCasingColor: Int,
    @ColorInt val alternativeRouteCasingColor: Int,
    @ColorInt val inActiveRouteLegsColor: Int
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .lowCongestionRange(lowCongestionRange)
            .moderateCongestionRange(moderateCongestionRange)
            .heavyCongestionRange(heavyCongestionRange)
            .severeCongestionRange(severeCongestionRange)
            .routeDefaultColor(routeDefaultColor)
            .routeLowCongestionColor(routeLowCongestionColor)
            .routeModerateCongestionColor(routeModerateCongestionColor)
            .routeHeavyCongestionColor(routeHeavyCongestionColor)
            .routeSevereCongestionColor(routeSevereCongestionColor)
            .routeUnknownCongestionColor(routeUnknownCongestionColor)
            .routeClosureColor(routeClosureColor)
            .restrictedRoadColor(restrictedRoadColor)
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
            .inActiveRouteLegsColor(inActiveRouteLegsColor)
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteLineColorResources(" +
            "lowCongestionRange=$lowCongestionRange, " +
            "heavyCongestionRange=$heavyCongestionRange, " +
            "severeCongestionRange=$severeCongestionRange, " +
            "moderateCongestionRange=$moderateCongestionRange, " +
            "routeDefaultColor=$routeDefaultColor, " +
            "routeLowCongestionColor=$routeLowCongestionColor, " +
            "routeModerateCongestionColor=$routeModerateCongestionColor, " +
            "routeHeavyCongestionColor=$routeHeavyCongestionColor, " +
            "routeSevereCongestionColor=$routeSevereCongestionColor, " +
            "routeUnknownCongestionColor=$routeUnknownCongestionColor, " +
            "routeClosureColor=$routeClosureColor, " +
            "restrictedRoadColor=$restrictedRoadColor, " +
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
            "inActiveRouteLegsColor=$inActiveRouteLegsColor" +
            ")"
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = lowCongestionRange.hashCode()
        result = 31 * result + heavyCongestionRange.hashCode()
        result = 31 * result + severeCongestionRange.hashCode()
        result = 31 * result + moderateCongestionRange.hashCode()
        result = 31 * result + routeDefaultColor
        result = 31 * result + routeLowCongestionColor
        result = 31 * result + routeModerateCongestionColor
        result = 31 * result + routeHeavyCongestionColor
        result = 31 * result + routeSevereCongestionColor
        result = 31 * result + routeUnknownCongestionColor
        result = 31 * result + routeClosureColor
        result = 31 * result + restrictedRoadColor
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

        if (lowCongestionRange != other.lowCongestionRange) return false
        if (heavyCongestionRange != other.heavyCongestionRange) return false
        if (severeCongestionRange != other.severeCongestionRange) return false
        if (moderateCongestionRange != other.moderateCongestionRange) return false
        if (routeDefaultColor != other.routeDefaultColor) return false
        if (routeLowCongestionColor != other.routeLowCongestionColor) return false
        if (routeModerateCongestionColor != other.routeModerateCongestionColor) return false
        if (routeHeavyCongestionColor != other.routeHeavyCongestionColor) return false
        if (routeSevereCongestionColor != other.routeSevereCongestionColor) return false
        if (routeUnknownCongestionColor != other.routeUnknownCongestionColor) return false
        if (routeClosureColor != other.routeClosureColor) return false
        if (restrictedRoadColor != other.restrictedRoadColor) return false
        if (alternativeRouteDefaultColor != other.alternativeRouteDefaultColor) return false
        if (alternativeRouteLowCongestionColor != other.alternativeRouteLowCongestionColor)
            return false
        if (
            alternativeRouteModerateCongestionColor != other.alternativeRouteModerateCongestionColor
        ) return false
        if (alternativeRouteHeavyCongestionColor != other.alternativeRouteHeavyCongestionColor)
            return false
        if (alternativeRouteSevereCongestionColor != other.alternativeRouteSevereCongestionColor)
            return false
        if (alternativeRouteUnknownCongestionColor != other.alternativeRouteUnknownCongestionColor)
            return false
        if (alternativeRouteRestrictedRoadColor != other.alternativeRouteRestrictedRoadColor)
            return false
        if (alternativeRouteClosureColor != other.alternativeRouteClosureColor) return false
        if (routeLineTraveledColor != other.routeLineTraveledColor) return false
        if (routeLineTraveledCasingColor != other.routeLineTraveledCasingColor) return false
        if (routeCasingColor != other.routeCasingColor) return false
        if (inActiveRouteLegsColor != other.inActiveRouteLegsColor) return false
        if (alternativeRouteCasingColor != other.alternativeRouteCasingColor) return false

        return true
    }

    /**
     * A builder for instantiating the RouteLineResources class
     */
    class Builder {
        private var lowCongestionRange: IntRange = RouteLayerConstants.LOW_CONGESTION_RANGE
        private var moderateCongestionRange: IntRange =
            RouteLayerConstants.MODERATE_CONGESTION_RANGE
        private var heavyCongestionRange: IntRange = RouteLayerConstants.HEAVY_CONGESTION_RANGE
        private var severeCongestionRange: IntRange = RouteLayerConstants.SEVERE_CONGESTION_RANGE
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
        private var inActiveRouteLegsColor: Int =
            RouteLayerConstants.IN_ACTIVE_ROUTE_LEG_COLOR

        /**
         * The default range for low traffic congestion
         *
         * @param range the range to be used
         *
         * @return the builder
         * @throws IllegalArgumentException if the range includes values less than 0 or greater than 100
         */
        fun lowCongestionRange(range: IntRange): Builder {
            if (!rangeInBounds(range)) {
                throw IllegalArgumentException(
                    "Ranges containing values less than 0 or greater than 100 are invalid."
                )
            } else {
                this.lowCongestionRange = range
            }
            return this
        }

        /**
         * The default range for moderate traffic congestion
         *
         * @param range the range to be used
         *
         * @return the builder
         * @throws IllegalArgumentException if the range includes values less than 0 or greater than 100
         */
        fun moderateCongestionRange(range: IntRange): Builder {
            if (!rangeInBounds(range)) {
                throw IllegalArgumentException(
                    "Ranges containing values less than 0 or greater than 100 are invalid."
                )
            } else {
                this.moderateCongestionRange = range
            }
            return this
        }

        /**
         * The default range for heavy traffic congestion
         *
         * @param range the range to be used
         *
         * @return the builder
         * @throws IllegalArgumentException if the range includes values less than 0 or greater than 100
         */
        fun heavyCongestionRange(range: IntRange): Builder {
            if (!rangeInBounds(range)) {
                throw IllegalArgumentException(
                    "Ranges containing values less than 0 or greater than 100 are invalid."
                )
            } else {
                this.heavyCongestionRange = range
            }
            return this
        }

        /**
         * The default range for severe traffic congestion
         *
         * @param range the range to be used
         *
         * @return the builder
         * @throws IllegalArgumentException if the range includes values less than 0 or greater than 100
         */
        fun severeCongestionRange(range: IntRange): Builder {
            if (!rangeInBounds(range)) {
                throw IllegalArgumentException(
                    "Ranges containing values less than 0 or greater than 100 are invalid."
                )
            } else {
                this.severeCongestionRange = range
            }
            return this
        }

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
            if (rangesOverlap()) {
                throw IllegalStateException(
                    "Traffic congestion ranges should not overlap each other."
                )
            }

            return RouteLineColorResources(
                lowCongestionRange,
                moderateCongestionRange,
                heavyCongestionRange,
                severeCongestionRange,
                routeDefaultColor,
                routeLowCongestionColor,
                routeModerateCongestionColor,
                routeHeavyCongestionColor,
                routeSevereCongestionColor,
                routeUnknownCongestionColor,
                alternativeRouteDefaultColor,
                alternativeRouteLowCongestionColor,
                alternativeRouteModerateCongestionColor,
                alternativeRouteHeavyCongestionColor,
                alternativeRouteSevereCongestionColor,
                alternativeRouteUnknownCongestionColor,
                restrictedRoadColor,
                routeClosureColor,
                alternativeRouteRestrictedRoadColor,
                alternativeRouteClosureColor,
                routeLineTraveledColor,
                routeLineTraveledCasingColor,
                routeCasingColor,
                alternativeRouteCasingColor,
                inActiveRouteLegsColor
            )
        }

        private fun rangeInBounds(range: IntRange): Boolean {
            return range.first >= 0 && range.last <= 100
        }

        private fun rangesOverlap(): Boolean {
            val logTag = "Mbx${RouteLineColorResources::class.java.canonicalName}"
            return when {
                lowCongestionRange.intersect(moderateCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Low and moderate ranges are overlapping.")
                    true
                }
                lowCongestionRange.intersect(heavyCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Low and moderate ranges are overlapping.")
                    true
                }
                lowCongestionRange.intersect(severeCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Low and severe ranges are overlapping.")
                    true
                }

                moderateCongestionRange.intersect(heavyCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Moderate and heavy ranges are overlapping.")
                    true
                }

                moderateCongestionRange.intersect(severeCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Moderate and severe ranges are overlapping.")
                    true
                }

                heavyCongestionRange.intersect(severeCongestionRange).isNotEmpty() -> {
                    Log.e(logTag, "Heavy and severe ranges are overlapping.")
                    true
                }
                else -> false
            }
        }
    }
}
