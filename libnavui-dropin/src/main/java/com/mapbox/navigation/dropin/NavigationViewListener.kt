package com.mapbox.navigation.dropin

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin

/**
 * Interface definition for the NavigationView listener.
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class NavigationViewListener {
    /**
     * Called when destination has changed.
     *
     * @param destination destination Point or `null` when destination is cleared.
     */
    open fun onDestinationChanged(destination: Point?) = Unit

    /**
     * Called when NavigationView enters Free Drive state.
     */
    open fun onFreeDrive() = Unit

    /**
     * Called when NavigationView enters Destination Preview state.
     */
    open fun onDestinationPreview() = Unit

    /**
     * Called when NavigationView enters Route Preview state.
     */
    open fun onRoutePreview() = Unit

    /**
     * Called when NavigationView enters Active Navigation state.
     */
    open fun onActiveNavigation() = Unit

    /**
     * Called when NavigationView enters Arrival state.
     */
    open fun onArrival() = Unit

    /**
     * Called when Map [Style] has changed. Invoked once the new style has been fully loaded,
     * including the style specified sprite and sources.
     *
     * @param style Fully loaded style.
     */
    open fun onMapStyleChanged(style: Style) = Unit

    /**
     * Called when camera mode has changed to Idle.
     */
    open fun onIdleCameraMode() = Unit

    /**
     * Called when camera mode has changed to Overview.
     */
    open fun onOverviewCameraMode() = Unit

    /**
     * Called when camera mode has changed to Following.
     */
    open fun onFollowingCameraMode() = Unit

    /**
     * Called when `NavigationCamera` padding has changed. The padding is defined as a box relative to map size which the user location indicator and route geometry is fit into during camera following and overview states.
     *
     * @param padding New camera padding value.
     */
    open fun onCameraPaddingChanged(padding: EdgeInsets) = Unit

    /**
     * Called when Audio Guidance mute state has change.
     *
     * @param muted Audio Guidance muted state.
     */
    open fun onAudioGuidanceStateChanged(muted: Boolean) = Unit

    /**
     * Called when route request using the [Router] was successful. The result contains a list of
     * [NavigationRoute]. If there are no routes, then the list will be empty.
     *
     * @param routes All routes from origin to destination with waypoints if available.
     */
    open fun onRouteFetchSuccessful(routes: List<NavigationRoute>) = Unit

    /**
     * Called when route request using the [Router] was canceled.
     *
     * @param routeOptions Used to fetch the route.
     * @param routerOrigin Origin of the route request
     */
    open fun onRouteFetchCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) = Unit

    /**
     * Called when route request using the [Router] failed.
     *
     * @param reasons For why the request failed.
     * @param routeOptions Used to fetch the route.
     */
    open fun onRouteFetchFailed(reasons: List<RouterFailure>, routeOptions: RouteOptions) = Unit

    /**
     * Called when the route is being fetched using the [Router].
     *
     * @param requestId Id of the route requested.
     */
    open fun onRouteFetching(requestId: Long) = Unit
}
