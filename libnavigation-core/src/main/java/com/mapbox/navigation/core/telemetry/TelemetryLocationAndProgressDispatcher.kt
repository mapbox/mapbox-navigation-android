package com.mapbox.navigation.core.telemetry

import android.location.Location
import android.util.Log
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.MAX_TIME_LOCATION_COLLECTION
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import com.mapbox.navigation.utils.time.Time
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

internal typealias OffRouteBuffers = Pair<List<Location>, List<Location>>

internal class TelemetryLocationAndProgressDispatcher :
        RouteProgressObserver, LocationEngineCallback<LocationEngineResult> {
    private var lastLocation: AtomicReference<Location> = AtomicReference<Location>(Location("Default"))
    private var firstLocation: AtomicReference<Location> = AtomicReference<Location>(Location("Default"))
    private var routeProgress: AtomicReference<RouteProgressWithTimeStamp> = AtomicReference(RouteProgressWithTimeStamp(0, RouteProgress.Builder().build()))
    private val channelOnRouteProgress = Channel<RouteProgressWithTimeStamp>(Channel.CONFLATED) // we want just the last notification
    private val offRouteLocationsBeforeOffroute = ArrayDeque<Location>()
    private val offRouteLocationsAfterOffroute = ArrayDeque<Location>()
    private var offRouteLocationQueue = AtomicReference<ArrayDeque<Location>>(offRouteLocationsBeforeOffroute)
    private var channelLocation = Channel<Location>(Channel.CONFLATED)
    private var channelLastNSecondsOfLocations = Channel<Location>(Channel.CONFLATED)
    private var jobControl = ThreadController.getIOScopeAndRootJob()
    private var monitorJob: Job = Job()
    private var channelBufferReady: ReceiveChannel<List<Location>>

    init {
        channelBufferReady = monitorLastDeltaLocations()
        monitorJob = monitorLocationChannel()
    }

    /**
     * This method accumulates locations. The number of locations is limited by [MapboxNavigationTelemetry.LOCATION_BUFFER_MAX_SIZE].
     * Once this limit is reached, an item is removed before another is added. The method returns [true] if the queue reaches capacity,
     * [false] otherwise
     */
    private fun accumulateLocationAsync(location: Location, queue: ArrayDeque<Location>): Boolean {
        var result = false
        when (queue.count() >= MapboxNavigationTelemetry.LOCATION_BUFFER_MAX_SIZE) {
            true -> {
                queue.removeLast()
                queue.addFirst(location)
                result = true
            }
            false -> {
                queue.addFirst(location)
            }
        }
        return result
    }

    /**
     * This method returns a [Pair] of buffers. The first represents a fixed number of locations before an off route event,
     * while the second represents a fixed number of locations after the off route event
     */
    fun getLocationBuffersAsync(): Deferred<OffRouteBuffers> = accumulatePostOffRouteEventLocationsAsync()

    /**
     * This method populates two location buffers. One with pre-offroute events and the other with post-offroute events
     */
    private fun accumulatePostOffRouteEventLocationsAsync(): Deferred<OffRouteBuffers> {
        val result = CompletableDeferred<OffRouteBuffers>()
        jobControl.scope.launch {
            monitorJob.cancelAndJoin() // Cancel the monitor before calling it again. This call suspends
            val monitorControl = CompletableDeferred<Boolean>() // This variable will be signalled once enough location data is accumulated
            val preOffRoute = mutableListOf<Location>()
            preOffRoute.addAll(offRouteLocationQueue.get()) // copy pre event locations
            offRouteLocationQueue.set(offRouteLocationsAfterOffroute) // point to post event buffer
            monitorLocationChannel(monitorControl) // Start accumulating post event locations
            monitorControl.await() // suspend until we get enough data
            val postOffRoute = mutableListOf<Location>()
            postOffRoute.addAll(offRouteLocationQueue.get()) // copy post event locations
            offRouteLocationQueue.set(offRouteLocationsBeforeOffroute) // reset the buffer to pre event
            result.complete(Pair(preOffRoute, postOffRoute)) // notify caller the job is complete

            // Clear old data
            offRouteLocationsAfterOffroute.clear()
            offRouteLocationsBeforeOffroute.clear()
            monitorJob = monitorLocationChannel() // restart monitor
        }
        return result
    }

    /**
     * This method accumulates locations. The location objects are stored in a FIFO queue.
     * Once the queue size reaches a predefined limit, it becomes signaled and the caller is
     * notified via a deferred object
     */
    private fun monitorLocationChannel(result: CompletableDeferred<Boolean>? = null): Job {
        return jobControl.scope.monitorChannelWithException(channelLocation, { location ->
            if (accumulateLocationAsync(location, offRouteLocationQueue.get())) {
                result?.complete(true)
            }
        })
    }

    private fun monitorLastDeltaLocations(timeDelta: Long = MAX_TIME_LOCATION_COLLECTION): ReceiveChannel<List<Location>> {
        var timeStart = Time.SystemImpl.millis() // initialize clock
        val locationBuffer = ArrayDeque<Location>() // working buffer will accumulate locations
        val lastNSecondsOfLocationsBuffer = mutableListOf<Location>() // return buffer contains the last N seconds of data
        val channelBufferReady = Channel<List<Location>>(Channel.CONFLATED) // Return channel contains the latest data ready for consumption
        jobControl.scope.monitorChannelWithException(channelLastNSecondsOfLocations, { location -> // Receive data from the LocationEngine
            when (Time.SystemImpl.millis() - timeStart >= timeDelta) {
                true -> { // The timer has expired. Copy collected data to the return buffer, clear the working buffer and reset the timer.
                    lastNSecondsOfLocationsBuffer.clear() // Clear the last batch of data
                    lastNSecondsOfLocationsBuffer.addAll(locationBuffer) // Copy new data to the return buffer
                    channelBufferReady.offer(lastNSecondsOfLocationsBuffer) // Offer the collected data to whomever is monitoring this channel
                    locationBuffer.clear() // Clear the working buffer
                    locationBuffer.add(location) // Add the location just received
                    timeStart = Time.SystemImpl.millis() // Reset the timer
                }
                false -> {
                    locationBuffer.add(lastLocation.get())
                }
            }
        })
        return channelBufferReady
    }

    private val predicateSetFirstLocation: (Location) -> Unit = { location: Location ->
        firstLocation.set(location)
        lastLocation.set(location)
    }
    private val predicateSetNextLocation: (Location) -> Unit = { location: Location ->
        lastLocation.set(location)
    }

    private var predicateSetLocation = predicateSetFirstLocation

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val data = RouteProgressWithTimeStamp(Time.SystemImpl.millis(), routeProgress)
        this.routeProgress.set(data)
        channelOnRouteProgress.offer(data)
    }

    override fun onSuccess(locationEngineResult: LocationEngineResult?) {
        locationEngineResult?.lastLocation?.let { location ->
            channelLocation.offer(location)
            channelLastNSecondsOfLocations.offer(location)
            predicateSetLocation(location)
        }
    }

    override fun onFailure(exception: java.lang.Exception) {
        Log.e(MapboxNavigationTelemetry.TAG, "Location engine returned an error $exception")
    }

    fun getRouteProgressChannel(): ReceiveChannel<RouteProgressWithTimeStamp> = channelOnRouteProgress
    fun getLastLocation() = lastLocation.get()
    fun getFirstLocation() = firstLocation.get()
    fun getRouteProgress() = routeProgress.get()

    suspend fun getLastNSecondsOfLocations() = channelBufferReady.receive()

    fun markFirstLocation(): Location {
        predicateSetLocation = predicateSetNextLocation
        firstLocation.set(lastLocation.get())
        return firstLocation.get()
    }

    fun unmarkFirstLocation(): Location {
        predicateSetLocation = predicateSetFirstLocation
        firstLocation.set(lastLocation.get())
        return firstLocation.get()
    }
}
