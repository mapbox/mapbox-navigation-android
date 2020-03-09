package com.mapbox.navigation.core.telemetry

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.LOCATION_BUFFER_MAX_SIZE
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.TAG
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import com.mapbox.navigation.utils.time.Time
import java.util.ArrayDeque
import java.util.Collections
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

internal typealias OffRouteBuffers = Pair<List<Location>, List<Location>>

internal class TelemetryLocationAndProgressDispatcher :
        RouteProgressObserver, LocationObserver, RoutesObserver {
    private var lastLocation: AtomicReference<Location> = AtomicReference(Location("Default"))
    private var routeProgress: AtomicReference<RouteProgressWithTimestamp> =
            AtomicReference(RouteProgressWithTimestamp(0, RouteProgress.Builder().build()))
    private val channelRouteSelected = Channel<RouteAvailable>(Channel.CONFLATED)
    private val channelLocationRecieved = Channel<Location>(Channel.CONFLATED)
    private val channelOnRouteProgress =
            Channel<RouteProgressWithTimestamp>(Channel.CONFLATED) // we want just the last notification
    private var jobControl = ThreadController.getIOScopeAndRootJob()
    private val routeSelected = AtomicReference<RouteAvailable?>(null)
    private var accumulationJob: Job = Job()
    private val currentLocationBuffer = SynchronizedLocationbuffer()

    /**
     * Thi class provides thread-safe access to a mutable list of locations
     */
    private class SynchronizedLocationbuffer {
        private val synchronizedCollection: MutableList<Location> = Collections.synchronizedList(mutableListOf<Location>())

        fun addLocation(location: Location) {
            synchronized(synchronizedCollection) {
                synchronizedCollection.add(0, location)
            }
        }

        fun removeLocation() {
            synchronized(synchronizedCollection) {
                if (synchronizedCollection.isNotEmpty()) {
                    val index = synchronizedCollection.size - 1
                    synchronizedCollection.removeAt(index)
                }
            }
        }

        fun getCopy(): List<Location> {
            val result = mutableListOf<Location>()
            synchronized(synchronizedCollection) {
                result.addAll(synchronizedCollection)
            }
            return result
        }

        fun clear() {
            synchronized(synchronizedCollection) {
                synchronizedCollection.clear()
            }
        }

        fun size() = synchronizedCollection.size
    }

    init {
        jobControl.scope.monitorChannelWithException(channelLocationRecieved, { location ->
            accumulateLocationAsync(location, currentLocationBuffer)
        })
    }

    /**
     * This method accumulates locations. The number of locations is limited by [MapboxNavigationTelemetry.LOCATION_BUFFER_MAX_SIZE].
     * Once this limit is reached, an item is removed before another is added. The method returns true if the queue reaches capacity,
     * false otherwise
     */
    private fun accumulateLocationAsync(location: Location, queue: SynchronizedLocationbuffer): Boolean {
        var result = false
        when (queue.size() >= LOCATION_BUFFER_MAX_SIZE + 1) {
            true -> {
                queue.removeLocation()
                queue.addLocation(location)
                result = true
            }
            false -> {
                queue.addLocation(location)
            }
        }
        return result
    }

    /**
     * This method returns a [Pair] of buffers. The first represents a fixed number of locations before an  event of interest (offroute or user feedback),
     * while the second represents a fixed number of locations after same event
     */
    fun getLocationBuffersAsync() = accumulatePostEventLocationsAsync()

    /**
     * This method cancels all jobs that accumulate telemetry data. The side effect of this call is to call Telemetry.addEvent(), which may cause events to be sent
     * to the back-end server
     */
    fun cancelAccumulationJob() = accumulationJob.cancel()

    /**
     * This channel becomes signaled if a navigation route is selected
     */
    fun getDirectionsRouteChannel(): ReceiveChannel<RouteAvailable> = channelRouteSelected

    fun getLastDirectionsRoute() = routeSelected

    /**
     * This method populates two location buffers. One with pre-offroute events and the other with post-offroute events.
     * The buffers are sent to the caller if the job completes or is canceled. This job maybe canceled by a navigation.cancel event.
     * This code is shared between user feedback events and off-route events
     */
    private fun accumulatePostEventLocationsAsync(): Deferred<OffRouteBuffers> {
        val result = CompletableDeferred<OffRouteBuffers>()
        val preEventBuffer = mutableListOf<Location>()
        val postEventBuffer = mutableListOf<Location>()
        accumulationJob = jobControl.scope.launch {
            val preEventLocationBuffer = acquireAccumulatedLocations(true) // grab whatever is in the location buffer. This will return at least 1 location value
            postEventBuffer.addAll(preEventLocationBuffer.await()) // copy pre event locations
            currentLocationBuffer.clear()

            val postEventLocationBuffer = acquireAccumulatedLocations() // accumulate post event locations
            postEventBuffer.addAll(postEventLocationBuffer.await())
            currentLocationBuffer.clear()

            Log.d(TAG, "resetting location monitor")
        }

        jobControl.scope.launch {
            select<Unit> {
                accumulationJob.onJoin {
                    result.complete(
                            Pair(
                                preEventBuffer,
                                postEventBuffer
                            )
                    ) // notify caller the job is complete
                }
            }
        }
        return result
    }

    /**
     * This method accumulates locations. The location objects are stored in a FIFO queue.
     * Once the queue size reaches a predefined limit, it becomes signaled and the caller is
     * notified via a deferred object
     */
    private fun acquireAccumulatedLocations(getDataNow: Boolean = false): CompletableDeferred<ArrayDeque<Location>> {
        val result = CompletableDeferred<ArrayDeque<Location>>()
        jobControl.scope.launch {
            val locationQueue = ArrayDeque<Location>() // Temporary buffer to collect locations while waiting for completion
            while (currentLocationBuffer.size() <= LOCATION_BUFFER_MAX_SIZE && isActive) { // If the buffer is full, copy its contents
                locationQueue.clear()
                locationQueue.addAll(currentLocationBuffer.getCopy()) // Copy the collected data to the return buffer
                if (getDataNow && currentLocationBuffer.size() >= 1) { // The caller does not wish to wait for LOCATION_BUFFER_MAX_SIZE items before getting the data. If there is at least one item in the buffer, return it.
                    result.complete(locationQueue) // Notify
                    Log.d(TAG, "getDataNow = $getDataNow")
                    return@launch
                }
                delay(500)
            }
            result.complete(locationQueue) // Notify whomever is listening of the result
        }
        return result
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val data = RouteProgressWithTimestamp(Time.SystemImpl.millis(), routeProgress)
        this.routeProgress.set(data)
        channelOnRouteProgress.offer(data)
    }

    fun getRouteProgressChannel(): ReceiveChannel<RouteProgressWithTimestamp> =
            channelOnRouteProgress

    fun getLastLocation(): Location = lastLocation.get()

    fun getRouteProgress(): RouteProgressWithTimestamp = routeProgress.get()

    fun isRouteAvailable(): AtomicReference<RouteAvailable?> = routeSelected

    override fun onRawLocationChanged(rawLocation: Location) {
        // Do nothing
    }

    override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
        channelLocationRecieved.offer(enhancedLocation)
        lastLocation.set(enhancedLocation)
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {
        when (routes.isEmpty()) {
            true -> {
                routeSelected.set(null)
            }
            false -> {
                val date = Date()
                channelRouteSelected.offer(RouteAvailable(routes[0], date))
                routeSelected.set(RouteAvailable(routes[0], date))
            }
        }
    }
}
