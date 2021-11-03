package com.mapbox.androidauto

import androidx.car.app.CarContext
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import com.mapbox.androidauto.car.navigation.maneuver.CarManeuverMapper
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Register this observer using [MapboxNavigationApp.registerObserver]. As long as it is
 * registered, the trip status of [MapboxNavigation] will be sent to the [NavigationManager].
 * This is needed to keep the vehicle cluster display updated.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxCarNavigationManager(
    carContext: CarContext
) : MapboxNavigationObserver {

    private val navigationManager: NavigationManager by lazy {
        carContext.getCarService(NavigationManager::class.java)
    }

    private var maneuverApi: MapboxManeuverApi? = null
    private var mapboxNavigation: MapboxNavigation? = null

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        ifNonNull(maneuverApi) {
            val trip = CarManeuverMapper.from(routeProgress, it)
            navigationManager.updateTrip(trip)
        }
    }

    private val tripSessionStateObserver = TripSessionStateObserver { tripSessionState ->
        logAndroidAuto("MapboxNavigationManager tripSessionStateObserver state: $tripSessionState")
        when (tripSessionState) {
            TripSessionState.STARTED -> navigationManager.navigationStarted()
            TripSessionState.STOPPED -> navigationManager.navigationEnded()
        }
    }

    private val navigationManagerCallback = object : NavigationManagerCallback {
        override fun onStopNavigation() {
            logAndroidAuto("MapboxNavigationManager onStopNavigation")
            super.onStopNavigation()
            mapboxNavigation?.stopTripSession()
        }

        override fun onAutoDriveEnabled() {
            _autoDriveEnabled.value = true
        }
    }

    private val _autoDriveEnabled = MutableStateFlow(false)

    /**
     * Observe the onAutoDriveEnabled callback state. This is false by default, and at any point
     * can become true. The state does not go back to false.
     */
    val autoDriveEnabledFlow: StateFlow<Boolean> = _autoDriveEnabled

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        logAndroidAuto("MapboxNavigationManager onAttached")
        this.mapboxNavigation = mapboxNavigation
        val distanceFormatter = MapboxDistanceFormatter(
            mapboxNavigation.navigationOptions.distanceFormatterOptions
        )
        maneuverApi = MapboxManeuverApi(distanceFormatter)
        navigationManager.setNavigationManagerCallback(navigationManagerCallback)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        logAndroidAuto("MapboxNavigationManager onDetached")
        this.mapboxNavigation = null
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)

        // clearNavigationManagerCallback() can't be called during active navigation.
        // However, this callback lets AA stop the trip session which we don't want it to do if
        // the user simply exited AA but is still navigating via the phone app. Since
        // there is only one instance of MapboxNavigation allowing AA to stop the trip
        // session when the MainCarSession is inactive but possibly the phone app. is
        // actively navigating would cause unpredictable side effects.
        navigationManager.navigationEnded()
        navigationManager.clearNavigationManagerCallback()
    }
}
