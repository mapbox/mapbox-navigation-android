package com.mapbox.navigation.core.replay.route

import android.annotation.SuppressLint
import android.content.Context
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.internal.logW
import java.util.Collections

/**
 * Used to create a replay trip session. Continue to use [MapboxNavigation.setNavigationRoutes] to
 * decide the route that should be replayed.
 *
 * Do not use this class with the [ReplayProgressObserver]. They will create conflicts with and
 * the results are undefined.
 *
 * The simulated driver from [ReplayRouteSession] will slow down to a stop depending
 * [ReplayRouteSessionOptions.decodeMinDistance]. To remove this behavior you can set it to
 * [Double.MAX_VALUE], be aware that it will require more memory.
 *
 * Use [ReplayRouteSessionOptions] for customizations. For example, this is how can update the
 * location frequency.
 *
 * ```
 * replayRouteSession.setOptions(
 *     replayRouteSession.getOptions().toBuilder()
 *         .replayRouteOptions(
 *             replayRouteSession.getOptions().replayRouteOptions.toBuilder()
 *                 .frequency(25.0)
 *                 .build()
 *         )
 *         .build()
 * )
 * ```
 *
 * Enable and disable the [ReplayRouteSession] with [MapboxNavigation] or [MapboxNavigationApp].
 * The replay session will be enabled when [MapboxNavigation] is attached.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ReplayRouteSession : MapboxNavigationObserver {

    private var options = ReplayRouteSessionOptions.Builder().build()

    private lateinit var replayRouteMapper: ReplayRouteMapper
    private var mapboxNavigation: MapboxNavigation? = null
    private var lastLocationEvent: ReplayEventUpdateLocation? = null
    private var polylineDecodeStream: ReplayPolylineDecodeStream? = null
    private var currentRoute: NavigationRoute? = null

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        if (currentRoute?.id != routeProgress.navigationRoute.id) {
            currentRoute = routeProgress.navigationRoute
            onRouteChanged(routeProgress.navigationRoute, routeProgress.currentRouteGeometryIndex)
        }
    }

    private val routesObserver = RoutesObserver { result ->
        if (result.navigationRoutes.isEmpty()) {
            mapboxNavigation?.resetReplayLocation()
            currentRoute = null
            polylineDecodeStream = null
        } else if (mapboxNavigation?.mapboxReplayer?.isPlaying() != true) {
            // In order to get route progress updates, we need location updates.
            // If we don't have any location updates, we don't get route progress updates
            // and we'll never start navigating the route.
            // If we have location updates, we'll update the route from RouteProgressObserver,
            // because it has more information, e. g. current route geometry index.
            currentRoute = result.navigationRoutes.first()
            onRouteChanged(result.navigationRoutes.first(), 0)
        }
    }

    private val replayEventsObserver = ReplayEventsObserver { events ->
        if (currentRoute != null && isLastEventPlayed(events)) {
            pushMorePoints()
        }
    }

    /**
     * Get the options that are currently set. This can be used to change the options.
     * ```
     * setOptions(getOptions().toBuilder().locationResetEnabled(false).build())
     * ```
     */
    fun getOptions(): ReplayRouteSessionOptions = options

    /**
     * Set new options for the [ReplayRouteSession]. This will not effect previously simulated
     * events, the end behavior will depend on the values you have used. If you want to guarantee
     * the effect of the options, you need to set options before [MapboxNavigation] is attached.
     */
    fun setOptions(options: ReplayRouteSessionOptions) = apply {
        this.options = options
        if (::replayRouteMapper.isInitialized) {
            replayRouteMapper.options = this.options.replayRouteOptions
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        this.replayRouteMapper = ReplayRouteMapper(options.replayRouteOptions)
        this.mapboxNavigation = mapboxNavigation
        mapboxNavigation.startReplayTripSession()
        mapboxNavigation.resetReplayLocation()
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.mapboxReplayer.registerObserver(replayEventsObserver)
        mapboxNavigation.mapboxReplayer.play()
    }

    private fun MapboxNavigation.resetReplayLocation() {
        mapboxReplayer.clearEvents()
        resetTripSession {
            if (options.locationResetEnabled) {
                val context = navigationOptions.applicationContext
                if (PermissionsManager.areLocationPermissionsGranted(context)) {
                    pushRealLocation(context)
                } else {
                    logW(LOG_CATEGORY) {
                        "Location permissions have not been accepted. If this is intentional, " +
                            "disable this warning with " +
                            "ReplayRouteSessionOptions.locationResetEnabled."
                    }
                }
            }
            mapboxReplayer.play()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.mapboxReplayer.unregisterObserver(replayEventsObserver)
        mapboxNavigation.mapboxReplayer.stop()
        mapboxNavigation.mapboxReplayer.clearEvents()
        this.mapboxNavigation = null
        this.currentRoute = null
    }

    private fun onRouteChanged(navigationRoute: NavigationRoute, currentIndex: Int) {
        val mapboxReplayer = mapboxNavigation?.mapboxReplayer ?: return
        mapboxReplayer.clearEvents()
        mapboxReplayer.play()
        val geometries = navigationRoute.directionsRoute.routeOptions()!!.geometries()
        val usesPolyline6 = geometries.contains(DirectionsCriteria.GEOMETRY_POLYLINE6)
        val geometry = navigationRoute.directionsRoute.geometry()
        if (!usesPolyline6 || geometry.isNullOrEmpty()) {
            logW(LOG_CATEGORY) {
                "The NavigationRouteReplay must have geometry encoded with polyline6 " +
                    "$geometries $geometry"
            }
            return
        }
        polylineDecodeStream = ReplayPolylineDecodeStream(geometry, 6)

        // Skip up to the current geometry index. There is some imprecision here because the
        // distance traveled is not equal to a route index.
        polylineDecodeStream?.skip(currentIndex)

        pushMorePoints()
    }

    private fun isLastEventPlayed(events: List<ReplayEventBase>): Boolean {
        val currentLocationEvent = events.lastOrNull { it is ReplayEventUpdateLocation }
            ?: return false
        val lastEventTimestamp = this.lastLocationEvent?.eventTimestamp ?: 0.0
        return currentLocationEvent.eventTimestamp >= lastEventTimestamp
    }

    private fun pushMorePoints() {
        val nextPoints = polylineDecodeStream?.decode(options.decodeMinDistance) ?: return
        val nextReplayLocations = replayRouteMapper.mapPointList(nextPoints)
        lastLocationEvent = nextReplayLocations.lastOrNull { it is ReplayEventUpdateLocation }
            as? ReplayEventUpdateLocation
        mapboxNavigation?.mapboxReplayer?.clearPlayedEvents()
        mapboxNavigation?.mapboxReplayer?.pushEvents(nextReplayLocations)
    }

    /**
     * This function is similar to [MapboxReplayer.pushRealLocation] except that it checks if there
     * is an active route before it tries to push a gps location. This is needed to avoid a race
     * condition between setting routes and requesting a location.
     */
    @SuppressLint("MissingPermission")
    private fun pushRealLocation(context: Context) {
        LocationEngineProvider.getBestLocationEngine(context.applicationContext)
            .getLastLocation(
                object : LocationEngineCallback<LocationEngineResult> {
                    override fun onSuccess(result: LocationEngineResult?) {
                        if (mapboxNavigation?.getNavigationRoutes()?.isNotEmpty() == true) {
                            return
                        }
                        result?.lastLocation?.let {
                            val event = ReplayRouteMapper.mapToUpdateLocation(0.0, it)
                            mapboxNavigation?.mapboxReplayer?.pushEvents(
                                Collections.singletonList(event)
                            )
                        }
                    }

                    override fun onFailure(exception: Exception) {
                        // Intentionally empty
                    }
                }
            )
    }

    private companion object {
        private const val LOG_CATEGORY = "MapboxReplayRouteTripSession"
    }
}
