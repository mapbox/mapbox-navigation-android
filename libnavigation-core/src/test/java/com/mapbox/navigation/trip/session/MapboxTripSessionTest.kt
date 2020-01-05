package com.mapbox.navigation.trip.session

import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.TripStatus
import com.mapbox.navigation.trip.service.TripService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxTripSessionTest {

    private lateinit var tripSession: MapboxTripSession

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

    private val routeProgress: RouteProgress = mockk()

    @Before
    fun setUp() {
        tripSession = MapboxTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigator
        )

        every { navigator.getStatus(any()) } returns tripStatus
        every { tripStatus.enhancedLocation } returns enhancedLocation

        every {
            locationEngine.requestLocationUpdates(
                any(),
                capture(locationCallbackSlot),
                any()
            )
        } answers {}
        every { locationEngineResult.locations } returns listOf(location)

        every { tripStatus.routeProgress } returns routeProgress
    }

    @Test
    fun startSession() {
        tripSession.start()

        verify { tripService.startService() }
        verify {
            locationEngine.requestLocationUpdates(
                locationEngineRequest,
                any(),
                Looper.getMainLooper()
            )
        }
    }

    @Test
    fun stopSession() {
        tripSession.start()

        tripSession.stop()

        verify { tripService.stopService() }
        verify { locationEngine.removeLocationUpdates(locationCallbackSlot.captured) }
    }

    @Test
    fun locationObserverSuccess() = runBlocking {
        tripSession.start()
        val observer: TripSession.LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        tripSession.navigatorPolling()
        verify { observer.onRawLocationChanged(location) }
        verify { observer.onEnhancedLocationChanged(enhancedLocation) }
        assertEquals(location, tripSession.getRawLocation())
        assertEquals(enhancedLocation, tripSession.getEnhancedLocation())
    }

    @Test
    fun locationObserverImmediate() = runBlocking {
        tripSession.start()
        val observer: TripSession.LocationObserver = mockk(relaxUnitFun = true)
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        tripSession.navigatorPolling()

        tripSession.registerLocationObserver(observer)

        verify { observer.onRawLocationChanged(location) }
        verify { observer.onEnhancedLocationChanged(enhancedLocation) }
    }

    @Test
    fun unregisterLocationObserver() = runBlocking {
        tripSession.start()
        val observer: TripSession.LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        tripSession.unregisterLocationObserver(observer)

        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        tripSession.navigatorPolling()
        verify(exactly = 0) { observer.onRawLocationChanged(any()) }
        verify(exactly = 0) { observer.onEnhancedLocationChanged(any()) }
    }

    @Test
    fun enhancedLocationPush() = runBlocking {
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        tripSession.navigatorPolling()

        verify { navigator.updateLocation(location) }
    }

    @Test
    fun routeProgressObserverSuccess() = runBlocking {
        tripSession.start()
        val observer: TripSession.RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)

        tripSession.navigatorPolling()

        verify { observer.onRouteProgressChanged(routeProgress) }
        assertEquals(routeProgress, tripSession.getRouteProgress())
    }

    @Test
    fun routeProgressObserverImmediate() = runBlocking {
        tripSession.start()
        val observer: TripSession.RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.navigatorPolling()

        tripSession.registerRouteProgressObserver(observer)

        verify { observer.onRouteProgressChanged(routeProgress) }
        assertEquals(routeProgress, tripSession.getRouteProgress())
    }

    @Test
    fun routeProgressObserverUnregister() = runBlocking {
        tripSession.start()
        val observer: TripSession.RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)

        tripSession.unregisterRouteProgressObserver(observer)

        tripSession.navigatorPolling()
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
    fun setRoute() = runBlocking {
        tripSession.route = route

        verify { navigator.setRoute(route) }
    }
}
