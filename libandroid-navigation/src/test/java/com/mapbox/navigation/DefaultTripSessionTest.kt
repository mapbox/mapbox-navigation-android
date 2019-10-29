package com.mapbox.navigation

import android.location.Location
import android.os.Handler
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.trip.RouteProgress
import com.mapbox.navigation.base.trip.TripService
import com.mapbox.navigation.base.trip.TripSession
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.TripStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DefaultTripSessionTest {

    private lateinit var tripSession: DefaultTripSession

    private val tripService: TripService = mockk(relaxUnitFun = true)
    private val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    private val locationEngineRequest: LocationEngineRequest = mockk()
    private val route: Route = mockk()

    private val locationCallbackSlot = slot<LocationEngineCallback<LocationEngineResult>>()
    private val locationEngineResult: LocationEngineResult = mockk(relaxUnitFun = true)
    private val location: Location = mockk(relaxUnitFun = true)
    private val enhancedLocation: Location = mockk(relaxUnitFun = true)

    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val tripStatus: TripStatus = mockk(relaxUnitFun = true)
    private val mainHandler: Handler = mockk(relaxUnitFun = true)
    private val workerHandler: Handler = mockk(relaxUnitFun = true)
    private val mainHandlerRunnableSlot = slot<Runnable>()
    private val handlerRunnableSlot = slot<Runnable>()
    private val handlerDelayedRunnableSlot = slot<Runnable>()

    private val routeProgress: RouteProgress = mockk()

    @Before
    fun setUp() {
        tripSession = DefaultTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigator,
            mainHandler,
            workerHandler
        )

        every { navigator.getStatus(any()) } returns tripStatus
        every { tripStatus.enhancedLocation } returns enhancedLocation

        every { locationEngine.requestLocationUpdates(any(), capture(locationCallbackSlot), any()) } answers {}
        every { locationEngineResult.locations } returns listOf(location)

        every { mainHandler.post(capture(mainHandlerRunnableSlot)) } returns true
        every { workerHandler.post(capture(handlerRunnableSlot)) } returns true
        every { workerHandler.postDelayed(capture(handlerDelayedRunnableSlot), any()) } returns true

        every { tripStatus.routeProgress } returns routeProgress
    }

    @Test
    fun startSession() {
        tripSession.start()
        verify { tripService.startService(any()) }
        verify { locationEngine.requestLocationUpdates(locationEngineRequest, any(), Looper.getMainLooper()) }

        verify { workerHandler.postDelayed(handlerDelayedRunnableSlot.captured, 1000) }
    }

    @Test
    fun stopSession() {
        tripSession.start()
        tripSession.stop()
        verify { tripService.stopService() }
        verify { locationEngine.removeLocationUpdates(locationCallbackSlot.captured) }
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        verify { workerHandler.removeCallbacks(handlerRunnableSlot.captured) }
        verify { workerHandler.removeCallbacks(handlerDelayedRunnableSlot.captured) }
    }

    @Test
    fun locationObserverSuccess() {
        tripSession.start()
        val observer: TripSession.LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        handlerDelayedRunnableSlot.captured.run()
        mainHandlerRunnableSlot.captured.run()
        verify { observer.onRawLocationChanged(location) }
        verify { observer.onEnhancedLocationChanged(enhancedLocation) }
        assertEquals(location, tripSession.getRawLocation())
        assertEquals(enhancedLocation, tripSession.getEnhancedLocation())
    }

    @Test
    fun locationObserverImmediate() {
        tripSession.start()
        val observer: TripSession.LocationObserver = mockk(relaxUnitFun = true)
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        handlerDelayedRunnableSlot.captured.run()
        mainHandlerRunnableSlot.captured.run()
        tripSession.registerLocationObserver(observer)
        verify { observer.onRawLocationChanged(location) }
        verify { observer.onEnhancedLocationChanged(enhancedLocation) }
    }

    @Test
    fun unregisterLocationObserver() {
        tripSession.start()
        val observer: TripSession.LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        tripSession.unregisterLocationObserver(observer)
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        handlerDelayedRunnableSlot.captured.run()
        mainHandlerRunnableSlot.captured.run()
        verify(exactly = 0) { observer.onRawLocationChanged(any()) }
        verify(exactly = 0) { observer.onEnhancedLocationChanged(any()) }
    }

    @Test
    fun enhancedLocationPush() {
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        val runnable = handlerRunnableSlot.captured
        verify { workerHandler.post(runnable) }
        runnable.run()
        verify { navigator.updateLocation(location) }
    }

    @Test
    fun routeProgressObserverSuccess() {
        tripSession.start()
        val observer: TripSession.RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)
        handlerDelayedRunnableSlot.captured.run()
        mainHandlerRunnableSlot.captured.run()
        verify { observer.onRouteProgressChanged(routeProgress) }
        assertEquals(routeProgress, tripSession.getRouteProgress())
    }

    @Test
    fun routeProgressObserverImmediate() {
        tripSession.start()
        val observer: TripSession.RouteProgressObserver = mockk(relaxUnitFun = true)
        handlerDelayedRunnableSlot.captured.run()
        mainHandlerRunnableSlot.captured.run()
        tripSession.registerRouteProgressObserver(observer)
        verify { observer.onRouteProgressChanged(routeProgress) }
        assertEquals(routeProgress, tripSession.getRouteProgress())
    }

    @Test
    fun routeProgressObserverUnregister() {
        tripSession.start()
        val observer: TripSession.RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)
        tripSession.unregisterRouteProgressObserver(observer)
        handlerDelayedRunnableSlot.captured.run()
        mainHandlerRunnableSlot.captured.run()
        verify(exactly = 0) { observer.onRouteProgressChanged(routeProgress) }
    }

    @Test
    fun getTripService() {
        assertEquals(tripService, tripSession.tripService)
    }

    @Test
    fun getLocationEngine() {
        assertEquals(locationEngine, tripSession.locationEngine)
    }

    @Test
    fun getRoute() {
        tripSession.route = route
        assertEquals(route, tripSession.route)
    }

    @Test
    fun setRoute() {
        tripSession.route = route
        handlerRunnableSlot.captured.run()
        verify { navigator.setRoute(route) }
    }
}
