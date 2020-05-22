package com.mapbox.navigation.core.telemetry

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.LOCATION_BUFFER_MAX_SIZE
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry.TAG
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import com.mapbox.navigation.utils.internal.monitorChannelWithException
import java.util.Collections
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

private const val DISPATCHER_TAG = "TELEMETRY_TEST"
private typealias RouteProgressReference = (RouteProgress) -> Unit

internal class TelemetryLocationAndProgressDispatcher(scope: CoroutineScope) :
    RouteProgressObserver, LocationObserver, RoutesObserver, OffRouteObserver {
    private var lastLocation: AtomicReference<Location?> = AtomicReference(null)
    private var routeProgress: AtomicReference<RouteProgressWithTimestamp> =
        AtomicReference(RouteProgressWithTimestamp(0, RouteProgress.Builder().build()))
    private val channelOffRouteEvent = Channel<Boolean>(Channel.CONFLATED)
    private val channelNewRouteAvailable = Channel<RouteAvailable>(Channel.CONFLATED)
    private val channelLocationReceived = Channel<Location>(Channel.CONFLATED)
    private val channelOnRouteProgress =
        Channel<RouteProgressWithTimestamp>(Channel.CONFLATED) // we want just the last notification
    private var jobControl: CoroutineScope = scope
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
    private val originalRouteDiffered = CompletableDeferred<DirectionsRoute>()
    private var originalRouteDifferedValue: DirectionsRoute? = null

    private val originalRoutePostInit = { _: List<DirectionsRoute> -> Unit }
    private var originalRouteDelegate: (List<DirectionsRoute>) -> Unit = originalRoutePreInit
    private val firstLocation = CompletableDeferred<Location>()
    private var firstLocationValue: Location? = null
    private var priorState = RouteProgressState.ROUTE_INVALID
    private val routeProgressPredicate = AtomicReference<RouteProgressReference>()

    init {
        routeProgressPredicate.set { routeProgress -> beforeArrival(routeProgress) }
    }

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
        accumulationJob = jobControl.monitorChannelWithException(channelLocationReceived, { location ->
            accumulateLocationAsync(location, currentLocationBuffer)
            processLocationBuffer(location)
        })
    }

    /**
     * Process the location event buffer twice. The first time, update each of it's elements
     * with a new location object. On the second pass, execute the stored lambda if the buffer
     * size is equal to or greater than a given value.
     */
    private fun processLocationBuffer(location: Location) {
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
    fun cancelCollectionAndPostFinalEvents(): Job {
        return ThreadController.getIOScopeAndRootJob().scope.launch {
            flushBuffers()
            locationEventBuffer.clear()
        }
    }

    /**
     * This channel becomes signaled if a navigation route is selected
     */
    fun getDirectionsRouteChannel(): ReceiveChannel<RouteAvailable> = channelNewRouteAvailable

    fun getCopyOfCurrentLocationBuffer() = currentLocationBuffer.getCopy()

    fun getOriginalRouteReadOnly() = originalRoute.get()

    fun getOriginalRouteReadWrite() = originalRoute

    fun resetRouteProgressProcessor() {
        routeProgressPredicate.set { routeProgress -> beforeArrival(routeProgress) }
    }

    fun getOffRouteEventChannel(): ReceiveChannel<Boolean> = channelOffRouteEvent
    /**
     * This method is called for any state change, excluding RouteProgressState.ROUTE_ARRIVED.
     * It forwards the route progress data to a listener and saves it to a local variable
     */
    private fun beforeArrival(routeProgress: RouteProgress) {
        val data = RouteProgressWithTimestamp(Time.SystemImpl.millis(), routeProgress)
        this.routeProgress.set(data)
        channelOnRouteProgress.offer(data)
        if (routeProgress.currentState == RouteProgressState.ROUTE_ARRIVED) {
            routeProgressPredicate.set { progress -> afterArrival(progress) }
        }
    }

    /**
     * This method is called in response to receiving a RouteProgressState.ROUTE_ARRIVED event.
     * It stores the route progress data without notifying listeners.
     */
    private fun afterArrival(routeProgress: RouteProgress) {
        when (routeProgress.currentState) {
            priorState -> {
            }
            else -> {
                priorState = routeProgress.currentState ?: priorState
                Log.d(TAG, "route progress state = ${routeProgress.currentState}")
            }
        }
        val data = RouteProgressWithTimestamp(Time.SystemImpl.millis(), routeProgress)
        this.routeProgress.set(data)
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        routeProgressPredicate.get()(routeProgress)
    }

    fun getRouteProgressChannel(): ReceiveChannel<RouteProgressWithTimestamp> =
        channelOnRouteProgress

    fun getLastLocation(): Location? = lastLocation.get()

    fun getRouteProgress(): RouteProgressWithTimestamp = routeProgress.get()

    fun clearOriginalRoute() {
        originalRoute.set(null)
        originalRouteDifferedValue = null
        originalRouteDelegate = originalRoutePreInit
    }

    override fun onRawLocationChanged(rawLocation: Location) {
        // Do nothing
    }

    override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
        channelLocationReceived.offer(enhancedLocation)
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

    fun getOriginalRouteAsync() = originalRouteDiffered

    private fun notifyOfNewRoute(routes: List<DirectionsRoute>) {
        when (originalRouteDifferedValue) {
            null -> {
                Log.d(TAG, "First time route set")
                if (routes.isNotEmpty()) {
                    originalRouteDifferedValue = routes[0]
                    originalRouteDifferedValue?.let { route ->
                        originalRouteDiffered.complete(route)
                    }
                } else {
                    Log.d(TAG, "Empty route list received. Not setting route 2")
                }
            }
            else -> {
                if (routes.isNotEmpty()) {
                    Log.d(TAG, "Subsequent route set")
                    originalRouteDifferedValue?.let { route ->
                        originalRouteDiffered.complete(route)
                    }
                } else {
                    Log.d(TAG, "Empty route list received. Not setting route 2")
                }
            }
        }
    }

    override fun onRoutesChanged(routes: List<DirectionsRoute>) {

        when (routes.isEmpty()) {
            true -> {
                Log.d(DISPATCHER_TAG, "onRoutesChanged received an empty route list")
            }
            false -> {
                Log.d(DISPATCHER_TAG, "onRoutesChanged received a valid route list")
                val date = Date()
                channelNewRouteAvailable.offer(RouteAvailable(routes[0], date))
                originalRouteDelegate(routes)
                notifyOfNewRoute(routes)
            }
        }
    }

    override fun onOffRouteStateChanged(offRoute: Boolean) {
        Log.d(DISPATCHER_TAG, "onOffRouteStateChanged $offRoute")
        channelOffRouteEvent.offer(offRoute)
    }
}
