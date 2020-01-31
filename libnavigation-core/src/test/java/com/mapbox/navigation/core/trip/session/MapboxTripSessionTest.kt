package com.mapbox.navigation.core.trip.session

import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.TripStatus
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.thread.JobControl
import com.mapbox.navigation.utils.thread.ThreadController
import com.mapbox.navigator.NavigationStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapboxTripSessionTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var tripSession: MapboxTripSession

    private val tripService: TripService = mockk(relaxUnitFun = true)
    private val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    private val locationEngineRequest: LocationEngineRequest = mockk()
    private val route: DirectionsRoute = mockk()

    private val locationCallbackSlot = slot<LocationEngineCallback<LocationEngineResult>>()
    private val locationEngineResult: LocationEngineResult = mockk(relaxUnitFun = true)
    private val location: Location = mockk(relaxUnitFun = true)
    private val enhancedLocation: Location = mockk(relaxUnitFun = true)

    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val navigationStatus: NavigationStatus = mockk(relaxUnitFun = true)
    private val tripStatus: TripStatus = mockk(relaxUnitFun = true)

    private val routeProgress: RouteProgress = mockk()
    private val navigatorPollingDelay = 1500L

    @Before
    fun setUp() {
        tripSession = MapboxTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigatorPollingDelay,
            navigator
        )

        every { navigator.getStatus(any()) } returns tripStatus
        every { navigator.updateLocation(any()) } returns false
        every { navigator.setRoute(any()) } returns navigationStatus
        every { tripStatus.enhancedLocation } returns enhancedLocation
        every { tripStatus.offRoute } returns false

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

        tripSession.stop()
    }

    @Test
    fun stopSession() {
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        tripSession.stop()

        verify { tripService.stopService() }
        verify { locationEngine.removeLocationUpdates(locationCallbackSlot.captured) }
    }

    @Test
    fun locationObserverSuccess() {
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        verify { observer.onRawLocationChanged(location) }
        assertEquals(location, tripSession.getRawLocation())

        tripSession.stop()
    }

    @Test
    fun locationObserverImmediate() {
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        tripSession.registerLocationObserver(observer)

        verify { observer.onRawLocationChanged(location) }

        tripSession.stop()
    }

    @Test
    fun unregisterLocationObserver() {
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        tripSession.unregisterLocationObserver(observer)

        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        verify(exactly = 0) { observer.onRawLocationChanged(any()) }

        tripSession.stop()
    }

    @Test
    fun locationPush() {
        tripSession.start()

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        verify { navigator.updateLocation(location) }

        tripSession.stop()
    }

    @Test
    fun routeProgressObserverSuccess() = coroutineRule.runBlockingTest {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        mockkObject(ThreadController)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { routeProgress.bannerInstructions() } returns null
        every { routeProgress.voiceInstructions() } returns null
        tripSession = MapboxTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigatorPollingDelay,
            navigator,
            ThreadController
        )
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        val observer: RouteProgressObserver = mockk(relaxUnitFun = true)

        tripSession.registerRouteProgressObserver(observer)

        verify { observer.onRouteProgressChanged(routeProgress) }
        assertEquals(routeProgress, tripSession.getRouteProgress())
        tripSession.stop()
        unmockkObject(ThreadController)
    }

    @Test
    fun routeProgressObserverImmediate() = coroutineRule.runBlockingTest {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        mockkObject(ThreadController)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        tripSession = MapboxTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigatorPollingDelay,
            navigator,
            ThreadController
        )
        val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        tripSession.unregisterRouteProgressObserver(observer)

        tripSession.registerRouteProgressObserver(observer)

        verify(exactly = 2) { observer.onRouteProgressChanged(routeProgress) }
        assertEquals(routeProgress, tripSession.getRouteProgress())
        tripSession.stop()
        unmockkObject(ThreadController)
    }

    @Test
    fun routeProgressObserverUnregister() = coroutineRule.runBlockingTest {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        mockkObject(ThreadController)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { routeProgress.bannerInstructions() } returns null
        every { routeProgress.voiceInstructions() } returns null
        tripSession = MapboxTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigatorPollingDelay,
            navigator,
            ThreadController
        )
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)

        tripSession.unregisterRouteProgressObserver(observer)

        tripSession.stop()
        verify(exactly = 1) { observer.onRouteProgressChanged(routeProgress) }
        unmockkObject(ThreadController)
    }

    @Test
    fun enhancedLocationObserverSuccess() = coroutineRule.runBlockingTest {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        mockkObject(ThreadController)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { routeProgress.bannerInstructions() } returns null
        every { routeProgress.voiceInstructions() } returns null
        tripSession = MapboxTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigatorPollingDelay,
            navigator,
            ThreadController
        )
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        val observer: LocationObserver = mockk(relaxUnitFun = true)

        tripSession.registerLocationObserver(observer)

        verify { observer.onEnhancedLocationChanged(enhancedLocation) }
        assertEquals(enhancedLocation, tripSession.getEnhancedLocation())
        tripSession.stop()
        unmockkObject(ThreadController)
    }

    @Test
    fun enhancedLocationObserverImmediate() = coroutineRule.runBlockingTest {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        mockkObject(ThreadController)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { routeProgress.bannerInstructions() } returns null
        every { routeProgress.voiceInstructions() } returns null
        tripSession = MapboxTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigatorPollingDelay,
            navigator,
            ThreadController
        )
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        tripSession.unregisterLocationObserver(observer)

        tripSession.registerLocationObserver(observer)

        verify(exactly = 2) { observer.onEnhancedLocationChanged(enhancedLocation) }
        assertEquals(enhancedLocation, tripSession.getEnhancedLocation())
        tripSession.stop()
        unmockkObject(ThreadController)
    }

    @Test
    fun enhancedLocationObserverUnregister() = coroutineRule.runBlockingTest {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        mockkObject(ThreadController)
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { routeProgress.bannerInstructions() } returns null
        every { routeProgress.voiceInstructions() } returns null
        tripSession = MapboxTripSession(
            tripService,
            locationEngine,
            locationEngineRequest,
            navigatorPollingDelay,
            navigator,
            ThreadController
        )
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        tripSession.unregisterLocationObserver(observer)

        tripSession.stop()
        verify(exactly = 1) { observer.onEnhancedLocationChanged(enhancedLocation) }
        unmockkObject(ThreadController)
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

        verify { navigator.setRoute(route) }
    }

    @Test
    fun unregisterAllLocationObservers() {
        every { routeProgress.bannerInstructions() } returns null
        every { routeProgress.voiceInstructions() } returns null

        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        tripSession.unregisterAllLocationObservers()

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        verify(exactly = 0) { observer.onRawLocationChanged(location) }
        assertEquals(location, tripSession.getRawLocation())

        tripSession.stop()
    }

    @Test
    fun unregisterAllRouteProgressObservers() {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        mockkObject(ThreadController)

        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { routeProgress.bannerInstructions() } returns null
        every { routeProgress.voiceInstructions() } returns null

        tripSession = MapboxTripSession(
                tripService,
                locationEngine,
                locationEngineRequest,
                navigatorPollingDelay,
                navigator,
                ThreadController
        )
        tripSession.start()
        val routeProgressObserver: RouteProgressObserver = mockk(relaxUnitFun = true)
        val locationObserver: LocationObserver = mockk(relaxUnitFun = true)

        tripSession.registerLocationObserver(locationObserver)
        tripSession.registerRouteProgressObserver(routeProgressObserver)

        tripSession.unregisterAllRouteProgressObservers()

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        verify(exactly = 0) { routeProgressObserver.onRouteProgressChanged(any()) }

        tripSession.stop()
        unmockkObject(ThreadController)
    }

    @Test
    fun unregisterAllOffRouteObservers() {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        mockkObject(ThreadController)

        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { routeProgress.bannerInstructions() } returns null
        every { routeProgress.voiceInstructions() } returns null
        every { tripStatus.offRoute } returns true

        tripSession = MapboxTripSession(
                tripService,
                locationEngine,
                locationEngineRequest,
                navigatorPollingDelay,
                navigator,
                ThreadController
        )
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        val locationObserver: LocationObserver = mockk(relaxUnitFun = true)

        tripSession.registerLocationObserver(locationObserver)
        tripSession.registerOffRouteObserver(offRouteObserver)

        tripSession.unregisterAllOffRouteObservers()

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        // registerOffRouteObserver will call onOffRouteStateChanged() on
        // the offRouteObserver so that accounts for the verify 1 time
        // below. However there shouldn't be any additional calls when
        // the locationCallback.onSuccess() is called because the collection
        // of offRouteObservers should be empty.
        verify(exactly = 1) { offRouteObserver.onOffRouteStateChanged(any()) }

        tripSession.stop()
        unmockkObject(ThreadController)
    }

    @Test
    fun unregisterAllStateObservers() {
        val stateObserver: TripSessionStateObserver = mockk(relaxUnitFun = true)

        tripSession.registerStateObserver(stateObserver)

        tripSession.start()

        tripSession.unregisterAllStateObservers()

        // stop() would normally trigger a call to stateObserver.onSessionStopped()
        tripSession.stop()

        verify(exactly = 1) { stateObserver.onSessionStarted() }
        verify(exactly = 1) { stateObserver.onSessionStopped() }
    }

    @Test
    fun unregisterAllBannerInstructionsObservers() {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        val bannerInstructionsObserver: BannerInstructionsObserver = mockk(relaxUnitFun = true)
        val bannerInstructions: BannerInstructions = mockk()
        mockkObject(ThreadController)

        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { routeProgress.bannerInstructions() } returns bannerInstructions
        every { routeProgress.voiceInstructions() } returns null
        every { tripStatus.offRoute } returns true

        tripSession = MapboxTripSession(
                tripService,
                locationEngine,
                locationEngineRequest,
                navigatorPollingDelay,
                navigator,
                ThreadController
        )
        tripSession.start()
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        tripSession.stop()

        tripSession.start()
        tripSession.unregisterAllBannerInstructionsObservers()

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        verify(exactly = 1) { bannerInstructionsObserver.onNewBannerInstructions(any()) }

        tripSession.stop()
        unmockkObject(ThreadController)
    }

    @Test
    fun unregisterAllVoiceInstructionsObservers() {
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)
        val voiceInstructionsObserver: VoiceInstructionsObserver = mockk(relaxUnitFun = true)
        val voiceInstructions: VoiceInstructions = mockk()
        mockkObject(ThreadController)

        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { routeProgress.bannerInstructions() } returns null
        every { routeProgress.voiceInstructions() } returns voiceInstructions
        every { tripStatus.offRoute } returns true

        tripSession = MapboxTripSession(
                tripService,
                locationEngine,
                locationEngineRequest,
                navigatorPollingDelay,
                navigator,
                ThreadController
        )
        tripSession.start()
        tripSession.registerVoiceInstructionsObserver(voiceInstructionsObserver)

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        tripSession.stop()

        tripSession.start()
        tripSession.unregisterAllVoiceInstructionsObservers()

        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        verify(exactly = 1) { voiceInstructionsObserver.onNewVoiceInstructions(any()) }

        tripSession.stop()
        unmockkObject(ThreadController)
    }
}
