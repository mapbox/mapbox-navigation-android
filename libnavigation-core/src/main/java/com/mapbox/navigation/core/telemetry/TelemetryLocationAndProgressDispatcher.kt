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
import java.util.Collections
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

internal class TelemetryLocationAndProgressDispatcher(val scope: CoroutineScope) :
    RouteProgressObserver, LocationObserver, RoutesObserver {
    private var lastLocation: AtomicReference<Location?> = AtomicReference(null)
    private var routeProgress: AtomicReference<RouteProgressWithTimestamp> =
        AtomicReference(RouteProgressWithTimestamp(0, RouteProgress.Builder().build()))
    private val channelOffRouteEvent = Channel<RouteAvailable>(Channel.CONFLATED)
    private val channelLocationReceived_1 = Channel<Location>(Channel.CONFLATED)
    private val channelLocationReceived_2 = Channel<Location>(Channel.CONFLATED)
    private val channelOnRouteProgress =
        Channel<RouteProgressWithTimestamp>(Channel.CONFLATED) // we want just the last notification
    private lateinit var jobControl: CoroutineScope
    private var originalRoute = AtomicReference<RouteAvailable?>(null)
    private var accumulationJob: Job = Job()
    private val currentLocationBuffer = SynchronizedItemBuffer<Location>()
    private val locationEventBuffer = SynchronizedItemBuffer<ItemAccumulationEventDescriptor<Location>>()
    private val originalRoutePreInit = { routes: List<DirectionsRoute> ->
        if (originalRoute.get() == null) {
            originalRoute.set(RouteAvailable(routes[0], Date()))
            originalRouteDelegate = originalRoutePostInit
        }
    }
    private val originalRouteDeffered = CompletableDeferred<DirectionsRoute>()
    private var originalRouteDefferedValue: DirectionsRoute? = null

    private val originalRoutePostInit = { routes: List<DirectionsRoute> -> Unit }
    private var originalRouteDelegate: (List<DirectionsRoute>) -> Unit = originalRoutePreInit
    private val firstLocation = CompletableDeferred<Location>()
    private var firstLocationValue: Location? = null

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
        jobControl = scope
        // Unconditionally update the contents of the pre-event buffer
        jobControl.monitorChannelWithException(channelLocationReceived_1, { location ->
            accumulateLocationAsync(location, currentLocationBuffer)
        })

        /**
         * Process the location event buffer twice. The first time, update each of it's elements
         * with a new location object. On the second pass, execute the stored lambda if the buffer
         * size is equal to or greater than a given value.
         */
        accumulationJob = jobControl.monitorChannelWithException(channelLocationReceived_2, { location ->
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
        }, onCancellation = { Log.d(TAG, "channelLocationReceived_2 canceled") })
    }

    fun flushBuffers() {
        Log.d(TAG, "flushing buffers before ${currentLocationBuffer.size()}")
        locationEventBuffer.applyToEach { item ->
            item.onBufferFull(item.preEventBuffer, item.postEventBuffer)
            false
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
    fun cancelCollectionAndPostFinalEvents() {
        ThreadController.getIOScopeAndRootJob().scope.launch {
            flushBuffers()
        }
    }

    /**
     * This channel becomes signaled if a navigation route is selected
     */
    fun getDirectionsRouteChannel(): ReceiveChannel<RouteAvailable> = channelOffRouteEvent

    fun getCopyOfCurrentLocationBuffer() = currentLocationBuffer.getCopy()

    fun getOriginalRoute() = originalRoute.get()

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val data = RouteProgressWithTimestamp(Time.SystemImpl.millis(), routeProgress)
        this.routeProgress.set(data)
        channelOnRouteProgress.offer(data)
    }

    fun getRouteProgressChannel(): ReceiveChannel<RouteProgressWithTimestamp> =
        channelOnRouteProgress

    fun getLastLocation(): Location? = lastLocation.get()

    fun getRouteProgress(): RouteProgressWithTimestamp = routeProgress.get()

    fun isRouteAvailable(): RouteAvailable? = originalRoute.get()

    fun clearOriginalRoute() {
        originalRoute.set(null)
        originalRouteDefferedValue = null
        originalRouteDelegate = originalRoutePreInit
    }

    override fun onRawLocationChanged(rawLocation: Location) {
        // Do nothing
    }

    override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
        channelLocationReceived_1.offer(enhancedLocation)
        channelLocationReceived_2.offer(enhancedLocation)
        lastLocation.set(enhancedLocation)
        when (firstLocationValue) {
            null -> {
                firstLocationValue = enhancedLocation
                firstLocationValue?.let { location ->
                    firstLocation.complete(location)
                }
            }
            else -> {
                firstLocationValue?.let { location ->
                    firstLocation.complete(location)
                }
            }
        }
    }

    fun getFirstLocationAsync() = firstLocation

    fun getOriginalRouteAsync() = originalRouteDeffered

    private fun notifyOfNewRoute(routes: List<DirectionsRoute>) {
        when (originalRouteDefferedValue) {
            null -> {
                Log.d(TAG, "First time route set")
                originalRouteDefferedValue = routes[0]
                originalRouteDefferedValue?.let { route ->
                    originalRouteDeffered.complete(route)
                }
            }
            else -> {
                Log.d(TAG, "Subsequent route set")
                originalRouteDefferedValue?.let { route ->
                    originalRouteDeffered.complete(route)
                }
            }
        }
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {

        when (routes.isEmpty()) {
            true -> {
                // Do nothing.
            }
            false -> {
                val date = Date()
                channelOffRouteEvent.offer(RouteAvailable(routes[0], date))
                originalRouteDelegate(routes)
                notifyOfNewRoute(routes)
            }
        }
    }
}
