package com.mapbox.navigation.dropin

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.dropin.infopanel.InfoPanelBinder
import com.mapbox.navigation.ui.base.lifecycle.Binder
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
     * Set if the Info Panel should be visible for the Free Drive state. When set to `true`
     * [InfoPanelBinder] will only render an empty info panel. You are responsible to inflate
     * appropriate [Binder] to render the content you would like to show on the info panel during
     * free drive.
     * Set to `false` for the default behavior.
     */
    var showInfoPanelInFreeDrive: Boolean? = null

    /**
     * Set to false if you don't want [NavigationView] to intercept [OnMapLongClickListener] events.
     * Set to `true` as the default behavior.
     */
    var enableMapLongClickIntercept: Boolean? = null

    /**
     * Sets whether the Info Panel can hide when it is swiped down.
     * Set to `false` for the default behavior.
     */
    var isInfoPanelHideable: Boolean? = null

    /**
     * Set to override Info Panel BottomSheet state.
     * Setting this value to one of [BottomSheetBehavior.STATE_COLLAPSED], [BottomSheetBehavior.STATE_EXPANDED],
     * [BottomSheetBehavior.STATE_HIDDEN] or [BottomSheetBehavior.STATE_HALF_EXPANDED] will disable
     * default (auto-hiding) behaviour.
     * Set to `0` to restore the default behavior.
     */
    var infoPanelForcedState: Int? = null

    /**
     * Set to override [DistanceFormatterOptions]
     */
    var distanceFormatterOptions: DistanceFormatterOptions? = null

    /**
     * Sets whether the camera debug info should be visible.
     * Set to `false` for the default behavior.
     */
    var showCameraDebugInfo: Boolean? = null

    /**
     * Sets whether the maneuver view should be visible.
     * Set to `true` for the default behavior.
     */
    var showManeuver: Boolean? = null

    /**
     * Sets whether the speed limit view should be visible.
     * Set to `true` for the default behavior.
     */
    var showSpeedLimit: Boolean? = null

    /**
     * Sets whether the road name view should be visible.
     * Set to `true` for the default behavior.
     */
    var showRoadName: Boolean? = null

    /**
     * Sets whether action buttons should be visible.
     * Set to `true` for the default behavior.
     */
    var showActionButtons: Boolean? = null

    /**
     * Sets whether the compass action button should be visible.
     * Set to `false` for the default behavior.
     */
    var showCompassActionButton: Boolean? = null

    /**
     * Sets whether the camera mode button should be visible.
     * Set to `true` for the default behavior.
     */
    var showCameraModeActionButton: Boolean? = null

    /**
     * Sets whether the toggle audio button should be visible.
     * Set to `true` for the default behavior.
     */
    var showToggleAudioActionButton: Boolean? = null

    /**
     * Sets whether the recenter camera button should be visible.
     * Set to `true` for the default behavior.
     */
    var showRecenterActionButton: Boolean? = null

    /**
     * Sets whether the trip progress view should be visible.
     * Set to `true` for the default behavior.
     */
    var showTripProgress: Boolean? = null

    /**
     * Sets whether the map scalebar should be visible.
     * Set to `false` for the default behavior.
     */
    var showMapScalebar: Boolean? = null

    /**
     * Sets whether the route preview button should be visible.
     * Set to `true` for the default behavior.
     */
    var showRoutePreviewButton: Boolean? = null

    /**
     * Sets whether the end navigation button should be visible.
     * Set to `true` for the default behavior.
     */
    var showEndNavigationButton: Boolean? = null

    /**
     * Sets whether the start navigation button should be visible.
     * Set to `true` for the default behavior.
     */
    var showStartNavigationButton: Boolean? = null

    companion object {
        /**
         * Default route line options.
         */
        fun defaultRouteLineOptions(context: Context) = MapboxRouteLineOptions.Builder(context)
            .withRouteLineResources(RouteLineResources.Builder().build())
            .withRouteLineBelowLayerId("road-label-navigation")
            .withVanishingRouteLineEnabled(true)
            .build()

        /**
         * Default route arrow options.
         */
        fun defaultRouteArrowOptions(context: Context) = RouteArrowOptions.Builder(context)
            .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
            .build()
    }
}
