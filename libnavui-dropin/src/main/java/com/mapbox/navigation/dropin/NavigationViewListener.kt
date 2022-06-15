package com.mapbox.navigation.dropin

import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

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
    open fun onFreeDriveStarted() = Unit

    /**
     * Called when NavigationView enters Destination Preview state.
     */
    open fun onDestinationPreviewStared() = Unit

    /**
     * Called when NavigationView enters Route Preview state.
     */
    open fun onRoutePreviewStared() = Unit

    /**
     * Called when NavigationView enters Active Navigation state.
     */
    open fun onActiveNavigationStared() = Unit

    /**
     * Called when NavigationView enters Arrival state.
     */
    open fun onArrivalStared() = Unit

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
}
