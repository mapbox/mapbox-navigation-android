package com.mapbox.navigation.core.replay.route

import android.annotation.SuppressLint
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToPoints
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.replay.history.ReplayEventsObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.utils.nearestPointOnGeometryCheapRuler
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc
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

    /**
     * Speed and position of the last location the replayer actually played. When the route
     * changes mid-drive, the replay of the new route continues from here rather than restarting
     * from a standstill at the nearest geometry vertex.
     */
    private var currentSpeedMps = 0.0
    private var lastPlayedPoint: Point? = null

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
            // Nothing is playing, so whatever was played before says nothing about where the
            // driver is or how fast it is going.
            currentSpeedMps = 0.0
            lastPlayedPoint = null
            onRouteChanged(result.navigationRoutes.first(), 0)
        }
    }

    private val replayEventsObserver = ReplayEventsObserver { events ->
        val playedLocation = events.lastOrNull { it is ReplayEventUpdateLocation }
            as? ReplayEventUpdateLocation
        playedLocation?.location?.let { location ->
            location.speed?.let { currentSpeedMps = it }
            lastPlayedPoint = Point.fromLngLat(location.lon, location.lat)
        }
        if (currentRoute != null && isLastEventPlayed(playedLocation)) {
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
    fun setOptions(options: ReplayRouteSessionOptions): ReplayRouteSession = apply {
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
        currentSpeedMps = 0.0
        lastPlayedPoint = null
        mapboxReplayer.clearEvents()
        resetTripSession {
            if (options.locationResetEnabled) {
                val context = navigationOptions.applicationContext
                if (PermissionsManager.areLocationPermissionsGranted(context)) {
                    pushRealLocation()
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
        this.currentSpeedMps = 0.0
        this.lastPlayedPoint = null
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
        val stream = ReplayPolylineDecodeStream(geometry, 6)
        polylineDecodeStream = stream

        val driverPoint = lastPlayedPoint
        val nextVertexIndex = driverPoint?.let { nextVertexIndexOnRoute(it, navigationRoute) }
        if (nextVertexIndex != null) {
            // The driver is already on the new route, so carry on from where it is, at the speed
            // it is doing, towards the first vertex ahead of it.
            stream.skip(nextVertexIndex)
            pushMorePoints(startPoint = driverPoint)
        } else {
            // Skip up to the current geometry index. There is some imprecision here because the
            // distance traveled is not equal to a route index.
            stream.skip(currentIndex)
            currentSpeedMps = 0.0
            pushMorePoints()
        }
    }

    /**
     * Index of the first geometry vertex of [navigationRoute] ahead of [driverPoint], found by
     * projecting the driver onto the route line. The driver's position is trusted over the route
     * geometry index reported with the route change, which can lag behind the driver and would
     * otherwise move it backwards.
     *
     * @return null when the driver is not on the route, in which case the replay has to restart
     *  at a geometry vertex instead of continuing from the driver.
     */
    private fun nextVertexIndexOnRoute(driverPoint: Point, navigationRoute: NavigationRoute): Int? {
        val route = navigationRoute.directionsRoute
        val points = route.completeGeometryToPoints()
        if (points.size < 2) return null

        val (segmentIndex, distanceToRouteMeters) =
            nearestPointOnGeometryCheapRuler(route, driverPoint, 0, points.lastIndex)
                ?.let { it.geometryIndex to it.distanceToRouteMeters }
                ?: TurfMisc.nearestPointOnLine(driverPoint, points, TurfConstants.UNIT_METERS)
                    .let {
                        it.getNumberProperty(NEAREST_SEGMENT_INDEX_KEY).toInt() to
                            it.getNumberProperty(NEAREST_DISTANCE_KEY).toDouble()
                    }
        if (distanceToRouteMeters > MAX_RESUME_DISTANCE_METERS) return null
        // The index names the segment the nearest point lies on, so its end is the vertex ahead.
        return segmentIndex + 1
    }

    private fun isLastEventPlayed(currentLocationEvent: ReplayEventUpdateLocation?): Boolean {
        if (currentLocationEvent == null) return false
        val lastEventTimestamp = this.lastLocationEvent?.eventTimestamp ?: 0.0
        return currentLocationEvent.eventTimestamp >= lastEventTimestamp
    }

    /**
     * Decodes and pushes the next batch of the route. The batch is driven from the decoded
     * geometry at [currentSpeedMps], or from [startPoint] first when the replay resumes from a
     * position between two geometry vertices.
     */
    private fun pushMorePoints(startPoint: Point? = null) {
        val nextPoints = polylineDecodeStream?.decode(options.decodeMinDistance) ?: return
        val points = if (startPoint != null) listOf(startPoint) + nextPoints else nextPoints
        val nextReplayLocations = replayRouteMapper.mapPointList(points, currentSpeedMps)
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
    private fun pushRealLocation() {
        LocationServiceFactory.getOrCreate().getDeviceLocationProvider(null).onValue { provider ->
            provider.getLastLocation { location ->
                if (location != null && mapboxNavigation?.getNavigationRoutes().isNullOrEmpty()) {
                    val event = ReplayRouteMapper.mapToUpdateLocation(0.0, location)
                    mapboxNavigation?.mapboxReplayer?.pushEvents(
                        Collections.singletonList(event),
                    )
                }
            }
        }
    }

    private companion object {
        private const val LOG_CATEGORY = "MapboxReplayRouteTripSession"

        /**
         * How far the driver may be from the new route's line and still count as being on it.
         * Replayed locations sit on the geometry they were interpolated from, so anything beyond
         * this is a genuine relocation, such as a route set from somewhere else entirely.
         */
        private const val MAX_RESUME_DISTANCE_METERS = 50.0

        /** Property names of the feature returned by [TurfMisc.nearestPointOnLine]. */
        private const val NEAREST_DISTANCE_KEY = "dist"
        private const val NEAREST_SEGMENT_INDEX_KEY = "index"
    }
}
