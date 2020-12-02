package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.navigation.ui.maps.route.line.api.RouteLineResourceProvider

internal class MapboxRouteLineResourceProvider(
    private val routeLineTraveledColor: Int,
    private val routeLineTraveledCasingColor: Int,
    private val routeUnknownTrafficColor: Int,
    private val routeDefaultColor: Int,
    private val routeLowCongestionColor: Int,
    private val routeModerateColor: Int,
    private val routeHeavyColor: Int,
    private val routeSevereColor: Int,
    private val routeCasingColor: Int,
    private val roundedLineCap: Boolean,
    private val alternativeRouteUnknownColor: Int,
    private val alternativeRouteDefaultColor: Int,
    private val alternativeRouteLowColor: Int,
    private val alternativeRouteModerateColor: Int,
    private val alternativeRouteHeavyColor: Int,
    private val alternativeRouteSevereColor: Int,
    private val alternativeRouteCasingColor: Int,
    private val originWaypointIcon: Int,
    private val destinationWaypointIcon: Int,
    private val trafficBackfillRoadClasses: List<String>
) : RouteLineResourceProvider {
    override fun getRouteLineTraveledColor(): Int = routeLineTraveledColor

    override fun getRouteLineTraveledCasingColor(): Int = routeLineTraveledCasingColor

    override fun getRouteLineBaseColor(): Int = routeDefaultColor

    override fun getRouteLineCasingColor(): Int = routeCasingColor

    override fun getAlternativeRouteLineBaseColor(): Int = alternativeRouteDefaultColor

    override fun getAlternativeRouteLineCasingColor(): Int = alternativeRouteCasingColor

    override fun getRouteUnknownTrafficColor(): Int = routeUnknownTrafficColor

    override fun getRouteLowTrafficColor(): Int = routeLowCongestionColor

    override fun getRouteModerateTrafficColor(): Int = routeModerateColor

    override fun getRouteHeavyTrafficColor(): Int = routeHeavyColor

    override fun getRouteSevereTrafficColor(): Int = routeSevereColor

    override fun getAlternativeRouteUnknownTrafficColor(): Int = alternativeRouteUnknownColor

    override fun getAlternativeRouteLowTrafficColor(): Int = alternativeRouteLowColor

    override fun getAlternativeRouteModerateTrafficColor(): Int = alternativeRouteModerateColor

    override fun getAlternativeRouteHeavyTrafficColor(): Int = alternativeRouteHeavyColor

    override fun getAlternativeRouteSevereTrafficColor(): Int = alternativeRouteSevereColor

    override fun getUseRoundedLineCap(): Boolean = roundedLineCap

    override fun getOriginWaypointIcon(): Int = originWaypointIcon

    override fun getDestinationWaypointIcon(): Int = destinationWaypointIcon

    override fun getTrafficBackfillRoadClasses(): List<String> = trafficBackfillRoadClasses
}
