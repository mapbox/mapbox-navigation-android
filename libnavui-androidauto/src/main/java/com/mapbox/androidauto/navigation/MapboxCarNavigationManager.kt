package com.mapbox.androidauto.navigation

import androidx.car.app.CarContext
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Trip
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.internal.logAndroidAutoFailure
import com.mapbox.androidauto.navigation.maneuver.CarManeuverMapper
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.androidauto.telemetry.MapboxCarTelemetry
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Register this observer using [MapboxNavigationApp.registerObserver]. As long as it is
 * registered, the trip status of [MapboxNavigation] will be sent to the [NavigationManager].
 * This is needed to keep the vehicle cluster display updated.
 */
class MapboxCarNavigationManager internal constructor(
    carContext: CarContext
) : MapboxNavigationObserver {

    private val navigationManager: NavigationManager by lazy {
        carContext.getCarService(NavigationManager::class.java)
    }

    private var maneuverApi: MapboxManeuverApi? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private val carTelemetry = MapboxCarTelemetry()

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        val maneuverApi = maneuverApi ?: return@RouteProgressObserver
        val trip = CarManeuverMapper.from(routeProgress, maneuverApi)
        onUpdateTrip(trip)
    }

    private var inActiveNavigation = false

    private val routesObserver = RoutesObserver {
        if (it.navigationRoutes.isEmpty()) {
            if (inActiveNavigation) {
                logAndroidAuto("$LOG_CATEGORY stop active navigation")
                inActiveNavigation = false
                navigationManager.navigationEnded()
            }
        } else if (!inActiveNavigation) {
            logAndroidAuto("$LOG_CATEGORY start active navigation")
            inActiveNavigation = true
            navigationManager.navigationStarted()
        }
    }

    private val navigationManagerCallback = object : NavigationManagerCallback {
        override fun onStopNavigation() {
            logAndroidAuto("$LOG_CATEGORY onStopNavigation")
            super.onStopNavigation()
            mapboxNavigation?.setNavigationRoutes(emptyList())
            MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE)
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
        logAndroidAuto("$LOG_CATEGORY onAttached")
        this.mapboxNavigation = mapboxNavigation
        carTelemetry.onAttached(mapboxNavigation)
        val distanceFormatter = MapboxDistanceFormatter(
            mapboxNavigation.navigationOptions.distanceFormatterOptions
        )
        maneuverApi = MapboxManeuverApi(distanceFormatter)
        navigationManager.setNavigationManagerCallback(navigationManagerCallback)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        logAndroidAuto("$LOG_CATEGORY onDetached")
        this.mapboxNavigation = null
        carTelemetry.onDetached(mapboxNavigation)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)

        // Tell android auto navigation stopped. Navigation can continue with mapbox navigation
        // on another device.
        navigationManager.navigationEnded()
        navigationManager.clearNavigationManagerCallback()
    }

    private fun onUpdateTrip(trip: Trip) {
        if (inActiveNavigation) {
            // There is no way to know if NavigationManager isNavigating and it will crash if false
            // https://issuetracker.google.com/u/0/issues/260968395
            try { navigationManager.updateTrip(trip) } catch (e: IllegalStateException) {
                logAndroidAutoFailure("$LOG_CATEGORY updateTrip failed", e)
                if (e.message == UPDATE_TRIP_NAVIGATION_NOT_STARTED) {
                    logAndroidAuto(
                        "$LOG_CATEGORY calling NavigationManager.navigationStarted(). Use " +
                            "MapboxNavigation.stopTripSession in order to stop navigation."
                    )
                    navigationManager.navigationStarted()
                    navigationManager.updateTrip(trip)
                }
            }
        }
    }

    private companion object {
        private const val LOG_CATEGORY = "MapboxCarNavigationManager"
        private const val UPDATE_TRIP_NAVIGATION_NOT_STARTED = "Navigation is not started"
    }
}
