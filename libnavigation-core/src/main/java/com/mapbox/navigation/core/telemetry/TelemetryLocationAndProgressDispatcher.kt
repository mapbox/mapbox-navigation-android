package com.mapbox.navigation.core.telemetry

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.LOCATION_BUFFER_MAX_SIZE
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigation.utils.thread.monitorChannelWithException
import com.mapbox.navigation.utils.time.Time
import java.util.Collections
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

internal class TelemetryLocationAndProgressDispatcher :
    RouteProgressObserver, LocationObserver, RoutesObserver {
    private var lastLocation: AtomicReference<Location> = AtomicReference(Location("Default"))
    private var routeProgress: AtomicReference<RouteProgressWithTimestamp> =
            AtomicReference(RouteProgressWithTimestamp(0, RouteProgress.Builder().build()))
    private val channelRouteSelected = Channel<RouteAvailable>(Channel.CONFLATED)
    private val channelLocationRecieved_1 = Channel<Location>(Channel.CONFLATED)
    private val channelLocationReceived_2 = Channel<Location>(Channel.CONFLATED)
    private val channelOnRouteProgress =
        Channel<RouteProgressWithTimestamp>(Channel.CONFLATED) // we want just the last notification
    private var jobControl = ThreadController.getIOScopeAndRootJob()
    private val routeSelected = AtomicReference<RouteAvailable?>(null)
    private var accumulationJob: Job = Job()
    private val currentLocationBuffer = SynchronizedItemBuffer<Location>()
    private val locationEventBuffer = SynchronizedItemBuffer<ItemAccumulationEventDescriptor<Location>>()
    /**
     * This class provides thread-safe access to a mutable list of locations
     */
    private class SynchronizedItemBuffer<T> {
        private val synchronizedCollection: MutableList<T> = Collections.synchronizedList(mutableListOf<T>())

        fun addItem(item: T) {
            synchronized(synchronizedCollection) {
                synchronizedCollection.add(0, item)
            }
        }

        fun removeItem() {
            synchronized(synchronizedCollection) {
                if (synchronizedCollection.isNotEmpty()) {
                    val index = synchronizedCollection.size - 1
                    synchronizedCollection.removeAt(index)
                }
            }
        }

        fun getCopy(): List<T> {
            val result = mutableListOf<T>()
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

        fun applyToEach(predicate: (T) -> Boolean) {
            synchronized(synchronizedCollection) {
                val iterator = synchronizedCollection.iterator()
                while (iterator.hasNext()) {
                    val nextItem = iterator.next()
                    if (!predicate(nextItem)) {
                        iterator.remove()
                    }
                }
            }
        }

        fun size() = synchronizedCollection.size
    }

    init {
        // Unconditionally update the contents of the pre-event buffer
        jobControl.scope.monitorChannelWithException(channelLocationRecieved_1, { location ->
            accumulateLocationAsync(location, currentLocationBuffer)
        })

        /**
         * Process the location event buffer twice. The first time, update each of it's elements
         * with a new location object. On the second pass, execute the stored lambda if the buffer
         * size is equal to or greater then a given value.
         */
        accumulationJob = jobControl.scope.monitorChannelWithException(channelLocationReceived_2, { location ->
            // Update each event buffer with a new location
            locationEventBuffer.applyToEach { item ->
                item.postEventBuffer.addFirst(location)
                true
            }
            locationEventBuffer.applyToEach { item ->
                when (item.postEventBuffer.size >= LOCATION_BUFFER_MAX_SIZE) {
                    true -> {
                        item.onBufferFull(item.preEventBuffer, item.postEventBuffer)
                        false
                    }
                    else -> {
                        // Do nothing.
                        true
                    }
                }
            }
        })
        jobControl.scope.launch {
            select<Unit> {
                accumulationJob.onJoin {
                    locationEventBuffer.applyToEach { item ->
                        item.onBufferFull(item.preEventBuffer, item.postEventBuffer)
                        false
                    }
                }
            }
        }
    }

    /**
     * This method accumulates locations. The number of locations is limited by [MapboxNavigationTelemetry.LOCATION_BUFFER_MAX_SIZE].
     * Once this limit is reached, an item is removed before another is added. The method returns true if the queue reaches capacity,
     * false otherwise
     */
    private fun accumulateLocationAsync(location: Location, queue: SynchronizedItemBuffer<Location>): Boolean {
        var result = false
        when (queue.size() >= LOCATION_BUFFER_MAX_SIZE) {
            true -> {
                queue.removeItem()
                queue.addItem(location)
                result = true
            }
            false -> {
                queue.addItem(location)
            }
        }
        return result
    }

    fun addLocationEventDescriptor(eventDescriptor: ItemAccumulationEventDescriptor<Location>) {
        eventDescriptor.preEventBuffer.clear()
        eventDescriptor.postEventBuffer.clear()
        eventDescriptor.preEventBuffer.addAll(currentLocationBuffer.getCopy())
        locationEventBuffer.addItem(eventDescriptor)
    }
    /**
     * This method cancels all jobs that accumulate telemetry data. The side effect of this call is to call Telemetry.addEvent(), which may cause events to be sent
     * to the back-end server
     */
    fun cancelCollectionAndPostFinalEvents() = accumulationJob.cancel()

    /**
     * This channel becomes signaled if a navigation route is selected
     */
    fun getDirectionsRouteChannel(): ReceiveChannel<RouteAvailable> = channelRouteSelected

    fun getLastDirectionsRoute() = routeSelected

    fun getCopyOfCurrentLocationBuffer() = currentLocationBuffer.getCopy()

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
        channelLocationRecieved_1.offer(enhancedLocation)
        channelLocationReceived_2.offer(enhancedLocation)
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
