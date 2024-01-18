@file:Suppress("unused")

package com.mapbox.navigation.dropin

import androidx.annotation.UiThread
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.dropin.navigationview.NavigationViewListener
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer

/**
 * Api that gives you the ability to change the state of [NavigationView].
 */
@UiThread
abstract class NavigationViewApi {

    /**
     * Clear route data and request [NavigationView] to enter free drive state.
     *
     * [NavigationViewListener.onFreeDrive] will be called once [NavigationView] enters
     * free drive state.
     */
    abstract fun startFreeDrive()

    /**
     * Sets a [point] as destination and request [NavigationView] to enter destination preview state.
     *
     * [NavigationViewListener.onDestinationChanged] will be called once new destination is set
     * and [NavigationViewListener.onDestinationPreview] once [NavigationView] enters
     * Destination Preview state.
     */
    abstract fun startDestinationPreview(point: Point)

    /**
     * Request [NavigationView] to enter route preview state.
     *
     * [NavigationViewListener.onRoutePreview] will be called once [NavigationView] enters
     * route preview state.
     *
     * Fails with an error when either destination or preview routes has not been set.
     */
    abstract fun startRoutePreview(): Expected<NavigationViewApiError, Unit>

    /**
     * Sets preview [routes] and request [NavigationView] to enter route preview state.
     * Last [DirectionsWaypoint] location will be used as the destination.
     *
     * [NavigationViewListener.onDestinationChanged] will be called once new destination is set
     * and [NavigationViewListener.onRoutePreview] will be called once [NavigationView] enters
     * route preview state.
     *
     * Fails with an error when [routes] is an empty list.
     */
    abstract fun startRoutePreview(
        routes: List<NavigationRoute>
    ): Expected<NavigationViewApiError, Unit>

    /**
     * Request [NavigationView] to enter active navigation state.
     *
     * [NavigationViewListener.onActiveNavigation] will be called once [NavigationView] enters
     * route preview state.
     *
     * Fails with an error when either destination or preview routes has not been set.
     */
    abstract fun startActiveGuidance(): Expected<NavigationViewApiError, Unit>

    /**
     * Sets [routes] and request [NavigationView] to enter active navigation state.
     * Last [DirectionsWaypoint] location will be used as the destination.
     *
     * [NavigationViewListener.onDestinationChanged] will be called once new destination is set
     * and [NavigationViewListener.onActiveNavigation] will be called once [NavigationView] enters
     * active navigation state.
     *
     * Fails with an error when [routes] is an empty list.
     */
    abstract fun startActiveGuidance(
        routes: List<NavigationRoute>
    ): Expected<NavigationViewApiError, Unit>

    /**
     * Request [NavigationView] to enter arrival state.
     *
     * [NavigationViewListener.onArrival] will be called once [NavigationView] enters
     * arrival state.
     *
     * Fails with an error when either destination or routes has not been set.
     */
    abstract fun startArrival(): Expected<NavigationViewApiError, Unit>

    /**
     * Sets [routes] and request [NavigationView] to enter arrival state.
     * Last [DirectionsWaypoint] location will be used as the destination.
     *
     * [NavigationViewListener.onDestinationChanged] will be called once new destination is set
     * and [NavigationViewListener.onArrival] will be called once [NavigationView] enters
     * arrival state.
     *
     * Fails with an error when [routes] is an empty list.
     */
    abstract fun startArrival(routes: List<NavigationRoute>): Expected<NavigationViewApiError, Unit>

    /**
     * Checks if the current trip is being simulated.
     */
    abstract fun isReplayEnabled(): Boolean

    /**
     * Enable/Disable replay trip session based on simulated locations.
     */
    abstract fun routeReplayEnabled(enabled: Boolean)

    /**
     * Access the current instance of [MapboxVoiceInstructionsPlayer] created by [NavigationView].
     */
    abstract fun getCurrentVoiceInstructionsPlayer(): MapboxVoiceInstructionsPlayer?

    /**
     * If the camera is idle or in overview mode, calling this function will dispatch an action
     * that will reposition the camera back to the user locaiton indicator (as if the recenter button was clicked).
     */
    abstract fun recenterCamera()
}
