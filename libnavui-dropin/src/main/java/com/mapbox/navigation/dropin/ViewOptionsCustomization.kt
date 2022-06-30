package com.mapbox.navigation.dropin

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

/**
 * A class that allows you to define values for various different properties used by the
 * [NavigationView]. If not specified, [NavigationView] uses the default values defined for
 * each of these properties.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ViewOptionsCustomization {
    /**
     * Provide custom navigation style for day mode.
     * Use [NavigationStyles.NAVIGATION_DAY_STYLE] to reset to default.
     */
    var mapStyleUriDay: String? = null

    /**
     * Provide custom navigation style for night mode.
     * Use [NavigationStyles.NAVIGATION_NIGHT_STYLE] to reset to default.
     */
    var mapStyleUriNight: String? = null

    /**
     * Provide custom route line options.
     * Use [ViewOptionsCustomization.defaultRouteLineOptions] to reset to default.
     */
    var routeLineOptions: MapboxRouteLineOptions? = null

    /**
     * Provide custom route arrow options.
     * Use [ViewOptionsCustomization.defaultRouteArrowOptions] to reset to default.
     */
    var routeArrowOptions: RouteArrowOptions? = null

    /**
     * Set if Info Panel should hide for the Free Drive state.
     */
    var showInfoPanelInFreeDrive: Boolean? = null

    companion object {
        /**
         * Default route line options.
         */
        fun defaultRouteLineOptions(context: Context) = MapboxRouteLineOptions.Builder(context)
            .withRouteLineResources(RouteLineResources.Builder().build())
            .withRouteLineBelowLayerId("road-label-navigation")
            .build()

        /**
         * Default route arrow options.
         */
        fun defaultRouteArrowOptions(context: Context) = RouteArrowOptions.Builder(context)
            .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .build()
    }
}
