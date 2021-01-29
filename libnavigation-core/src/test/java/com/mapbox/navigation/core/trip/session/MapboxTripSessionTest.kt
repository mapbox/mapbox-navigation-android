package com.mapbox.navigation.core.trip.session

import android.content.Context
import android.location.Location
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerView
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.options.DEFAULT_NAVIGATOR_PREDICTION_MILLIS
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.core.navigator.getMapMatcherResult
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.RouteInitInfo
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MapboxTripSessionTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var tripSession: MapboxTripSession

    private val tripService: TripService = mockk(relaxUnitFun = true)
    private val route: DirectionsRoute = mockk()

    private val context: Context = mockk()
    private val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    private lateinit var navigationOptions: NavigationOptions
    private val locationCallbackSlot = slot<LocationEngineCallback<LocationEngineResult>>()
    private val locationEngineResult: LocationEngineResult = mockk(relaxUnitFun = true)
    private val location: Location = mockk(relaxUnitFun = true)
    private val enhancedLocation: Location = mockk(relaxUnitFun = true)
    private val keyPoints: List<Location> = listOf(mockk(relaxUnitFun = true))

    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val tripStatus: TripStatus = mockk(relaxUnitFun = true)
    private val logger: Logger = mockk(relaxUnitFun = true)

    private val routeProgress: RouteProgress = mockk()

    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    private val stateObserver: TripSessionStateObserver = mockk(relaxUnitFun = true)

    private val mapMatcherResult: MapMatcherResult = mockk(relaxUnitFun = true)

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        mockkStatic("com.mapbox.navigation.core.navigator.NavigatorMapperKt")
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { context.applicationContext } returns context
        navigationOptions = NavigationOptions.Builder(context)
            .locationEngine(locationEngine)
            .build()
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions = navigationOptions,
            navigator,
            logger = logger,
            accessToken = "pk.1234"
        )

        coEvery { navigator.getStatus(any()) } returns tripStatus
        coEvery { navigator.updateLocation(any()) } returns false
        coEvery { navigator.setRoute(any()) } returns null
        every { tripStatus.enhancedLocation } returns enhancedLocation
        every { tripStatus.keyPoints } returns keyPoints
        every { tripStatus.offRoute } returns false
        every { tripStatus.getMapMatcherResult() } returns mapMatcherResult
        every { routeProgress.bannerInstructions } returns null
        every { routeProgress.voiceInstructions } returns null

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
                any(),
                any(),
                Looper.getMainLooper()
            )
        }

        tripSession.stop()
    }

    @Test
    fun stopSessionCallsTripServiceStopService() {
        tripSession.start()

        tripSession.stop()

        verify { locationEngine.removeLocationUpdates(locationCallbackSlot.captured) }
    }

    @Test
    fun stopSessionCallsLocationEngineRemoveLocationUpdates() {
        tripSession.start()
        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        tripSession.stop()

        verify { locationEngine.removeLocationUpdates(locationCallbackSlot.captured) }
    }

    @Test
    fun stopSessionDoesNotClearUpRoute() {
        tripSession.route = route
        tripSession.start()

        tripSession.stop()

        assertEquals(route, tripSession.route)
    }

    @Test
    fun stopTripSessionShouldStopRouteProgress() = coroutineRule.runBlockingTest {
        tripSession.route = route
        tripSession.start()
        tripSession.route = null
        tripSession.stop()

        coVerify(exactly = 1) { navigator.getStatus(any()) }
    }

    @Test
    fun locationObserverSuccess() = coroutineRule.runBlockingTest {
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        updateLocationAndJoin()

        verify { observer.onRawLocationChanged(location) }
        assertEquals(location, tripSession.getRawLocation())

        tripSession.stop()
    }

    @Test
    fun locationObserverSuccessWhenMultipleSamples() = coroutineRule.runBlockingTest {
        every { locationEngineResult.locations } returns listOf(mockk(), location)
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        updateLocationAndJoin()

        verify { observer.onRawLocationChanged(location) }
        assertEquals(location, tripSession.getRawLocation())

        tripSession.stop()
    }

    @Test
    fun locationObserverOnFailure() {
        tripSession.start()

        locationCallbackSlot.captured.onFailure(Exception("location failure"))

        verify(exactly = 0) { locationEngine.removeLocationUpdates(locationCallbackSlot.captured) }

        tripSession.stop()
    }

    @Test
    fun locationObserverImmediate() = coroutineRule.runBlockingTest {
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        updateLocationAndJoin()

        tripSession.registerLocationObserver(observer)

        verify { observer.onRawLocationChanged(location) }

        tripSession.stop()
    }

    @Test
    fun unregisterLocationObserver() = coroutineRule.runBlockingTest {
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        tripSession.unregisterLocationObserver(observer)
        updateLocationAndJoin()
        verify(exactly = 0) { observer.onRawLocationChanged(any()) }

        tripSession.stop()
    }

    @Test
    fun locationPush() = coroutineRule.runBlockingTest {
        tripSession.start()
        updateLocationAndJoin()
        coVerify { navigator.updateLocation(location) }
        tripSession.stop()
    }

    @Test
    fun locationPushWhenMultipleSamples() = coroutineRule.runBlockingTest {
        every { locationEngineResult.locations } returns listOf(mockk(), location)
        tripSession.start()
        updateLocationAndJoin()
        coVerify { navigator.updateLocation(location) }
        tripSession.stop()
    }

    @Test
    fun getStatusImmediatelyAfterUpdateLocation() = coroutineRule.runBlockingTest {
        tripSession.start()

        updateLocationAndJoin()
        val slot = slot<Long>()
        coVerify { navigator.getStatus(capture(slot)) }

        assertTrue("${slot.captured}", slot.captured == DEFAULT_NAVIGATOR_PREDICTION_MILLIS)
    }

    @Test
    fun noLocationUpdateLongerThanAPatienceUnconditionallyGetStatus() =
        coroutineRule.runBlockingTest {
            tripSession.start()

            locationCallbackSlot.captured.onSuccess(locationEngineResult)
            advanceTimeBy(MapboxTripSession.UNCONDITIONAL_STATUS_POLLING_PATIENCE)
            parentJob.cancelAndJoin()

            coVerify(exactly = 2) { navigator.getStatus(any()) }
            tripSession.stop()
        }

    @Test
    fun unconditionalGetStatusRepeated() = coroutineRule.runBlockingTest {
        tripSession.start()

        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        advanceTimeBy(MapboxTripSession.UNCONDITIONAL_STATUS_POLLING_PATIENCE)
        advanceTimeBy(MapboxTripSession.UNCONDITIONAL_STATUS_POLLING_INTERVAL)
        parentJob.cancelAndJoin()

        coVerify(exactly = 3) { navigator.getStatus(any()) }
        tripSession.stop()
    }

    @Test
    fun rawLocationCancelsUnconditionalGetStatusRepetition() = coroutineRule.runBlockingTest {
        tripSession.start()

        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        advanceTimeBy(MapboxTripSession.UNCONDITIONAL_STATUS_POLLING_PATIENCE - 100)
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        advanceTimeBy(MapboxTripSession.UNCONDITIONAL_STATUS_POLLING_INTERVAL - 100)
        parentJob.cancelAndJoin()

        coVerify(exactly = 2) { navigator.getStatus(any()) }
        tripSession.stop()
    }

    @Test
    fun routeProgressObserverSuccess() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)
        updateLocationAndJoin()

        verify { observer.onRouteProgressChanged(routeProgress) }
        assertEquals(routeProgress, tripSession.getRouteProgress())
        tripSession.stop()
    }

    @Test
    fun routeProgressObserverNotCalledWhenInFreeDrive() = coroutineRule.runBlockingTest {
        every { tripStatus.routeProgress } returns null
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)
        updateLocationAndJoin()

        verify(exactly = 0) { observer.onRouteProgressChanged(routeProgress) }
        assertNull(tripSession.getRouteProgress())
        tripSession.stop()
    }

    @Test
    fun routeProgressObserverImmediate() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        updateLocationAndJoin()
        val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)

        verify(exactly = 1) { observer.onRouteProgressChanged(routeProgress) }
        assertEquals(routeProgress, tripSession.getRouteProgress())
        tripSession.stop()
    }

    @Test
    fun routeProgressObserverImmediateEmittedBelongsToCurrentRoute() =
        coroutineRule.runBlockingTest {
            tripSession = MapboxTripSession(
                tripService,
                navigationOptions,
                navigator,
                ThreadController,
                logger = logger,
                accessToken = "pk.1234"
            )
            tripSession.start()
            updateLocationAndJoin()
            tripSession.route = null
            val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
            tripSession.registerRouteProgressObserver(observer)

            verify(exactly = 0) { observer.onRouteProgressChanged(routeProgress) }
            assertEquals(null, tripSession.getRouteProgress())
            tripSession.stop()
        }

    @Test
    fun routeProgressObserverUnregister() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)
        tripSession.unregisterRouteProgressObserver(observer)
        updateLocationAndJoin()

        verify(exactly = 0) { observer.onRouteProgressChanged(routeProgress) }
        tripSession.stop()
    }

    @Test
    fun routeProgressObserverDoubleRegister() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)
        updateLocationAndJoin()
        tripSession.unregisterRouteProgressObserver(observer)
        tripSession.registerRouteProgressObserver(observer)

        verify(exactly = 2) { observer.onRouteProgressChanged(routeProgress) }
        tripSession.stop()
    }

    @Test
    fun offRouteObserverCalledWhenStatusIsDifferentToCurrent() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)

        every { tripStatus.offRoute } returns true
        updateLocationAndJoin()

        verify(exactly = 1) { offRouteObserver.onOffRouteStateChanged(false) }
        verify(exactly = 1) { offRouteObserver.onOffRouteStateChanged(true) }
        verifyOrder {
            offRouteObserver.onOffRouteStateChanged(false)
            offRouteObserver.onOffRouteStateChanged(true)
        }
        tripSession.stop()
    }

    @Test
    fun offRouteObserverNotCalledWhenStatusIsEqualToCurrent() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)

        updateLocationAndJoin()

        verify(exactly = 1) { offRouteObserver.onOffRouteStateChanged(false) }
        verify(exactly = 0) { offRouteObserver.onOffRouteStateChanged(true) }
        tripSession.stop()
    }

    @Test
    fun isOffRouteIsSetToFalseWhenSettingARoute() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.route = route
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)
        every { tripStatus.offRoute } returns true
        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        tripSession.route = route

        parentJob.cancelAndJoin()
        verify(exactly = 2) { offRouteObserver.onOffRouteStateChanged(false) }
        verify(exactly = 1) { offRouteObserver.onOffRouteStateChanged(true) }
        verifyOrder {
            offRouteObserver.onOffRouteStateChanged(false)
            offRouteObserver.onOffRouteStateChanged(true)
            offRouteObserver.onOffRouteStateChanged(false)
        }
        tripSession.stop()
    }

    @Test
    fun isOffRouteIsSetToFalseWhenSettingANullRoute() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.route = route
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)
        every { tripStatus.offRoute } returns true
        locationCallbackSlot.captured.onSuccess(locationEngineResult)

        tripSession.route = null

        parentJob.cancelAndJoin()
        verify(exactly = 2) { offRouteObserver.onOffRouteStateChanged(false) }
        verify(exactly = 1) { offRouteObserver.onOffRouteStateChanged(true) }
        verifyOrder {
            offRouteObserver.onOffRouteStateChanged(false)
            offRouteObserver.onOffRouteStateChanged(true)
            offRouteObserver.onOffRouteStateChanged(false)
        }
        tripSession.stop()
    }

    @Test
    fun enhancedLocationObserverSuccess() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        updateLocationAndJoin()

        verify { observer.onEnhancedLocationChanged(enhancedLocation, keyPoints) }
        assertEquals(enhancedLocation, tripSession.getEnhancedLocation())
        tripSession.stop()
    }

    @Test
    fun enhancedLocationObserverImmediate() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        updateLocationAndJoin()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        verify(exactly = 1) { observer.onEnhancedLocationChanged(enhancedLocation, emptyList()) }
        assertEquals(enhancedLocation, tripSession.getEnhancedLocation())
        tripSession.stop()
    }

    @Test
    fun enhancedLocationObserverUnregister() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        tripSession.unregisterLocationObserver(observer)
        updateLocationAndJoin()
        verify(exactly = 0) { observer.onEnhancedLocationChanged(enhancedLocation, keyPoints) }

        tripSession.stop()
    }

    @Test
    fun getTripService() {
        assertEquals(tripService, tripSession.tripService)
    }

    @Test
    fun getRoute() {
        tripSession.route = route
        assertEquals(route, tripSession.route)
    }

    @Test
    fun setRoute() {
        tripSession.route = route

        coVerify(exactly = 1) { navigator.setRoute(route) }
    }

    @Test
    fun setRoute_nullable() {
        tripSession.route = null

        coVerify(exactly = 1) { navigator.setRoute(null) }
    }

    @Test
    fun checksGetNavigatorStatusIsCalledAfterSettingARouteWhenTripSessionHasStarted() {
        tripSession.start()

        tripSession.route = route

        coVerify(exactly = 1) { navigator.setRoute(route) }
        coVerify(exactly = 1) { navigator.getStatus(any()) }
        coVerifyOrder {
            navigator.setRoute(route)
            navigator.getStatus(any())
        }
    }

    @Test
    fun checksGetNavigatorStatusIsNotCalledAfterSettingARouteIfTripSessionHasNotStarted() {
        tripSession.route = route

        coVerify(exactly = 1) { navigator.setRoute(route) }
        coVerify(exactly = 0) { navigator.getStatus(any()) }
    }

    @Test
    fun checksCancelOngoingUpdateNavigatorStatusDataJobsAreCalledWhenARouteIsSet() {
        tripSession = spyk(
            MapboxTripSession(
                tripService,
                navigationOptions,
                navigator,
                logger = logger,
                accessToken = "pk.1234"
            ),
            recordPrivateCalls = true
        )

        tripSession.route = null

        verify(exactly = 1) { tripSession["cancelOngoingUpdateNavigatorStatusDataJobs"]() }
    }

    @Test
    fun stateObserverImmediateStop() {
        tripSession.registerStateObserver(stateObserver)
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STOPPED) }
    }

    @Test
    fun stateObserverImmediateStart() {
        tripSession.start()
        tripSession.registerStateObserver(stateObserver)
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STARTED) }
    }

    @Test
    fun stateObserverStart() {
        tripSession.registerStateObserver(stateObserver)
        tripSession.start()
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STARTED) }
    }

    @Test
    fun stateObserverStop() {
        tripSession.start()
        tripSession.registerStateObserver(stateObserver)
        tripSession.stop()
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STOPPED) }
    }

    @Test
    fun stateObserverDoubleStart() {
        tripSession.registerStateObserver(stateObserver)
        tripSession.start()
        tripSession.start()
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STARTED) }
    }

    @Test
    fun stateObserverDoubleStop() {
        tripSession.start()
        tripSession.registerStateObserver(stateObserver)
        tripSession.stop()
        tripSession.stop()
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STOPPED) }
    }

    @Test
    fun stateObserverUnregister() {
        tripSession.registerStateObserver(stateObserver)
        clearMocks(stateObserver)
        tripSession.unregisterStateObserver(stateObserver)
        tripSession.start()
        tripSession.stop()
        verify(exactly = 0) { stateObserver.onSessionStateChanged(any()) }
    }

    @Test
    fun unregisterAllLocationObservers() = coroutineRule.runBlockingTest {
        every { routeProgress.bannerInstructions } returns null
        every { routeProgress.voiceInstructions } returns null

        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        tripSession.unregisterAllLocationObservers()

        updateLocationAndJoin()

        verify(exactly = 0) { observer.onRawLocationChanged(location) }
        assertEquals(location, tripSession.getRawLocation())

        tripSession.stop()
    }

    @Test
    fun unregisterAllRouteProgressObservers() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val routeProgressObserver: RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        tripSession.unregisterAllRouteProgressObservers()
        updateLocationAndJoin()

        verify(exactly = 0) { routeProgressObserver.onRouteProgressChanged(any()) }

        tripSession.stop()
    }

    @Test
    fun unregisterAllOffRouteObservers() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)
        tripSession.unregisterAllOffRouteObservers()

        every { tripStatus.offRoute } returns true
        updateLocationAndJoin()

        // registerOffRouteObserver will call onOffRouteStateChanged() on
        // the offRouteObserver so that accounts for the verify 1 time
        // below. However there shouldn't be any additional calls when
        // the locationCallback.onSuccess() is called because the collection
        // of offRouteObservers should be empty.
        verify(exactly = 1) { offRouteObserver.onOffRouteStateChanged(false) }

        tripSession.stop()
    }

    @Test
    fun unregisterAllStateObservers() = coroutineRule.runBlockingTest {
        tripSession.registerStateObserver(stateObserver)
        clearMocks(stateObserver)
        tripSession.unregisterAllStateObservers()

        tripSession.stop()

        verify(exactly = 0) { stateObserver.onSessionStateChanged(any()) }
    }

    @Test
    fun unregisterAllBannerInstructionsObservers() = coroutineRule.runBlockingTest {
        val bannerInstructionsObserver: BannerInstructionsObserver = mockk(relaxUnitFun = true)
        val bannerInstructions: BannerInstructions = mockk()

        every { routeProgress.bannerInstructions } returns bannerInstructions
        every { routeProgress.bannerInstructions?.view() } returns null
        every { routeProgress.voiceInstructions } returns null
        every { tripStatus.offRoute } returns true

        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)

        updateLocationAndJoin()

        tripSession.stop()

        tripSession.start()
        tripSession.unregisterAllBannerInstructionsObservers()

        updateLocationAndJoin()

        verify(exactly = 1) { bannerInstructionsObserver.onNewBannerInstructions(any()) }

        tripSession.stop()
    }

    @Test
    fun unregisterAllVoiceInstructionsObservers() = coroutineRule.runBlockingTest {
        val voiceInstructionsObserver: VoiceInstructionsObserver = mockk(relaxUnitFun = true)
        val voiceInstructions: VoiceInstructions = mockk()
        every { routeProgress.bannerInstructions } returns null
        every { routeProgress.voiceInstructions } returns voiceInstructions
        every { tripStatus.offRoute } returns true

        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        tripSession.registerVoiceInstructionsObserver(voiceInstructionsObserver)

        updateLocationAndJoin()

        tripSession.stop()

        tripSession.start()
        tripSession.unregisterAllVoiceInstructionsObservers()

        updateLocationAndJoin()

        verify(exactly = 1) { voiceInstructionsObserver.onNewVoiceInstructions(any()) }

        tripSession.stop()
    }

    @Test
    fun `route alerts observer gets successfully called`() = coroutineRule.runBlockingTest {
        val routeAlertsObserver: RouteAlertsObserver = mockk(relaxUnitFun = true)
        val routeAlerts: List<RouteAlert> = listOf(mockk())
        coEvery { navigator.setRoute(any(), any()) } returns RouteInitInfo(routeAlerts)
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )

        tripSession.registerRouteAlertsObserver(routeAlertsObserver)
        tripSession.route = mockk(relaxed = true)

        verify(exactly = 1) { routeAlertsObserver.onNewRouteAlerts(routeAlerts) }
    }

    @Test
    fun `route alerts observer gets called only one on duplicate updates`() =
        coroutineRule.runBlockingTest {
            val routeAlertsObserver: RouteAlertsObserver = mockk(relaxUnitFun = true)
            val routeAlerts: List<RouteAlert> = listOf(mockk())
            coEvery { navigator.setRoute(any(), any()) } returns RouteInitInfo(routeAlerts)
            tripSession = MapboxTripSession(
                tripService,
                navigationOptions,
                navigator,
                ThreadController,
                logger = logger,
                accessToken = "pk.1234"
            )

            tripSession.registerRouteAlertsObserver(routeAlertsObserver)
            tripSession.route = mockk(relaxed = true)
            tripSession.route = mockk(relaxed = true)

            verify(exactly = 1) { routeAlertsObserver.onNewRouteAlerts(routeAlerts) }
        }

    @Test
    fun `route alerts observer doesn't get called when unregistered`() =
        coroutineRule.runBlockingTest {
            val routeAlertsObserver: RouteAlertsObserver = mockk(relaxUnitFun = true)
            val routeAlerts: List<RouteAlert> = listOf(mockk())
            coEvery { navigator.setRoute(any(), any()) } returns RouteInitInfo(routeAlerts)
            tripSession = MapboxTripSession(
                tripService,
                navigationOptions,
                navigator,
                ThreadController,
                logger = logger,
                accessToken = "pk.1234"
            )

            tripSession.registerRouteAlertsObserver(routeAlertsObserver)
            tripSession.route = mockk(relaxed = true)
            tripSession.unregisterRouteAlertsObserver(routeAlertsObserver)
            tripSession.route = mockk(relaxed = true)

            verify(exactly = 1) { routeAlertsObserver.onNewRouteAlerts(routeAlerts) }
        }

    @Test
    fun `route alerts observer gets immediately notified`() = coroutineRule.runBlockingTest {
        val routeAlertsObserver: RouteAlertsObserver = mockk(relaxUnitFun = true)
        val routeAlerts: List<RouteAlert> = listOf(mockk())
        coEvery { navigator.setRoute(any(), any()) } returns RouteInitInfo(routeAlerts)
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )

        tripSession.route = mockk(relaxed = true)
        tripSession.registerRouteAlertsObserver(routeAlertsObserver)

        verify(exactly = 1) { routeAlertsObserver.onNewRouteAlerts(routeAlerts) }
    }

    @Test
    fun `route alerts get cleared when route is cleared`() = coroutineRule.runBlockingTest {
        val routeAlertsObserver: RouteAlertsObserver = mockk(relaxUnitFun = true)
        val routeAlerts: List<RouteAlert> = listOf(mockk())
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )

        coEvery { navigator.setRoute(any(), any()) } returns RouteInitInfo(routeAlerts)
        tripSession.route = mockk(relaxed = true)
        tripSession.registerRouteAlertsObserver(routeAlertsObserver)
        coEvery { navigator.setRoute(any(), any()) } returns null
        tripSession.route = null

        verifySequence {
            routeAlertsObserver.onNewRouteAlerts(routeAlerts)
            routeAlertsObserver.onNewRouteAlerts(emptyList())
        }
    }

    @Test
    fun `route alerts observer is notified with null if there's no route`() =
        coroutineRule.runBlockingTest {
            val routeAlertsObserver: RouteAlertsObserver = mockk(relaxUnitFun = true)
            tripSession = MapboxTripSession(
                tripService,
                navigationOptions,
                navigator,
                ThreadController,
                logger = logger,
                accessToken = "pk.1234"
            )

            tripSession.registerRouteAlertsObserver(routeAlertsObserver)

            verify(exactly = 1) { routeAlertsObserver.onNewRouteAlerts(emptyList()) }
        }

    @Test
    fun unregisterAllRouteAlertsObservers() = coroutineRule.runBlockingTest {
        val routeAlertsObserver: RouteAlertsObserver = mockk(relaxUnitFun = true)
        val routeAlerts: List<RouteAlert> = listOf(mockk())
        coEvery { navigator.setRoute(any(), any()) } returns RouteInitInfo(routeAlerts)
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )

        tripSession.registerRouteAlertsObserver(routeAlertsObserver)
        tripSession.route = mockk(relaxed = true)
        tripSession.unregisterAllRouteAlertsObservers()
        tripSession.route = mockk(relaxed = true)

        verify(exactly = 1) { routeAlertsObserver.onNewRouteAlerts(routeAlerts) }
    }

    @Test
    fun guidanceViewURLWithNoAccessToken() = coroutineRule.runBlockingTest {
        val bannerInstructionsObserver: BannerInstructionsObserver = mockk(relaxUnitFun = true)
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponent = getBannerComponent()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf(bannerComponent)

        every { routeProgress.bannerInstructions } returns bannerInstructions
        every { routeProgress.bannerInstructions?.view() } returns bannerView
        every {
            routeProgress.bannerInstructions?.view()?.components()
        } returns bannerComponentsList
        every { routeProgress.voiceInstructions } returns null
        every { tripStatus.offRoute } returns true

        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = null
        )
        tripSession.start()
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)

        updateLocationAndJoin()

        assertEquals(
            "https://api.mapbox.com/guidance-views/v1/1580515200/jct/CB211101?arrow_ids=" +
                "CB21110A&access_token=null",
            bannerComponentsList[0].imageUrl()
        )

        tripSession.unregisterAllBannerInstructionsObservers()
        tripSession.stop()
    }

    @Test
    fun guidanceViewURLWithAccessToken() = coroutineRule.runBlockingTest {
        val bannerInstructionsObserver: BannerInstructionsObserver = mockk(relaxUnitFun = true)
        val bannerInstructions: BannerInstructions = mockk()
        val bannerView: BannerView = mockk()
        val bannerComponent = getBannerComponent()
        val bannerComponentsList: MutableList<BannerComponents> = mutableListOf(bannerComponent)

        every { routeProgress.bannerInstructions } returns bannerInstructions
        every { routeProgress.bannerInstructions?.view() } returns bannerView
        every {
            routeProgress.bannerInstructions?.view()?.components()
        } returns bannerComponentsList
        every { routeProgress.voiceInstructions } returns null
        every { tripStatus.offRoute } returns true

        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)

        updateLocationAndJoin()

        assertEquals(
            "https://api.mapbox.com/guidance-views/v1/1580515200/jct/CB211101?arrow_ids=" +
                "CB21110A&access_token=pk.1234",
            bannerComponentsList[0].imageUrl()
        )

        tripSession.unregisterAllBannerInstructionsObservers()
        tripSession.stop()
    }

    @Test
    fun `map matcher result success`() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        val observer: MapMatcherResultObserver = mockk(relaxUnitFun = true)
        tripSession.registerMapMatcherResultObserver(observer)
        updateLocationAndJoin()

        verify(exactly = 1) { observer.onNewMapMatcherResult(mapMatcherResult) }
        tripSession.stop()
    }

    @Test
    fun `map matcher result immediate`() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        updateLocationAndJoin()
        val observer: MapMatcherResultObserver = mockk(relaxUnitFun = true)
        tripSession.registerMapMatcherResultObserver(observer)

        verify(exactly = 1) { observer.onNewMapMatcherResult(mapMatcherResult) }
        tripSession.stop()
    }

    @Test
    fun `map matcher result cleared on reset`() = coroutineRule.runBlockingTest {
        tripSession = MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger = logger,
            accessToken = "pk.1234"
        )
        tripSession.start()
        updateLocationAndJoin()
        tripSession.stop()
        val observer: MapMatcherResultObserver = mockk(relaxUnitFun = true)
        tripSession.registerMapMatcherResultObserver(observer)

        verify(exactly = 0) { observer.onNewMapMatcherResult(any()) }
    }

    @After
    fun cleanUp() {
        unmockkObject(ThreadController)
        unmockkStatic("com.mapbox.navigation.core.navigator.NavigatorMapperKt")
    }

    private suspend fun updateLocationAndJoin() {
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        parentJob.cancelAndJoin()
    }

    private fun getBannerComponent() =
        BannerComponents.builder()
            .text("some text")
            .type("guidance-view")
            .imageUrl(
                "https://api.mapbox.com/guidance-views/v1/1580515200/" +
                    "jct/CB211101?arrow_ids=CB21110A"
            )
            .build()
}
