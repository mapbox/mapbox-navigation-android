package com.mapbox.navigation.core.replay.route

import android.app.PendingIntent
import android.location.Location
import android.os.Handler
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import java.util.ArrayList

/**
 * Mock [LocationEngine] for replaying route movement through [DirectionsRoute]
 *
 * @param logger [Logger](optional)
 * @param converter ReplayLocationConverter
 */
class ReplayRouteLocationEngine(
    val logger: Logger? = null,
    private val converter: ReplayLocationConverter
) : LocationEngine, Runnable {

    private var speed = DEFAULT_SPEED
    private var delay = DEFAULT_DELAY
    private val handler: Handler = Handler()
    private lateinit var mockedLocations: MutableList<Location>
    private lateinit var dispatcher: ReplayLocationDispatcher
    private lateinit var replayLocationListener: ReplayRouteLocationListener
    private var lastLocation = Location(REPLAY_ROUTE)
    private var route: DirectionsRoute? = null
    private var point: Point? = null

    /**
     * @param [Logger](optional)
     */
    constructor(logger: Logger? = null) : this(
        logger,
        ReplayRouteLocationConverter(
            DEFAULT_SPEED,
            DEFAULT_DELAY
        )
    )

    companion object {
        private const val HEAD = 0
        private const val MOCKED_POINTS_LEFT_THRESHOLD = 5
        private const val ONE_SECOND_IN_MILLISECONDS = 1000
        private const val FORTY_FIVE_KM_PER_HOUR = 45
        private const val DEFAULT_SPEED = FORTY_FIVE_KM_PER_HOUR
        private const val ONE_SECOND = 1
        private const val DEFAULT_DELAY = ONE_SECOND
        private const val DO_NOT_DELAY = 0
        private const val ZERO = 0
        private const val SPEED_MUST_BE_GREATER_THAN_ZERO_KM_H =
            "Speed must be greater than 0 km/h."
        private const val DELAY_MUST_BE_GREATER_THAN_ZERO_SECONDS =
            "Delay must be greater than 0 seconds."
        private const val REPLAY_ROUTE =
            "com.mapbox.navigation.core.replay.route.ReplayRouteLocationEngine"
    }

    /**
     * Assign a new route. If [moveTo] was called before, stay invalid
     */
    fun assign(route: DirectionsRoute) {
        this.route = route
        this.point = null
    }

    /**
     * Move current location to [Point]
     */
    fun moveTo(point: Point) {
        this.point = point
        this.route = null
    }

    /**
     * Entry point of route or set location to custom map location
     */
    fun assignLastLocation(currentPosition: Point) {
        lastLocation.longitude = currentPosition.longitude()
        lastLocation.latitude = currentPosition.latitude()
    }

    /**
     * @param customSpeedInKmPerHour speed km/h
     */
    fun updateSpeed(customSpeedInKmPerHour: Int) {
        require(customSpeedInKmPerHour > 0) { SPEED_MUST_BE_GREATER_THAN_ZERO_KM_H }
        this.speed = customSpeedInKmPerHour
    }

    /**
     * Delay between each [Point]
     *
     * @param customDelayInSeconds delay in seconds, must be > 0
     */
    fun updateDelay(customDelayInSeconds: Int) {
        require(customDelayInSeconds > 0) { DELAY_MUST_BE_GREATER_THAN_ZERO_SECONDS }
        this.delay = customDelayInSeconds
    }

    /**
     * Start mocked movement
     */
    override fun run() {
        var nextMockedLocations = converter.toLocations()
        if (nextMockedLocations.isEmpty()) {
            if (converter.isMultiLegRoute) {
                nextMockedLocations = converter.toLocations()
            } else {
                handler.removeCallbacks(this)
                return
            }
        }
        dispatcher.add(nextMockedLocations)
        mockedLocations.addAll(nextMockedLocations)
        scheduleNextDispatch()
    }

    /**
     * Returns the most recent location currently available.
     *
     * If a location is not available, which should happen very rarely, null will be returned.
     */
    @Throws(SecurityException::class)
    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        callback.onSuccess(LocationEngineResult.create(lastLocation))
    }

    /**
     * Requests location updates with a callback on the specified Looper thread.
     */
    @Throws(SecurityException::class)
    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?
    ) {
        beginReplayWith(callback)
    }

    /**
     * Requests location updates with callback on the specified PendingIntent.
     */
    @Throws(SecurityException::class)
    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        pendingIntent: PendingIntent
    ) {
        logger?.e(msg = Message("ReplayEngine does not support PendingIntent."))
    }
    /**
     * Removes location updates for the given location engine callback.
     *
     * It is recommended to remove location requests when the activity is in a paused or
     * stopped state, doing so helps battery performance.
     *
     */
    override fun removeLocationUpdates(callback: LocationEngineCallback<LocationEngineResult>) {
        deactivate()
    }

    /**
     * Removes location updates for the given pending intent.
     *
     * It is recommended to remove location requests when the activity is in a paused or
     * stopped state, doing so helps battery performance.
     */
    override fun removeLocationUpdates(pendingIntent: PendingIntent) {
        logger?.e(msg = Message("ReplayEngine does not support PendingIntent."))
    }

    internal fun updateLastLocation(lastLocation: Location) {
        this.lastLocation = lastLocation
    }

    internal fun removeLastMockedLocation() {
        if (mockedLocations.isNotEmpty()) {
            mockedLocations.removeAt(HEAD)
        }
    }

    private fun deactivate() {
        if (::dispatcher.isInitialized) {
            dispatcher.stop()
        }
        handler.removeCallbacks(this)
    }

    private fun start(
        route: DirectionsRoute,
        callback: LocationEngineCallback<LocationEngineResult>
    ) {
        handler.removeCallbacks(this)
        converter.setRoute(route)
        converter.initializeTime()
        mockedLocations = converter.toLocations().toMutableList()
        dispatcher = obtainDispatcher(callback)
        dispatcher.run()
        scheduleNextDispatch()
    }

    private fun obtainDispatcher(callback: LocationEngineCallback<LocationEngineResult>): ReplayLocationDispatcher {
        if (::dispatcher.isInitialized && ::replayLocationListener.isInitialized) {
            dispatcher.stop()
            dispatcher.removeReplayLocationListener(replayLocationListener)
        }
        dispatcher =
            ReplayLocationDispatcher(
                mockedLocations
            )
        replayLocationListener =
            ReplayRouteLocationListener(
                this,
                callback
            )
        dispatcher.addReplayLocationListener(replayLocationListener)

        return dispatcher
    }

    private fun startRoute(
        point: Point,
        lastLocation: Location,
        callback: LocationEngineCallback<LocationEngineResult>
    ) {
        handler.removeCallbacks(this)
        converter.updateSpeed(speed)
        converter.updateDelay(delay)
        converter.initializeTime()
        val route = obtainRoute(point, lastLocation)
        mockedLocations =
            converter.calculateMockLocations(converter.sliceRoute(route)).toMutableList()
        dispatcher = obtainDispatcher(callback)
        dispatcher.run()
    }

    private fun obtainRoute(point: Point, lastLocation: Location): LineString {
        val pointList = ArrayList<Point>()
        pointList.add(Point.fromLngLat(lastLocation.longitude, lastLocation.latitude))
        pointList.add(point)
        return LineString.fromLngLats(pointList)
    }

    private fun scheduleNextDispatch() {
        val currentMockedPoints = mockedLocations.size
        when {
            currentMockedPoints == ZERO -> handler.postDelayed(this, DO_NOT_DELAY.toLong())
            currentMockedPoints <= MOCKED_POINTS_LEFT_THRESHOLD -> handler.postDelayed(
                this,
                ONE_SECOND_IN_MILLISECONDS.toLong()
            )
            else -> handler.postDelayed(
                this,
                ((currentMockedPoints - MOCKED_POINTS_LEFT_THRESHOLD) * ONE_SECOND_IN_MILLISECONDS).toLong()
            )
        }
    }

    private fun beginReplayWith(callback: LocationEngineCallback<LocationEngineResult>) {
        route?.let {
            start(it, callback)
        } ?: point?.let {
            startRoute(it, lastLocation, callback)
        } ?: callback.onFailure(Exception("No route found to replay."))
    }
}
