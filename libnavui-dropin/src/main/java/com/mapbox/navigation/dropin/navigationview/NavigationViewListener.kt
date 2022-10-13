package com.mapbox.navigation.dropin.navigationview

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
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

    /**
     * Called when the info panel behavior updates to hidden.
     */
    open fun onInfoPanelHidden() = Unit

    /**
     * Called when the info panel behavior updates to expanded.
     */
    open fun onInfoPanelExpanded() = Unit

    /**
     * Called when the info panel behavior updates to collapsed.
     */
    open fun onInfoPanelCollapsed() = Unit

    /**
     * Called when the info panel behavior updates to dragging.
     */
    open fun onInfoPanelDragging() = Unit

    /**
     * Called when the info panel behavior updates to settling.
     */
    open fun onInfoPanelSettling() = Unit

    /**
     * Called when the info panel is being dragged.
     *
     * @param slideOffset The new offset of info panel within [-1,1] range. Offset increases
     *  as info panel is moving upward and decreases in the opposite direction.
     *  Range 0 to 1: Denotes the panel is transitioning from collapsed to expanded state.
     *  Range -1 to 0: Denotes the panel is transitioning from hidden to collapsed state.
     */
    open fun onInfoPanelSlide(slideOffset: Float) = Unit

    /**
     * Called when maneuver view has been expanded.
     */
    open fun onManeuverExpanded() = Unit

    /**
     * Called when maneuver view has been collapsed.
     */
    open fun onManeuverCollapsed() = Unit

    /**
     * Called when a map was clicked, but the event was not handled by NavigationView.
     *
     * @param point The projected map coordinate the user clicked on.
     */
    open fun onMapClicked(point: Point) = Unit
}
