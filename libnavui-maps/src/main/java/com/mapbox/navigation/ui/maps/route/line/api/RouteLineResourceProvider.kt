package com.mapbox.navigation.ui.maps.route.line.api

/**
 *
 */
interface RouteLineResourceProvider {

    /**
     *
     */
    fun getRouteLineTraveledColor(): Int

    /**
     *
     */
    fun getRouteLineTraveledCasingColor(): Int

    /**
     *
     */
    fun getRouteLineBaseColor(): Int

    /**
     *
     */
    fun getRouteLineCasingColor(): Int

    /**
     *
     */
    fun getAlternativeRouteLineBaseColor(): Int

    /**
     *
     */
    fun getAlternativeRouteLineCasingColor(): Int

    /**
     *
     */
    fun getRouteUnknownTrafficColor(): Int

    /**
     *
     */
    fun getRouteLowTrafficColor(): Int

    /**
     *
     */
    fun getRouteModerateTrafficColor(): Int

    /**
     *
     */
    fun getRouteHeavyTrafficColor(): Int

    /**
     *
     */
    fun getRouteSevereTrafficColor(): Int

    /**
     *
     */
    fun getAlternativeRouteUnknownTrafficColor(): Int

    /**
     *
     */
    fun getAlternativeRouteLowTrafficColor(): Int

    /**
     *
     */
    fun getAlternativeRouteModerateTrafficColor(): Int

    /**
     *
     */
    fun getAlternativeRouteHeavyTrafficColor(): Int

    /**
     *
     */
    fun getAlternativeRouteSevereTrafficColor(): Int

    /**
     *
     */
    fun getUseRoundedLineCap(): Boolean

    /**
     *
     */
    fun getOriginWaypointIcon(): Int

    /**
     *
     */
    fun getDestinationWaypointIcon(): Int

    /**
     *
     */
    fun getTrafficBackfillRoadClasses(): List<String>
}
