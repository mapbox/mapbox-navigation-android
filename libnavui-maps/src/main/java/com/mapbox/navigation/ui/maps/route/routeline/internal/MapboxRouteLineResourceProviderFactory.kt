package com.mapbox.navigation.ui.maps.route.routeline.internal

import android.content.Context
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineResourceProvider
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getBooleanStyledValue
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getResourceStyledValue
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getStyledColor
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getStyledStringArray

object MapboxRouteLineResourceProviderFactory {

    @JvmStatic
    fun getRouteLineResourceProvider(context: Context, styleRes: Int): RouteLineResourceProvider {
        val routeLineTraveledColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeLineTraveledColor,
                R.color.mapbox_navigation_route_line_traveled_color,
                context,
                styleRes
            )
        val routeLineTraveledCasingColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeLineCasingTraveledColor,
                R.color.mapbox_navigation_route_casing_line_traveled_color,
                context,
                styleRes
            )
        val routeUnknownTrafficColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeUnknownCongestionColor,
                R.color.mapbox_navigation_route_layer_congestion_unknown,
                context,
                styleRes
            )
        val routeDefaultColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeColor,
                R.color.mapbox_navigation_route_layer_blue,
                context,
                styleRes
            )
        val routeLowCongestionColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeLowCongestionColor,
                R.color.mapbox_navigation_route_traffic_layer_color,
                context,
                styleRes
            )
        val routeModerateColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeModerateCongestionColor,
                R.color.mapbox_navigation_route_layer_congestion_yellow,
                context,
                styleRes
            )
        val routeHeavyColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeHeavyCongestionColor,
                R.color.mapbox_navigation_route_layer_congestion_heavy,
                context,
                styleRes
            )
        val routeSevereColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeSevereCongestionColor,
                R.color.mapbox_navigation_route_layer_congestion_red,
                context,
                styleRes
            )
        val routeCasingColor: Int =getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_routeCasingColor,
                R.color.mapbox_navigation_route_casing_layer_color,
                context,
                styleRes
            )
        val roundedLineCap: Boolean = getBooleanStyledValue(
                R.styleable.MapboxStyleNavigationMapRoute_roundedLineCap,
                true,
                context,
                styleRes
            )
        val alternativeRouteUnknownColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteUnknownCongestionColor,
                R.color.mapbox_navigation_route_alternative_congestion_unknown,
                context,
                styleRes
            )
        val alternativeRouteDefaultColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteColor,
                R.color.mapbox_navigation_route_alternative_color,
                context,
                styleRes
            )
        val alternativeRouteLowColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteLowCongestionColor,
                R.color.mapbox_navigation_route_alternative_color,
                context,
                styleRes
            )
        val alternativeRouteModerateColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteSevereCongestionColor,
                R.color.mapbox_navigation_route_alternative_congestion_red,
                context,
                styleRes
            )
        val alternativeRouteHeavyColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteHeavyCongestionColor,
                R.color.mapbox_navigation_route_alternative_congestion_heavy,
                context,
                styleRes
            )
        val alternativeRouteSevereColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteSevereCongestionColor,
                R.color.mapbox_navigation_route_alternative_congestion_red,
                context,
                styleRes
            )
        val alternativeRouteCasingColor: Int = getStyledColor(
                R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteCasingColor,
                R.color.mapbox_navigation_route_alternative_casing_color,
                context,
                styleRes
            )

        val originWaypointIcon: Int = getResourceStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_originWaypointIcon,
            R.drawable.mapbox_ic_route_origin,
            context,
            styleRes
        )
        val destinationWaypointIcon = getResourceStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_destinationWaypointIcon,
            R.drawable.mapbox_ic_route_destination,
            context,
            styleRes
        )

        val trafficBackfillRoadClasses: List<String> = getStyledStringArray(
                R.styleable.MapboxStyleNavigationMapRoute_trafficBackFillRoadClasses,
                context,
                styleRes,
                R.styleable.MapboxStyleNavigationMapRoute
            )

        return MapboxRouteLineResourceProvider(
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
            alternativeRouteUnknownColor,
            alternativeRouteDefaultColor,
            alternativeRouteLowColor,
            alternativeRouteModerateColor,
            alternativeRouteHeavyColor,
            alternativeRouteSevereColor,
            alternativeRouteCasingColor,
            originWaypointIcon,
            destinationWaypointIcon,
            trafficBackfillRoadClasses
        )
    }
}
