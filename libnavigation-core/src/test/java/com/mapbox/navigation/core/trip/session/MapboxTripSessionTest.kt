package com.mapbox.navigation.core.trip.session

import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.internal.utils.isSameRoute
import com.mapbox.navigation.core.internal.utils.isSameUuid
import com.mapbox.navigation.core.navigator.RouteInitInfo
import com.mapbox.navigation.core.navigator.getMapMatcherResult
import com.mapbox.navigation.core.navigator.getRouteInitInfo
import com.mapbox.navigation.core.navigator.getRouteProgressFrom
import com.mapbox.navigation.core.navigator.getTripStatusFrom
import com.mapbox.navigation.core.navigator.mapToDirectionsApi
import com.mapbox.navigation.core.navigator.toFixLocation
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.core.navigator.toLocations
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManager
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.navigator.internal.NativeNavigatorRecreationObserver
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.BannerInstruction
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
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

    private lateinit var tripSession: TripSession

    private val tripService: TripService = mockk(relaxUnitFun = true)
    private val route: DirectionsRoute = mockk(relaxed = true)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    private lateinit var navigationOptions: NavigationOptions
    private val locationCallbackSlot = slot<LocationEngineCallback<LocationEngineResult>>()
    private val locationEngineResult: LocationEngineResult = mockk(relaxUnitFun = true)
    private val location: Location = mockk(relaxed = true)
    private val fixLocation: FixLocation = mockk(relaxed = true)
    private val keyPoints: List<Location> = listOf(mockk(relaxUnitFun = true))
    private val keyFixPoints: List<FixLocation> = listOf(mockk(relaxed = true))

    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val tripStatus: TripStatus = mockk(relaxUnitFun = true)
    private val navigationStatusOrigin: NavigationStatusOrigin = mockk()
    private val navigationStatus: NavigationStatus = mockk(relaxed = true)
    private val logger: Logger = mockk(relaxUnitFun = true)

    private val routeProgress: RouteProgress = mockk()

    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    private val stateObserver: TripSessionStateObserver = mockk(relaxUnitFun = true)

    private val mapMatcherResult: MapMatcherResult = mockk(relaxUnitFun = true)
    private val eHorizonSubscriptionManager: EHorizonSubscriptionManager = mockk(relaxed = true)
    private val navigatorObserverImplSlot = slot<NavigatorObserver>()
    private val navigatorRecreationObserverImplSlot = slot<NativeNavigatorRecreationObserver>()

    @Before
    fun setUp() {
        mockkObject(MapboxNativeNavigatorImpl)
        mockkObject(ThreadController)
        mockkStatic("com.mapbox.navigation.core.navigator.NavigatorMapper")
        mockkStatic("com.mapbox.navigation.core.internal.utils.DirectionsRouteEx")
        mockkStatic("com.mapbox.navigation.core.navigator.LocationEx")
        every { location.toFixLocation() } returns fixLocation
        every { fixLocation.toLocation() } returns location
        every { keyFixPoints.toLocations() } returns keyPoints
        every { ThreadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        navigationOptions = NavigationOptions.Builder(context)
            .locationEngine(locationEngine)
            .build()
        tripSession = buildTripSession()

        coEvery { navigator.updateLocation(any()) } returns false
        coEvery { navigator.setRoute(any()) } returns null
        coEvery { navigator.updateAnnotations(any()) } returns Unit
        every { navigationStatus.getTripStatusFrom(any()) } returns tripStatus

        every { navigationStatus.location } returns fixLocation
        every { navigationStatus.keyPoints } returns keyFixPoints
        every { navigationStatus.routeState } returns RouteState.TRACKING
        every { navigationStatus.bannerInstruction } returns mockk(relaxed = true)

        every { tripStatus.navigationStatus } returns navigationStatus
        every { tripStatus.route } returns route

        every { tripStatus.getMapMatcherResult(any(), any()) } returns mapMatcherResult
        every { routeProgress.bannerInstructions } returns null
        every { routeProgress.voiceInstructions } returns null
        every { routeProgress.currentLegProgress } returns mockk(relaxed = true)
        every { getRouteProgressFrom(any(), any(), any(), any(), any()) } returns routeProgress
        every { route.isSameUuid(any()) } returns false
        every { route.isSameRoute(any()) } returns false
        every { route.requestUuid() } returns "uuid"

        every {
            locationEngine.requestLocationUpdates(
                any(),
                capture(locationCallbackSlot),
                any()
            )
        } answers {}
        every { locationEngineResult.locations } returns listOf(location)
        every {
            navigator.addNavigatorObserver(capture(navigatorObserverImplSlot))
        } answers {}
        every {
            navigator.setNativeNavigatorRecreationObserver(
                capture(navigatorRecreationObserverImplSlot)
            )
        } answers {}
    }

    private fun buildTripSession(): TripSession {
        return MapboxTripSession(
            tripService,
            navigationOptions,
            navigator,
            ThreadController,
            logger,
            eHorizonSubscriptionManager,
        )
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

        coVerify(exactly = 1) {
            navigator.removeNavigatorObserver(navigatorObserverImplSlot.captured)
        }
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
        coVerify { navigator.updateLocation(fixLocation) }
        tripSession.stop()
    }

    @Test
    fun locationPushWhenMultipleSamples() = coroutineRule.runBlockingTest {
        every { locationEngineResult.locations } returns listOf(mockk(), location)
        tripSession.start()
        updateLocationAndJoin()
        coVerify { navigator.updateLocation(fixLocation) }
        tripSession.stop()
    }

    @Test
    fun routeProgressObserverSuccess() = coroutineRule.runBlockingTest {
        every { routeProgress.currentLegProgress } returns null
        tripSession = buildTripSession()
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
        every { getRouteProgressFrom(any(), any(), any(), any(), any()) } returns null
        tripSession = buildTripSession()
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
        tripSession = buildTripSession()
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
            tripSession = buildTripSession()
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
        tripSession = buildTripSession()
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
        every { routeProgress.currentLegProgress } returns null
        tripSession = buildTripSession()
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
        every { routeProgress.currentLegProgress } returns null
        tripSession = buildTripSession()
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)

        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE
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
        tripSession = buildTripSession()
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
        every { routeProgress.currentLegProgress } returns null
        every { route.legs() } returns null
        tripSession = buildTripSession()
        tripSession.route = route
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)
        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)

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
        every { routeProgress.currentLegProgress } returns null
        every { route.legs() } returns null
        tripSession = buildTripSession()
        tripSession.route = route
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)
        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)

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

    /**
     * Test for a workaround for https://github.com/mapbox/mapbox-navigation-android/issues/4727.
     */
    @Test
    fun `banner instruction fallback for missing native events`() = coroutineRule.runBlockingTest {
        val step = mockk<LegStep>(relaxed = true)
        val nativeBanner = mockk<BannerInstruction>(relaxed = true)
        val banner = mockk<BannerInstructions>(relaxed = true)
        every { navigationStatus.legIndex } returns 0
        every { navigationStatus.stepIndex } returns 1
        every { route.legs() } returns listOf(
            mockk {
                every { steps() } returns listOf(
                    mockk(relaxed = true),
                    step
                )
            }
        )
        every { nativeBanner.mapToDirectionsApi(step) } returns banner
        every { MapboxNativeNavigatorImpl.getBannerInstruction(1) } returns nativeBanner
        val bannerInstructionsObserver: BannerInstructionsObserver = mockk(relaxUnitFun = true)

        tripSession = buildTripSession()
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)
        tripSession.route = route
        tripSession.start()
        every { navigationStatus.routeState } returns RouteState.TRACKING
        every { navigationStatus.bannerInstruction } returns null
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)

        verify { getRouteProgressFrom(route, navigationStatus, any(), banner, 0) }
        tripSession.stop()
    }

    @Test
    fun bannerInstructionObserverNotInvokedWhenRouteStateInvalid() = coroutineRule.runBlockingTest {
        val bannerInstructionsObserver: BannerInstructionsObserver = mockk(relaxUnitFun = true)
        val bannerInstructions: BannerInstructions = mockk()

        every { routeProgress.bannerInstructions } returns bannerInstructions
        every { routeProgress.bannerInstructions?.view() } returns null
        every { routeProgress.voiceInstructions } returns null
        every { navigationStatus.routeState } returns RouteState.INVALID

        tripSession = buildTripSession()
        tripSession.start()

        updateLocationAndJoin()

        tripSession.stop()

        verify(exactly = 0) { bannerInstructionsObserver.onNewBannerInstructions(any()) }
    }

    @Test
    fun enhancedLocationObserverSuccess() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        updateLocationAndJoin()

        verify { observer.onEnhancedLocationChanged(location, keyPoints) }
        assertEquals(location, tripSession.getEnhancedLocation())
        tripSession.stop()
    }

    @Test
    fun enhancedLocationObserverImmediate() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start()
        updateLocationAndJoin()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        verify(exactly = 1) { observer.onEnhancedLocationChanged(location, emptyList()) }
        assertEquals(location, tripSession.getEnhancedLocation())
        tripSession.stop()
    }

    @Test
    fun enhancedLocationObserverUnregister() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        tripSession.unregisterLocationObserver(observer)
        updateLocationAndJoin()
        verify(exactly = 0) { observer.onEnhancedLocationChanged(location, keyPoints) }

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
    fun checkNavigatorUpdateAnnotationsWhenRouteIsTheSame() {
        tripSession.start()
        every { route.isSameRoute(any()) } returns true
        every { route.isSameUuid(any()) } returns true

        tripSession.route = route

        coVerify(exactly = 1) { navigator.updateAnnotations(route) }
        coVerify(exactly = 0) { navigator.setRoute(any()) }
    }

    @Test
    fun checkNavigatorUpdateAnnotationsWhenRouteUuidSameButRouteIsAlternative() {
        tripSession.start()
        every { route.isSameRoute(any()) } returns false
        every { route.isSameUuid(any()) } returns true

        tripSession.route = route

        coVerify(exactly = 1) { navigator.setRoute(route) }
        coVerify(exactly = 0) { navigator.updateAnnotations(any()) }
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
        tripSession = buildTripSession()
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
        tripSession = buildTripSession()
        tripSession.start()
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)
        tripSession.unregisterAllOffRouteObservers()

        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE
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
        val step = mockk<LegStep>(relaxed = true)
        every { step.bannerInstructions() } returns listOf(mockk(relaxed = true))
        val leg = mockk<RouteLeg>(relaxed = true)
        every { leg.steps() } returns listOf(step)
        every { route.legs() } returns listOf(leg)
        val bannerInstructionsObserver: BannerInstructionsObserver = mockk(relaxUnitFun = true)
        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE

        tripSession = buildTripSession()
        tripSession.start()
        tripSession.route = route
        tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)
        tripSession.unregisterAllBannerInstructionsObservers()
        updateLocationAndJoin()
        tripSession.stop()
        verify(exactly = 0) { bannerInstructionsObserver.onNewBannerInstructions(any()) }
    }

    @Test
    fun unregisterAllVoiceInstructionsObservers() = coroutineRule.runBlockingTest {
        every { routeProgress.currentLegProgress } returns null
        val voiceInstructionsObserver: VoiceInstructionsObserver = mockk(relaxUnitFun = true)
        val voiceInstructions: VoiceInstructions = mockk()
        every { routeProgress.bannerInstructions } returns null
        every { routeProgress.voiceInstructions } returns voiceInstructions
        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE

        tripSession = buildTripSession()
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
    fun `road objects observer gets successfully called`() = coroutineRule.runBlockingTest {
        val roadObjectsObserver: RoadObjectsOnRouteObserver = mockk(relaxUnitFun = true)
        val roadObjects: List<UpcomingRoadObject> = listOf(mockk())
        val mockedRouteInitInfo: RouteInitInfo = mockk()
        every { mockedRouteInitInfo.roadObjects } returns roadObjects
        val mockedRouteInfo: RouteInfo = mockk()
        every { getRouteInitInfo(mockedRouteInfo) } returns mockedRouteInitInfo
        coEvery { navigator.setRoute(any(), any()) } returns mockedRouteInfo
        tripSession = buildTripSession()

        tripSession.registerRoadObjectsOnRouteObserver(roadObjectsObserver)
        tripSession.route = mockk(relaxed = true)

        verify(exactly = 1) { roadObjectsObserver.onNewRoadObjectsOnTheRoute(roadObjects) }
    }

    @Test
    fun `road objects observer gets called only one on duplicate updates`() =
        coroutineRule.runBlockingTest {
            val roadObjectsObserver: RoadObjectsOnRouteObserver = mockk(relaxUnitFun = true)
            val roadObjects: List<UpcomingRoadObject> = listOf(mockk())
            val mockedRouteInitInfo: RouteInitInfo = mockk()
            every { mockedRouteInitInfo.roadObjects } returns roadObjects
            val mockedRouteInfo: RouteInfo = mockk()
            every { getRouteInitInfo(mockedRouteInfo) } returns mockedRouteInitInfo
            coEvery { navigator.setRoute(any(), any()) } returns mockedRouteInfo
            tripSession = buildTripSession()

            tripSession.registerRoadObjectsOnRouteObserver(roadObjectsObserver)
            tripSession.route = mockk(relaxed = true)
            tripSession.route = mockk(relaxed = true)

            verify(exactly = 1) { roadObjectsObserver.onNewRoadObjectsOnTheRoute(roadObjects) }
        }

    @Test
    fun `road objects observer doesn't get called when unregistered`() =
        coroutineRule.runBlockingTest {
            val roadObjectsObserver: RoadObjectsOnRouteObserver = mockk(relaxUnitFun = true)
            val roadObjects: List<UpcomingRoadObject> = listOf(mockk())
            val mockedRouteInitInfo: RouteInitInfo = mockk()
            every { mockedRouteInitInfo.roadObjects } returns roadObjects
            val mockedRouteInfo: RouteInfo = mockk()
            every { getRouteInitInfo(mockedRouteInfo) } returns mockedRouteInitInfo
            coEvery { navigator.setRoute(any(), any()) } returns mockedRouteInfo
            tripSession = buildTripSession()

            tripSession.registerRoadObjectsOnRouteObserver(roadObjectsObserver)
            tripSession.route = mockk(relaxed = true)
            tripSession.unregisterRoadObjectsOnRouteObserver(roadObjectsObserver)
            tripSession.route = mockk(relaxed = true)

            verify(exactly = 1) { roadObjectsObserver.onNewRoadObjectsOnTheRoute(roadObjects) }
        }

    @Test
    fun `road objects observer gets immediately notified`() = coroutineRule.runBlockingTest {
        val roadObjectsObserver: RoadObjectsOnRouteObserver = mockk(relaxUnitFun = true)
        val roadObjects: List<UpcomingRoadObject> = listOf(mockk())
        val mockedRouteInitInfo: RouteInitInfo = mockk()
        every { mockedRouteInitInfo.roadObjects } returns roadObjects
        val mockedRouteInfo: RouteInfo = mockk()
        every { getRouteInitInfo(mockedRouteInfo) } returns mockedRouteInitInfo
        coEvery { navigator.setRoute(any(), any()) } returns mockedRouteInfo
        tripSession = buildTripSession()

        tripSession.route = mockk(relaxed = true)
        tripSession.registerRoadObjectsOnRouteObserver(roadObjectsObserver)

        verify(exactly = 1) { roadObjectsObserver.onNewRoadObjectsOnTheRoute(roadObjects) }
    }

    @Test
    fun `road objects get cleared when route is cleared`() = coroutineRule.runBlockingTest {
        val roadObjectsObserver: RoadObjectsOnRouteObserver = mockk(relaxUnitFun = true)
        val roadObjects: List<UpcomingRoadObject> = listOf(mockk())
        tripSession = buildTripSession()
        val mockedRouteInitInfo: RouteInitInfo = mockk()
        every { mockedRouteInitInfo.roadObjects } returns roadObjects
        val mockedRouteInfo: RouteInfo = mockk()
        every { getRouteInitInfo(mockedRouteInfo) } returns mockedRouteInitInfo
        coEvery { navigator.setRoute(any(), any()) } returns mockedRouteInfo
        tripSession.route = mockk(relaxed = true)
        tripSession.registerRoadObjectsOnRouteObserver(roadObjectsObserver)
        coEvery { navigator.setRoute(any(), any()) } returns null
        tripSession.route = null

        verifySequence {
            roadObjectsObserver.onNewRoadObjectsOnTheRoute(roadObjects)
            roadObjectsObserver.onNewRoadObjectsOnTheRoute(emptyList())
        }
    }

    @Test
    fun `road objects observer is notified with null if there's no route`() =
        coroutineRule.runBlockingTest {
            val roadObjectsObserver: RoadObjectsOnRouteObserver = mockk(relaxUnitFun = true)
            tripSession = buildTripSession()

            tripSession.registerRoadObjectsOnRouteObserver(roadObjectsObserver)

            verify(exactly = 1) { roadObjectsObserver.onNewRoadObjectsOnTheRoute(emptyList()) }
        }

    @Test
    fun unregisterAllRouteAlertsObservers() = coroutineRule.runBlockingTest {
        val roadObjectsObserver: RoadObjectsOnRouteObserver = mockk(relaxUnitFun = true)
        val roadObjects: List<UpcomingRoadObject> = listOf(mockk())
        val mockedRouteInitInfo: RouteInitInfo = mockk()
        every { mockedRouteInitInfo.roadObjects } returns roadObjects
        val mockedRouteInfo: RouteInfo = mockk()
        every { getRouteInitInfo(mockedRouteInfo) } returns mockedRouteInitInfo
        coEvery { navigator.setRoute(any(), any()) } returns mockedRouteInfo
        tripSession = buildTripSession()

        tripSession.registerRoadObjectsOnRouteObserver(roadObjectsObserver)
        tripSession.route = mockk(relaxed = true)
        tripSession.unregisterAllRoadObjectsOnRouteObservers()
        tripSession.route = mockk(relaxed = true)

        verify(exactly = 1) { roadObjectsObserver.onNewRoadObjectsOnTheRoute(roadObjects) }
    }

    @Test
    fun `map matcher result success`() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start()
        val observer: MapMatcherResultObserver = mockk(relaxUnitFun = true)
        tripSession.registerMapMatcherResultObserver(observer)
        updateLocationAndJoin()

        verify(exactly = 1) { observer.onNewMapMatcherResult(mapMatcherResult) }
        tripSession.stop()
    }

    @Test
    fun `map matcher result immediate`() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start()
        updateLocationAndJoin()
        val observer: MapMatcherResultObserver = mockk(relaxUnitFun = true)
        tripSession.registerMapMatcherResultObserver(observer)

        verify(exactly = 1) { observer.onNewMapMatcherResult(mapMatcherResult) }
        tripSession.stop()
    }

    @Test
    fun `map matcher result cleared on reset`() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start()
        updateLocationAndJoin()
        tripSession.stop()
        val observer: MapMatcherResultObserver = mockk(relaxUnitFun = true)
        tripSession.registerMapMatcherResultObserver(observer)

        verify(exactly = 0) { observer.onNewMapMatcherResult(any()) }
    }

    @Test
    fun `eHorizonObserver added to subscriptionManager on registerEHorizonObserver`() {
        tripSession = buildTripSession()
        tripSession.start()
        val observer: EHorizonObserver = mockk(relaxUnitFun = true)

        tripSession.registerEHorizonObserver(observer)

        verify(exactly = 1) { eHorizonSubscriptionManager.registerObserver(observer) }
        tripSession.stop()
    }

    @Test
    fun `eHorizonObserver removed from subscriptionManager on unregisterObserver`() {
        tripSession = buildTripSession()
        tripSession.start()
        val observer: EHorizonObserver = mockk(relaxUnitFun = true)

        tripSession.unregisterEHorizonObserver(observer)

        verify(exactly = 1) { eHorizonSubscriptionManager.unregisterObserver(observer) }
        tripSession.stop()
    }

    @Test
    fun `all eHorizonObservers removed from subscriptionManager on unregisterAllObservers`() {
        tripSession = buildTripSession()
        tripSession.start()

        tripSession.unregisterAllEHorizonObservers()

        verify(exactly = 1) { eHorizonSubscriptionManager.unregisterAllObservers() }
        tripSession.stop()
    }

    @Test
    fun `when session is started and navigator is recreated observer is reset`() {
        tripSession = buildTripSession()
        tripSession.start()

        navigatorRecreationObserverImplSlot.captured.onNativeNavigatorRecreated()

        verify(exactly = 2) { navigator.addNavigatorObserver(any()) }
        tripSession.stop()
    }

    @Test
    fun `when session is not started and navigator is recreated observer is not reset`() {
        tripSession = buildTripSession()

        navigatorRecreationObserverImplSlot.captured.onNativeNavigatorRecreated()

        verify(exactly = 0) { navigator.addNavigatorObserver(any()) }
    }

    @Test
    fun `when not empty fallback observers and navigator is recreated fallback observer reset`() {
        tripSession = buildTripSession()
        tripSession.registerFallbackVersionsObserver(mockk())

        navigatorRecreationObserverImplSlot.captured.onNativeNavigatorRecreated()

        verify(exactly = 2) { navigator.setFallbackVersionsObserver(any()) }
    }

    @Test
    fun `when no fallback observers and navigator is recreated fallback observer is not reset`() {
        tripSession = buildTripSession()

        navigatorRecreationObserverImplSlot.captured.onNativeNavigatorRecreated()

        verify(exactly = 0) { navigator.setFallbackVersionsObserver(any()) }
    }

    @After
    fun cleanUp() {
        unmockkObject(MapboxNativeNavigatorImpl)
        unmockkObject(ThreadController)
        unmockkStatic("com.mapbox.navigation.core.navigator.NavigatorMapper")
        unmockkStatic("com.mapbox.navigation.core.internal.utils.DirectionsRouteEx")
        unmockkStatic("com.mapbox.navigation.core.navigator.LocationEx")
    }

    private suspend fun updateLocationAndJoin() {
        locationCallbackSlot.captured.onSuccess(locationEngineResult)
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)
        parentJob.cancelAndJoin()
    }
}
