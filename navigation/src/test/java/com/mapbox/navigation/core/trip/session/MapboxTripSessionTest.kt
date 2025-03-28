package com.mapbox.navigation.core.trip.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
import com.mapbox.navigation.base.internal.route.refreshNativePeer
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.LegWaypoint
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.core.navigator.getCurrentBannerInstructions
import com.mapbox.navigation.core.navigator.getLocationMatcherResult
import com.mapbox.navigation.core.navigator.getRouteProgressFrom
import com.mapbox.navigation.core.navigator.getTripStatusFrom
import com.mapbox.navigation.core.navigator.toFixLocation
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.core.navigator.toLocations
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigation.core.trip.session.eh.EHorizonSubscriptionManager
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.NativeNavigatorRecreationObserver
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.navigator.internal.utils.calculateRemainingWaypoints
import com.mapbox.navigation.navigator.internal.utils.getCurrentLegDestination
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.factories.createNavigationStatus
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.FixLocation
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouteAlternative
import com.mapbox.navigator.RouteState
import com.mapbox.navigator.SetRoutesReason
import com.mapbox.navigator.SetRoutesResult
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var tripSession: MapboxTripSession

    private val tripService: TripService = mockk(relaxUnitFun = true) {
        every { hasServiceStarted() } returns false
    }
    private lateinit var locationUpdateAnswers: (Location) -> Unit
    private val tripSessionLocationEngine: TripSessionLocationEngine = mockk(relaxUnitFun = true) {
        every { startLocationUpdates(any(), captureLambda()) } answers {
            locationUpdateAnswers = secondArg()
        }
    }
    private val routes: List<NavigationRoute> = listOf(
        mockk(relaxed = true) {
            every { id } returns "abc#0"
        },
    )
    private val legIndex = 2
    private val geometryIndex = 23
    private val routeProgressData = RouteProgressData(
        legIndex,
        geometryIndex,
        66,
    )
    private val setRoutesInfo = SetRoutes.NewRoutes(legIndex)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var navigationOptions: NavigationOptions
    private val location: Location = mockLocation()
    private val fixLocation: FixLocation = mockk(relaxed = true)
    private val keyPoints: List<Location> = listOf(mockk(relaxUnitFun = true))
    private val keyFixPoints: List<FixLocation> = listOf(mockk(relaxed = true))

    private val navigator: MapboxNativeNavigator = mockk(relaxUnitFun = true)
    private val tripStatus: TripStatus = mockk(relaxUnitFun = true)
    private val navigationStatusOrigin: NavigationStatusOrigin = mockk()
    private val navigationStatus: NavigationStatus = mockk(relaxed = true)
    private val routeProgress: RouteProgress = mockk()
    private val threadController = spyk<ThreadController>()
    private val bannerInstructionEvent = spyk(BannerInstructionEvent())

    private val parentJob = SupervisorJob()
    private val testScope = CoroutineScope(parentJob + coroutineRule.testDispatcher)

    private val stateObserver: TripSessionStateObserver = mockk(relaxUnitFun = true)

    private val locationMatcherResult: LocationMatcherResult = mockk(relaxUnitFun = true) {
        every { enhancedLocation } returns location
    }
    private val eHorizonSubscriptionManager: EHorizonSubscriptionManager = mockk(relaxed = true)
    private val navigatorObserverImplSlot = slot<NavigatorObserver>()
    private val navigatorRecreationObserverImplSlot = slot<NativeNavigatorRecreationObserver>()

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.core.navigator.NavigatorMapper")
        mockkStatic("com.mapbox.navigation.core.navigator.LocationEx")
        mockkStatic("com.mapbox.navigation.navigator.internal.utils.TripStatusEx")
        mockkObject(BannerInstructionEvent.Companion)
        mockkObject(RoadObjectFactory)
        mockkStatic(LocationServiceFactory::class)
        every { LocationServiceFactory.getOrCreate() } returns mockk {
            every { getDeviceLocationProvider(null) } returns ExpectedFactory.createValue(mockk())
        }
        every { location.toFixLocation() } returns fixLocation
        every { fixLocation.toLocation() } returns location
        every { keyFixPoints.toLocations() } returns keyPoints
        every { threadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { BannerInstructionEvent.invoke() } returns bannerInstructionEvent
        navigationOptions = NavigationOptions.Builder(context).build()
        tripSession = buildTripSession()

        coEvery { navigator.updateLocation(any()) } returns false
        coEvery { navigator.setRoutes(any(), any(), any(), any()) } returns createSetRouteResult()
        coEvery { navigator.setAlternativeRoutes(any()) } returns listOf()
        coEvery {
            navigator.refreshRoute(any())
        } returns ExpectedFactory.createValue(listOf())
        every { navigationStatus.getTripStatusFrom(any()) } returns tripStatus

        every { navigationStatus.location } returns fixLocation
        every { navigationStatus.keyPoints } returns keyFixPoints
        every { navigationStatus.routeState } returns RouteState.TRACKING
        every { navigationStatus.bannerInstruction } returns mockk(relaxed = true)

        every { tripStatus.navigationStatus } returns navigationStatus
        every { tripStatus.route } returns routes[0]

        every {
            tripStatus.getLocationMatcherResult(any(), any(), any())
        } returns locationMatcherResult
        every { routeProgress.bannerInstructions } returns null
        every { routeProgress.voiceInstructions } returns null
        every { routeProgress.currentLegProgress } returns mockk(relaxed = true)
        every {
            getRouteProgressFrom(any(), any(), any(), any(), any(), any(), any(), any())
        } returns routeProgress
        every { routeProgress.currentState } returns RouteProgressState.TRACKING
        every { routes[0].responseUUID } returns "uuid"

        every {
            navigator.addNavigatorObserver(capture(navigatorObserverImplSlot))
        } answers {}
        every {
            navigator.setNativeNavigatorRecreationObserver(
                capture(navigatorRecreationObserverImplSlot),
            )
        } answers {}
    }

    private fun buildTripSession(): MapboxTripSession {
        return MapboxTripSession(
            tripService,
            tripSessionLocationEngine,
            navigator,
            threadController,
            eHorizonSubscriptionManager,
        )
    }

    @Test
    fun startSessionWithService() {
        every { tripService.hasServiceStarted() } returns true

        tripSession.start(true)

        assertTrue(tripSession.isRunningWithForegroundService())

        verifyOrder {
            navigator.startNavigationSession()
            tripService.startService()
            tripSessionLocationEngine.startLocationUpdates(any(), any())
        }

        tripSession.stop()
    }

    @Test
    fun startSessionWithoutTripService() {
        every { tripService.hasServiceStarted() } returns false

        tripSession.start(false)

        assertFalse(tripSession.isRunningWithForegroundService())
        verify(exactly = 0) { tripService.startService() }
        verifyOrder {
            navigator.startNavigationSession()
            tripSessionLocationEngine.startLocationUpdates(any(), any())
        }

        tripSession.stop()
    }

    @Test
    fun startSessionWithTripServiceCallStartAgain() {
        tripSession.start(true)
        tripSession.start(false)
        every { tripService.hasServiceStarted() } returns true
        assertTrue(tripSession.isRunningWithForegroundService())
        tripSession.stop()

        verifyOrder {
            navigator.addNavigatorObserver(any())
            navigator.startNavigationSession()
            tripService.startService()
            tripSessionLocationEngine.startLocationUpdates(any(), any())
            tripSessionLocationEngine.startLocationUpdates(any(), any())
            navigator.stopNavigationSession()
            navigator.removeNavigatorObserver(any())
            tripSession.stop()
            tripSessionLocationEngine.stopLocationUpdates()
        }
    }

    @Test
    fun startSessionWithReplayCallStartAgain() {
        tripSession.start(true, withReplayEnabled = false)
        tripSession.start(true, withReplayEnabled = true)
        tripSession.start(true, withReplayEnabled = false)
        tripSession.stop()

        verifyOrder {
            navigator.addNavigatorObserver(any())
            navigator.startNavigationSession()
            tripService.startService()
            tripSessionLocationEngine.startLocationUpdates(false, any())
            tripSessionLocationEngine.startLocationUpdates(true, any())
            tripSessionLocationEngine.startLocationUpdates(false, any())
            navigator.stopNavigationSession()
            navigator.removeNavigatorObserver(any())
            tripSession.stop()
            tripSessionLocationEngine.stopLocationUpdates()
        }
    }

    @Test
    fun stopSessionCallsTripServiceStopService() {
        tripSession.start(true)

        tripSession.stop()

        verify { tripService.stopService() }
    }

    @Test
    fun stopSessionStoppingNativeNavigationSession() {
        tripSession.start(true)

        tripSession.stop()

        verify(exactly = 1) { navigator.stopNavigationSession() }
    }

    @Test
    fun stopSessionCallsLocationEngineRemoveLocationUpdates() {
        tripSession.start(true)
        locationUpdateAnswers.invoke(location)

        tripSession.stop()

        verify { tripSessionLocationEngine.stopLocationUpdates() }
    }

    @Test
    fun stopSessionDoesNotClearUpRoute() = coroutineRule.runBlockingTest {
        tripSession.setRoutes(routes, setRoutesInfo)
        tripSession.start(true)

        tripSession.stop()

        assertEquals(routes.first(), tripSession.primaryRoute)
    }

    @Test
    fun stopTripSessionShouldStopRouteProgress() = coroutineRule.runBlockingTest {
        tripSession.setRoutes(routes, setRoutesInfo)
        tripSession.start(true)
        tripSession.setRoutes(
            emptyList(),
            SetRoutes.CleanUp,
        )
        tripSession.stop()

        coVerify(exactly = 1) {
            navigator.removeNavigatorObserver(navigatorObserverImplSlot.captured)
        }
    }

    @Test
    fun nextWaypointsAreCalculated() = coroutineRule.runBlockingTest {
        val nextLegWaypoint = mockk<LegWaypoint>()
        tripSession.start(true)
        every { tripStatus.calculateRemainingWaypoints() } returns 5
        every { tripStatus.getCurrentLegDestination(routes[0]) } returns nextLegWaypoint

        updateLocationAndJoin()

        verify {
            getRouteProgressFrom(
                routes[0],
                navigationStatus,
                5,
                any(),
                any(),
                any(),
                any(),
                nextLegWaypoint,
            )
        }
    }

    @Test
    fun locationObserverSuccess() = coroutineRule.runBlockingTest {
        tripSession.start(true)
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        updateLocationAndJoin()

        verify { observer.onNewRawLocation(location) }
        assertEquals(location, tripSession.getRawLocation())

        tripSession.stop()
    }

    @Test
    fun locationObserverSuccessWhenMultipleSamples() = coroutineRule.runBlockingTest {
        tripSession.start(true)
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        locationUpdateAnswers.invoke(mockLocation())
        updateLocationAndJoin()
        verify { observer.onNewRawLocation(location) }
        assertEquals(location, tripSession.getRawLocation())

        tripSession.stop()
    }

    @Test
    fun locationObserverImmediate() = coroutineRule.runBlockingTest {
        tripSession.start(true)
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        updateLocationAndJoin()

        tripSession.registerLocationObserver(observer)

        verify { observer.onNewRawLocation(location) }

        tripSession.stop()
    }

    @Test
    fun unregisterLocationObserver() = coroutineRule.runBlockingTest {
        tripSession.start(true)
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        tripSession.unregisterLocationObserver(observer)
        updateLocationAndJoin()
        verify(exactly = 0) {
            observer.onNewRawLocation(any())
            observer.onNewLocationMatcherResult(any())
        }

        tripSession.stop()
    }

    @Test
    fun locationPush() = coroutineRule.runBlockingTest {
        tripSession.start(true)
        updateLocationAndJoin()
        coVerify { navigator.updateLocation(fixLocation) }
        tripSession.stop()
    }

    @Test
    fun locationPushWhenMultipleSamples() = coroutineRule.runBlockingTest {
        tripSession.start(true)
        locationUpdateAnswers.invoke(mockLocation())
        locationUpdateAnswers.invoke(mockLocation())
        updateLocationAndJoin()
        coVerify { navigator.updateLocation(fixLocation) }
        tripSession.stop()
    }

    @Test
    fun routeProgressObserverSuccess() = coroutineRule.runBlockingTest {
        every { routeProgress.currentLegProgress } returns null
        tripSession = buildTripSession()
        tripSession.start(true)
        val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
        tripSession.registerRouteProgressObserver(observer)
        updateLocationAndJoin()

        verify { observer.onRouteProgressChanged(routeProgress) }
        assertEquals(routeProgress, tripSession.getRouteProgress())
        tripSession.stop()
    }

    @Test
    fun routeProgressObserverNotCalledWhenInFreeDrive() = coroutineRule.runBlockingTest {
        every {
            getRouteProgressFrom(any(), any(), any(), any(), any(), any(), any(), any())
        } returns null
        tripSession = buildTripSession()
        tripSession.start(true)
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
        tripSession.start(true)
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
            tripSession.start(true)
            updateLocationAndJoin()
            tripSession.setRoutes(
                emptyList(),
                SetRoutes.CleanUp,
            )
            val observer: RouteProgressObserver = mockk(relaxUnitFun = true)
            tripSession.registerRouteProgressObserver(observer)

            verify(exactly = 0) { observer.onRouteProgressChanged(routeProgress) }
            assertEquals(null, tripSession.getRouteProgress())
            tripSession.stop()
        }

    @Test
    fun routeProgressObserverUnregister() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start(true)
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
        tripSession.start(true)
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
        tripSession.start(true)
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
        tripSession.start(true)
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
        every { routes.first().directionsRoute.legs() } returns null
        tripSession = buildTripSession()
        tripSession.setRoutes(routes, setRoutesInfo)
        tripSession.start(true)
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)
        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE
        locationUpdateAnswers.invoke(mockLocation())
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)

        tripSession.setRoutes(
            routes,
            SetRoutes.Reroute(1),
        )

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
        every { routes.first().directionsRoute.legs() } returns null
        tripSession = buildTripSession()
        tripSession.setRoutes(routes, setRoutesInfo)
        tripSession.start(true)
        val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
        tripSession.registerOffRouteObserver(offRouteObserver)
        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE
        locationUpdateAnswers.invoke(mockLocation())
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)

        tripSession.setRoutes(
            emptyList(),
            SetRoutes.CleanUp,
        )

        parentJob.cancelAndJoin()
//        verify(exactly = 2) { offRouteObserver.onOffRouteStateChanged(false) }
//        verify(exactly = 1) { offRouteObserver.onOffRouteStateChanged(true) }
        verifyOrder {
            offRouteObserver.onOffRouteStateChanged(false)
            offRouteObserver.onOffRouteStateChanged(true)
            offRouteObserver.onOffRouteStateChanged(false)
        }
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
        tripSession.start(true)

        updateLocationAndJoin()

        tripSession.stop()

        verify(exactly = 0) { bannerInstructionsObserver.onNewBannerInstructions(any()) }
    }

    @Test
    fun getTripService() {
        assertEquals(tripService, tripSession.tripService)
    }

    @Test
    fun setRoutesUsesCorrectArguments() = coroutineRule.runBlockingTest {
        val expectedAlternatives = listOf(mockk<RouteAlternative>(), mockk())
        coEvery {
            navigator.setRoutes(any(), any(), any(), any())
        } returns createSetRouteResult(expectedAlternatives)
        val alternative: NavigationRoute = mockk {
            every { id } returns "abc#0"
        }
        tripSession.setRoutes(routes + alternative, setRoutesInfo)

        coVerify(exactly = 1) {
            navigator.setRoutes(
                routes.first(),
                setRoutesInfo.legIndex,
                listOf(alternative),
                SetRoutesReason.NEW_ROUTE,
            )
        }
    }

    @Test
    fun setRoutesReturnsValue() = coroutineRule.runBlockingTest {
        val expectedAlternatives = listOf(mockk<RouteAlternative>(), mockk())
        coEvery {
            navigator.setRoutes(any(), any(), any(), any())
        } returns createSetRouteResult(expectedAlternatives)
        val alternative: NavigationRoute = mockk {
            every { id } returns "abc#0"
        }
        val actual = tripSession.setRoutes(routes + alternative, setRoutesInfo)
        assertEquals(expectedAlternatives, (actual as NativeSetRouteValue).nativeAlternatives)
    }

    @Test
    fun setRoutesReturnsError() = coroutineRule.runBlockingTest {
        val error = "some error"
        coEvery { navigator.setRoutes(any(), any(), any(), any()) } returns createSetRouteError(
            error,
        )
        val alternative: NavigationRoute = mockk {
            every { id } returns "abc#0"
        }
        val actual = tripSession.setRoutes(routes + alternative, setRoutesInfo)
        assertEquals(error, (actual as NativeSetRouteError).error)
    }

    @Test
    fun setRoute_nullable() = coroutineRule.runBlockingTest {
        tripSession.setRoutes(
            emptyList(),
            SetRoutes.CleanUp,
        )

        coVerify(exactly = 1) {
            navigator.setRoutes(
                null,
                reason = SetRoutesReason.CLEAN_UP,
            )
        }
    }

    @Test
    fun checkNavigatorRefreshRouteWhenReasonIsRefresh() =
        coroutineRule.runBlockingTest {
            val refreshedRoutes = listOf(
                mockk<NavigationRoute>(relaxed = true) {
                    every { id } returns "id0"
                },
                mockk<NavigationRoute>(relaxed = true) {
                    every { id } returns "id1"
                },
            )
            tripSession.start(true)

            tripSession.setRoutes(
                refreshedRoutes,
                SetRoutes.RefreshRoutes(RouteProgressData(0, 0, null)),
            )

            coVerifyOrder {
                navigator.refreshRoute(refreshedRoutes[1])
                navigator.refreshRoute(refreshedRoutes[0])
            }
            refreshedRoutes.forEach {
                coVerify(exactly = 1) { navigator.refreshRoute(it) }
            }
            coVerify(exactly = 0) {
                navigator.setRoutes(
                    any(),
                    reason = SetRoutesReason.NEW_ROUTE,
                )
            }
            coVerify(exactly = 0) { navigator.setAlternativeRoutes(any()) }
        }

    @Test
    fun `verify only alternatives are updated when reason is ROUTES_UPDATE_REASON_ALTERNATIVE`() =
        coroutineRule.runBlockingTest {
            tripSession.start(true)

            val alternative = mockk<NavigationRoute> {
                every { id } returns "abc#0"
            }
            tripSession.setRoutes(
                routes + alternative,
                SetRoutes.Alternatives(2),
            )

            coVerify(exactly = 0) {
                navigator.setRoutes(routes.first(), 2, any(), any())
            }
            coVerify(exactly = 0) { navigator.refreshRoute(any()) }
        }

    @Test
    fun `route set result - native alternatives delivered when only alternatives are updated`() =
        coroutineRule.runBlockingTest {
            tripSession.start(true)

            val alternative = mockk<NavigationRoute> {
                every { id } returns "abc#0"
            }
            val nativeAlternatives = listOf(mockk<RouteAlternative>())
            coEvery {
                navigator.setAlternativeRoutes(listOf(alternative))
            } returns nativeAlternatives
            val result = tripSession.setRoutes(
                routes + alternative,
                SetRoutes.Alternatives(2),
            )

            assertEquals(nativeAlternatives, (result as NativeSetRouteValue).nativeAlternatives)
            assertEquals(routes + alternative, result.routes)
        }

    @Test
    fun `route set result - native alternatives delivered when routes are updated`() =
        coroutineRule.runBlockingTest {
            tripSession.start(true)

            val alternative = mockk<NavigationRoute> {
                every { id } returns "abc#0"
            }
            val nativeAlternatives = listOf(mockk<RouteAlternative>())
            coEvery {
                navigator.setRoutes(any(), any(), listOf(alternative), any())
            } returns createSetRouteResult(nativeAlternatives)
            val result = tripSession.setRoutes(
                routes + alternative,
                SetRoutes.NewRoutes(legIndex),
            )

            assertEquals(nativeAlternatives, (result as NativeSetRouteValue).nativeAlternatives)
            assertEquals(routes + alternative, result.routes)
        }

    @Test
    fun `route set result - empty native alternatives delivered when routes are cleared`() =
        coroutineRule.runBlockingTest {
            tripSession.start(true)

            coEvery {
                navigator.setAlternativeRoutes(emptyList())
            } returns emptyList()
            val result = tripSession.setRoutes(
                emptyList(),
                SetRoutes.CleanUp,
            )

            assertTrue((result as NativeSetRouteValue).nativeAlternatives.isEmpty())
            assertEquals(emptyList<NavigationRoute>(), result.routes)
        }

    @Test
    fun `route set result - native alternatives delivered when reroute`() =
        coroutineRule.runBlockingTest {
            tripSession.start(true)

            val alternative = mockk<NavigationRoute> {
                every { id } returns "abc#0"
            }
            val nativeAlternatives = listOf(mockk<RouteAlternative>())
            coEvery {
                navigator.setRoutes(any(), 1, eq(listOf(alternative)), any())
            } returns createSetRouteResult(nativeAlternatives = nativeAlternatives)
            val result = tripSession.setRoutes(
                routes + alternative,
                SetRoutes.Reroute(1),
            )

            assertEquals(nativeAlternatives, (result as NativeSetRouteValue).nativeAlternatives)
            assertEquals(routes + alternative, result.routes)
        }

    @Test
    fun `route set result - native alternatives returned successfully for refresh`() =
        coroutineRule.runBlockingTest {
            val mockAlternativesMetadata1 = listOf<RouteAlternative>(mockk())
            val mockAlternativesMetadata2 = listOf<RouteAlternative>(mockk(), mockk())
            val refreshedRoutes = listOf(
                mockk<NavigationRoute>(relaxed = true) {
                    every { id } returns "id0"
                },
                mockk<NavigationRoute>(relaxed = true) {
                    every { id } returns "id1"
                },
            )
            coEvery {
                navigator.refreshRoute(refreshedRoutes[0])
            } returns ExpectedFactory.createValue(mockAlternativesMetadata2)
            coEvery {
                navigator.refreshRoute(refreshedRoutes[1])
            } returns ExpectedFactory.createValue(mockAlternativesMetadata1)

            refreshedRoutes.forEachIndexed { i, route ->
                every { route.refreshNativePeer() } returns refreshedRoutes[i]
            }

            tripSession.start(true)
            val result = tripSession.setRoutes(
                refreshedRoutes,
                SetRoutes.RefreshRoutes(routeProgressData),
            )

            assertEquals(
                mockAlternativesMetadata2,
                (result as NativeSetRouteValue).nativeAlternatives,
            )

            result.routes.forEachIndexed { i, route ->
                assertTrue(route === refreshedRoutes[i])
            }
            assertEquals(tripSession.primaryRoute, refreshedRoutes.first())
        }

    @Test
    fun `refresh route updates primary route only when all routes are refreshed`() =
        coroutineRule.runBlockingTest {
            every {
                getRouteProgressFrom(any(), any(), any(), any(), any(), any(), any(), any())
            } answers {
                callOriginal()
            }
            val progressObserver = mockk<RouteProgressObserver>(relaxed = true)
            val routeProgresses = mutableListOf<RouteProgress>()
            every { progressObserver.onRouteProgressChanged(capture(routeProgresses)) } just Runs
            val mockAlternativesMetadata1 = listOf<RouteAlternative>(mockk())
            val mockAlternativesMetadata2 = listOf<RouteAlternative>(mockk(), mockk())
            val originalRoute1 = mockk<NavigationRoute>(relaxed = true) {
                every { id } returns "id0"
            }
            val originalRoute2 = mockk<NavigationRoute>(relaxed = true) {
                every { id } returns "id0"
            }
            val refreshedRoute1 = mockk<NavigationRoute>(relaxed = true) {
                every { id } returns "id0"
            }
            tripSession.start(true)
            val navigatorObserverSlot = slot<NavigatorObserver>()
            verify { navigator.addNavigatorObserver(capture(navigatorObserverSlot)) }
            val navigatorObserver = navigatorObserverSlot.captured
            tripSession.registerRouteProgressObserver(progressObserver)
            tripSession.setRoutes(
                listOf(originalRoute1, originalRoute2),
                SetRoutes.NewRoutes(0),
            )

            val refreshedRoutes = listOf(
                refreshedRoute1,
                mockk<NavigationRoute>(relaxed = true) {
                    every { id } returns "id1"
                },
            )
            coEvery {
                navigator.refreshRoute(refreshedRoutes[0])
            } coAnswers {
                delay(300)
                ExpectedFactory.createValue(mockAlternativesMetadata2)
            }
            coEvery {
                navigator.refreshRoute(refreshedRoutes[1])
            } coAnswers {
                delay(500)
                ExpectedFactory.createValue(mockAlternativesMetadata1)
            }

            refreshedRoutes.forEachIndexed { i, route ->
                every { route.refreshNativePeer() } returns refreshedRoutes[i]
            }

            tripSession.start(true)
            coroutineRule.testDispatcher.pauseDispatcher()
            val job = coroutineRule.coroutineScope.launch {
                tripSession.setRoutes(
                    refreshedRoutes,
                    SetRoutes.RefreshRoutes(routeProgressData),
                )
            }

            navigatorObserver.onStatus(
                NavigationStatusOrigin.UNCONDITIONAL,
                createNavigationStatus(),
            )
            assertTrue(routeProgresses.last().navigationRoute === originalRoute1)

            coroutineRule.testDispatcher.advanceTimeBy(501)

            navigatorObserver.onStatus(
                NavigationStatusOrigin.UNCONDITIONAL,
                createNavigationStatus(),
            )
            assertTrue(routeProgresses.last().navigationRoute === originalRoute1)

            coroutineRule.testDispatcher.advanceTimeBy(301)
            assertTrue(job.isCompleted)

            navigatorObserver.onStatus(
                NavigationStatusOrigin.UNCONDITIONAL,
                createNavigationStatus(),
            )
            assertTrue(routeProgresses.last().navigationRoute === refreshedRoute1)

            coroutineRule.testDispatcher.resumeDispatcher()
        }

    @Test
    fun `route set result - native alternatives successful for primary and failed for alternatives`() =
        coroutineRule.runBlockingTest {
            val error = "some error"
            val mockAlternativesMetadata1 = listOf<RouteAlternative>(mockk())
            val refreshedRoutes = listOf(
                mockk<NavigationRoute>(relaxed = true) {
                    every { id } returns "id0"
                },
                mockk<NavigationRoute>(relaxed = true) {
                    every { id } returns "id1"
                },
            )
            coEvery {
                navigator.refreshRoute(refreshedRoutes[0])
            } returns ExpectedFactory.createValue(mockAlternativesMetadata1)
            coEvery {
                navigator.refreshRoute(refreshedRoutes[1])
            } returns ExpectedFactory.createError(error)

            refreshedRoutes.forEachIndexed { i, route ->
                every { route.refreshNativePeer() } returns refreshedRoutes[i]
            }

            tripSession.start(true)
            val result = tripSession.setRoutes(
                refreshedRoutes,
                SetRoutes.RefreshRoutes(routeProgressData),
            )

            assertEquals(
                mockAlternativesMetadata1,
                (result as NativeSetRouteValue).nativeAlternatives,
            )

            result.routes.forEachIndexed { i, route ->
                assertTrue(route === refreshedRoutes[i])
            }
            assertEquals(tripSession.primaryRoute, refreshedRoutes.first())
        }

    @Test
    fun `route set result - native alternatives not returned for refresh`() =
        coroutineRule.runBlockingTest {
            val error = "some error"
            coEvery {
                navigator.refreshRoute(any())
            } returns ExpectedFactory.createError(error)

            tripSession.start(true)
            val result = tripSession.setRoutes(
                routes,
                SetRoutes.RefreshRoutes(routeProgressData),
            )

            assertEquals(error, (result as NativeSetRouteError).error)
        }

    @Test
    fun stateObserverImmediateStop() {
        tripSession.registerStateObserver(stateObserver)
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STOPPED) }
    }

    @Test
    fun stateObserverImmediateStart() {
        tripSession.start(true)
        tripSession.registerStateObserver(stateObserver)
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STARTED) }
    }

    @Test
    fun stateObserverStart() {
        tripSession.registerStateObserver(stateObserver)
        tripSession.start(true)
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STARTED) }
    }

    @Test
    fun stateObserverStop() {
        tripSession.start(true)
        tripSession.registerStateObserver(stateObserver)
        tripSession.stop()
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STOPPED) }
    }

    @Test
    fun stateObserverDoubleStart() {
        tripSession.registerStateObserver(stateObserver)
        tripSession.start(true)
        tripSession.start(true)
        verify(exactly = 1) { stateObserver.onSessionStateChanged(TripSessionState.STARTED) }
    }

    @Test
    fun stateObserverDoubleStop() {
        tripSession.start(true)
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
        tripSession.start(true)
        tripSession.stop()
        verify(exactly = 0) { stateObserver.onSessionStateChanged(any()) }
    }

    @Test
    fun unregisterAllLocationObservers() = coroutineRule.runBlockingTest {
        every { routeProgress.bannerInstructions } returns null
        every { routeProgress.voiceInstructions } returns null

        tripSession.start(true)
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        tripSession.unregisterAllLocationObservers()

        updateLocationAndJoin()

        verify(exactly = 0) {
            observer.onNewRawLocation(any())
            observer.onNewLocationMatcherResult(any())
        }
        assertEquals(location, tripSession.getRawLocation())

        tripSession.stop()
    }

    @Test
    fun unregisterAllRouteProgressObservers() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start(true)
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
        tripSession.start(true)
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
        val bannerInstructionsObserver: BannerInstructionsObserver = mockk(relaxUnitFun = true)
        every { navigationStatus.getCurrentBannerInstructions(routes.first()) } returns mockk()

        tripSession = buildTripSession()
        tripSession.start(true)
        tripSession.setRoutes(routes, setRoutesInfo)
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
        tripSession.start(true)
        tripSession.registerVoiceInstructionsObserver(voiceInstructionsObserver)

        updateLocationAndJoin()

        tripSession.stop()

        tripSession.start(true)
        tripSession.unregisterAllVoiceInstructionsObservers()

        updateLocationAndJoin()

        verify(exactly = 1) { voiceInstructionsObserver.onNewVoiceInstructions(any()) }

        tripSession.stop()
    }

    @Test
    fun `location matcher result success`() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start(true)
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)
        updateLocationAndJoin()

        verify(exactly = 1) { observer.onNewLocationMatcherResult(locationMatcherResult) }
        assertEquals(location, tripSession.locationMatcherResult?.enhancedLocation)
        tripSession.stop()
    }

    @Test
    fun `location matcher result immediate`() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start(true)
        updateLocationAndJoin()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        verify(exactly = 1) { observer.onNewLocationMatcherResult(locationMatcherResult) }
        assertEquals(location, tripSession.locationMatcherResult?.enhancedLocation)
        tripSession.stop()
    }

    @Test
    fun `location matcher result cleared on reset`() = coroutineRule.runBlockingTest {
        tripSession = buildTripSession()
        tripSession.start(true)
        updateLocationAndJoin()
        tripSession.stop()
        val observer: LocationObserver = mockk(relaxUnitFun = true)
        tripSession.registerLocationObserver(observer)

        verify(exactly = 0) { observer.onNewLocationMatcherResult(any()) }
    }

    @Test
    fun `eHorizonObserver added to subscriptionManager on registerEHorizonObserver`() {
        tripSession = buildTripSession()
        tripSession.start(true)
        val observer: EHorizonObserver = mockk(relaxUnitFun = true)

        tripSession.registerEHorizonObserver(observer)

        verify(exactly = 1) { eHorizonSubscriptionManager.registerObserver(observer) }
        tripSession.stop()
    }

    @Test
    fun `eHorizonObserver removed from subscriptionManager on unregisterObserver`() {
        tripSession = buildTripSession()
        tripSession.start(true)
        val observer: EHorizonObserver = mockk(relaxUnitFun = true)

        tripSession.unregisterEHorizonObserver(observer)

        verify(exactly = 1) { eHorizonSubscriptionManager.unregisterObserver(observer) }
        tripSession.stop()
    }

    @Test
    fun `all eHorizonObservers removed from subscriptionManager on unregisterAllObservers`() {
        tripSession = buildTripSession()
        tripSession.start(true)

        tripSession.unregisterAllEHorizonObservers()

        verify(exactly = 1) { eHorizonSubscriptionManager.unregisterAllObservers() }
        tripSession.stop()
    }

    @Test
    fun `when session is started and navigator is recreated observer is reset`() {
        tripSession = buildTripSession()
        tripSession.start(true)

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

    @Test
    fun `local route reference updated after route set`() = coroutineRule.runBlockingTest {
        coEvery { navigator.setRoutes(any(), any(), any(), any()) } coAnswers {
            delay(100)
            createSetRouteResult()
        }

        pauseDispatcher {
            launch {
                tripSession.setRoutes(routes, setRoutesInfo)
            }
            runCurrent()
            assertNull(tripSession.primaryRoute)
            advanceTimeBy(200)
            assertEquals(routes.first(), tripSession.primaryRoute)
        }
    }

    @Test
    fun `local route reference updated after route is cleared`() = runBlockingTest {
        tripSession.setRoutes(routes, setRoutesInfo)

        assertEquals(tripSession.primaryRoute, routes.first())

        tripSession.setRoutes(
            emptyList(),
            SetRoutes.CleanUp,
        )

        assertNull(tripSession.primaryRoute)
    }

    @Test
    fun `offRoute state is reset when setRoute is called`() = coroutineRule.runBlockingTest {
        coEvery { navigator.setRoutes(any(), reason = any()) } coAnswers {
            delay(100)
            createSetRouteResult()
        }

        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE
        every { routeProgress.currentState } returns RouteProgressState.OFF_ROUTE

        val offRouteObserver: OffRouteObserver = mockk(relaxed = true)

        tripSession = buildTripSession()
        tripSession.registerOffRouteObserver(offRouteObserver)

        tripSession.start(true)

        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)

        pauseDispatcher {
            launch {
                tripSession.setRoutes(routes, setRoutesInfo)
            }
            runCurrent()

            verifyOrder {
                offRouteObserver.onOffRouteStateChanged(false)
                offRouteObserver.onOffRouteStateChanged(true)
                offRouteObserver.onOffRouteStateChanged(false)
            }
        }
    }

    @Test
    fun `routeProgress is reset when setRoute is called`() = coroutineRule.runBlockingTest {
        coEvery { navigator.setRoutes(any(), reason = any()) } coAnswers {
            delay(100)
            createSetRouteResult()
        }

        every { navigationStatus.routeState } returns RouteState.OFF_ROUTE

        tripSession = buildTripSession()
        tripSession.start(true)
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)

        assertNotNull(tripSession.getRouteProgress())

        pauseDispatcher {
            launch {
                tripSession.setRoutes(routes, setRoutesInfo)
            }
            runCurrent()
            assertNull(tripSession.getRouteProgress())
        }
    }

    @Test
    fun `routeProgress updates ignored while primary route is being set`() =
        coroutineRule.runBlockingTest {
            val primary = mockNavigationRoute()
            val alternative = mockNavigationRoute()
            coEvery {
                navigator.setRoutes(
                    primary,
                    legIndex,
                    listOf(alternative),
                    any(),
                )
            } coAnswers {
                delay(100)
                createSetRouteResult()
            }

            val observer = mockk<RouteProgressObserver>(relaxUnitFun = true)

            val tripSession = buildTripSession()
            tripSession.registerRouteProgressObserver(observer)
            tripSession.start(withTripService = true)

            pauseDispatcher {
                launch { tripSession.setRoutes(listOf(primary, alternative), setRoutesInfo) }
                runCurrent()
                advanceTimeBy(delayTimeMillis = 50)
                navigatorObserverImplSlot.captured.onStatus(
                    navigationStatusOrigin,
                    navigationStatus,
                )
                verify(exactly = 0) { observer.onRouteProgressChanged(any()) }
                advanceTimeBy(delayTimeMillis = 100)
                navigatorObserverImplSlot.captured.onStatus(
                    navigationStatusOrigin,
                    navigationStatus,
                )
                verify(exactly = 1) { observer.onRouteProgressChanged(any()) }
            }
        }

    @Test
    fun `routeProgress updates not ignored while only alternative route is being set`() =
        coroutineRule.runBlockingTest {
            val primary = mockNavigationRoute()
            val alternative = mockNavigationRoute()
            coEvery { navigator.setAlternativeRoutes(listOf(alternative)) } coAnswers {
                delay(100)
                emptyList()
            }

            val observer = mockk<RouteProgressObserver>(relaxUnitFun = true)

            val tripSession = buildTripSession()
            tripSession.registerRouteProgressObserver(observer)
            tripSession.start(withTripService = true)

            pauseDispatcher {
                val setRoutesInfo = SetRoutes.Alternatives(legIndex)
                launch { tripSession.setRoutes(listOf(primary, alternative), setRoutesInfo) }
                runCurrent()
                advanceTimeBy(delayTimeMillis = 50)
                navigatorObserverImplSlot.captured.onStatus(
                    navigationStatusOrigin,
                    navigationStatus,
                )
                verify(exactly = 1) { observer.onRouteProgressChanged(any()) }
            }
        }

    @Test
    fun `routeProgress updates are not ignored while primary route is being refreshed`() =
        coroutineRule.runBlockingTest {
            val primary = mockNavigationRoute()
            val alternative = mockNavigationRoute()
            coEvery { navigator.refreshRoute(primary) } coAnswers {
                delay(100)
                ExpectedFactory.createValue(emptyList())
            }

            val observer = mockk<RouteProgressObserver>(relaxUnitFun = true)

            val tripSession = buildTripSession()
            tripSession.registerRouteProgressObserver(observer)
            tripSession.start(withTripService = true)

            pauseDispatcher {
                val setRoutesInfo = SetRoutes.RefreshRoutes(routeProgressData)
                launch { tripSession.setRoutes(listOf(primary, alternative), setRoutesInfo) }
                runCurrent()
                advanceTimeBy(delayTimeMillis = 50)
                navigatorObserverImplSlot.captured.onStatus(
                    navigationStatusOrigin,
                    navigationStatus,
                )
                verify(exactly = 1) { observer.onRouteProgressChanged(any()) }
            }
        }

    @Test
    fun `enhancedLocation, locationMatcherResult, zLevel are updating while setting a route, routeProgress, bannerInstructions and offRoute state are skipped`() =
        coroutineRule.runBlockingTest {
            val primary = mockNavigationRoute()
            val alternative = mockNavigationRoute()
            coEvery { navigator.setRoutes(any(), any(), any(), any()) } coAnswers {
                delay(100)
                createSetRouteResult()
            }

            val routeProgressObserver: RouteProgressObserver = mockk(relaxUnitFun = true)
            val locationObserver: LocationObserver = mockk(relaxUnitFun = true)
            val offRouteObserver: OffRouteObserver = mockk(relaxUnitFun = true)
            val bannerInstructionsObserver: BannerInstructionsObserver = mockk(relaxUnitFun = true)
            every { routeProgressObserver.onRouteProgressChanged(any()) } just Runs
            every { locationObserver.onNewLocationMatcherResult(any()) } just Runs
            every { offRouteObserver.onOffRouteStateChanged(any()) } just Runs
            every { bannerInstructionsObserver.onNewBannerInstructions(any()) } just Runs

            val banner: BannerInstructions = mockk(relaxed = true)
            every { navigationStatus.getCurrentBannerInstructions(primary) } returns banner

            tripSession = buildTripSession()
            tripSession.registerRouteProgressObserver(routeProgressObserver)
            tripSession.registerLocationObserver(locationObserver)
            tripSession.registerOffRouteObserver(offRouteObserver)
            tripSession.registerBannerInstructionsObserver(bannerInstructionsObserver)
            tripSession.start(true)

            pauseDispatcher {
                launch {
                    // it will notify offRouteObserver for the first time
                    tripSession.setRoutes(
                        listOf(primary, alternative),
                        setRoutesInfo,
                    )
                }
                runCurrent()

                every { navigationStatus.routeState } returns RouteState.OFF_ROUTE

                // first status update
                every { navigationStatus.layer } returns 100
                navigatorObserverImplSlot.captured.onStatus(
                    navigationStatusOrigin,
                    navigationStatus,
                )
                runCurrent()
                assertEquals(100, tripSession.zLevel)

                // second status update
                every { navigationStatus.layer } returns 200
                navigatorObserverImplSlot.captured.onStatus(
                    navigationStatusOrigin,
                    navigationStatus,
                )
                runCurrent()
                assertEquals(200, tripSession.zLevel)

                // finish setRoute
                advanceTimeBy(400)

                // third status update
                every { navigationStatus.layer } returns 300
                navigatorObserverImplSlot.captured.onStatus(
                    navigationStatusOrigin,
                    navigationStatus,
                )
                runCurrent()
                assertEquals(300, tripSession.zLevel)

                // locationObserver is notified on each status update
                verify(exactly = 3) { locationObserver.onNewLocationMatcherResult(any()) }

                // routeProgressObserver and bannerInstructionsObserver are notified when setRoute is finished
                verify(exactly = 1) { routeProgressObserver.onRouteProgressChanged(any()) }
                verify(exactly = 1) { bannerInstructionsObserver.onNewBannerInstructions(banner) }
                // offRouteObserver is notified twice:
                // when setRoute starts, and on a new status when setRoute is finished
                verify(exactly = 2) { offRouteObserver.onOffRouteStateChanged(any()) }
            }
        }

    @Test
    fun `zLevel is null before session is started`() {
        assertNull(tripSession.zLevel)
    }

    @Test
    fun `zLevel is null after session is started, but before navigator observer is invoked`() {
        tripSession.start(withTripService = true)
        assertNull(tripSession.zLevel)
    }

    @Test
    fun `zLevel returns layer from navigator status`() {
        tripSession.start(withTripService = true)
        every { navigationStatus.layer } returns 3
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)
        assertEquals(3, tripSession.zLevel)
    }

    @Test
    fun `zLevel returns null after session is stopped`() {
        tripSession.start(withTripService = true)
        every { navigationStatus.layer } returns 3
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)
        tripSession.stop()
        assertNull(tripSession.zLevel)
    }

    @Test
    fun `updateLegIndexJob is cancelled and callback is fired when setRoute succeeds`() =
        coroutineRule.runBlockingTest {
            coEvery { navigator.setRoutes(any(), reason = any()) } coAnswers {
                delay(50)
                createSetRouteResult()
            }

            coEvery { navigator.updateLegIndex(any()) } coAnswers {
                delay(100) // doesn't affect test duration
                true
            }

            tripSession = buildTripSession()
            tripSession.start(true)

            val legIndexUpdatedCallback: LegIndexUpdatedCallback = mockk(relaxed = true)

            pauseDispatcher {
                launch {
                    tripSession.updateLegIndex(1, legIndexUpdatedCallback)
                }
                runCurrent()
                launch {
                    tripSession.setRoutes(
                        routes,
                        SetRoutes.NewRoutes(legIndex),
                    )
                }
                advanceTimeBy(75)

                verify(exactly = 1) {
                    legIndexUpdatedCallback.onLegIndexUpdatedCallback(false)
                }
            }
        }

    @Test
    fun `updateLegIndexJob isn't cancelled and callback is fired when setRoute fails`() =
        coroutineRule.runBlockingTest {
            coEvery { navigator.setRoutes(any(), any(), any(), any()) } coAnswers {
                delay(50)
                createSetRouteError()
            }

            coEvery { navigator.updateLegIndex(any()) } coAnswers {
                delay(100) // doesn't affect test duration
                true
            }

            tripSession = buildTripSession()
            tripSession.start(true)

            val legIndexUpdatedCallback: LegIndexUpdatedCallback = mockk(relaxed = true)
            pauseDispatcher {
                launch {
                    tripSession.updateLegIndex(1, legIndexUpdatedCallback)
                }
                runCurrent()
                launch {
                    tripSession.setRoutes(
                        routes,
                        SetRoutes.NewRoutes(legIndex),
                    )
                }
                advanceTimeBy(75)

                verify(exactly = 0) {
                    legIndexUpdatedCallback.onLegIndexUpdatedCallback(any())
                }
            }

            verify(exactly = 1) {
                legIndexUpdatedCallback.onLegIndexUpdatedCallback(true)
            }
        }

    /*
        navigator.updateLegIndex(legIndex) might force invoking NavigatorObserver and one adds
        new latest banner instructions to BannerInstructionEvent. That's why requires pre-save
        latest banner instructions to remove legacy instructions only
     */
    @Test
    fun `updateLegIndex latest banner and voice instructions pre-save to later clean up them`() =
        coroutineRule.runBlockingTest {
            val idx = -1
            val mockLatestInstructionWrapper =
                mockk<BannerInstructionEvent.LatestInstructionWrapper>()
            every {
                bannerInstructionEvent.latestInstructionWrapper
            } returns mockLatestInstructionWrapper
            coEvery { navigator.updateLegIndex(idx) } coAnswers {
                true
            }
            val mockVoiceInstruction = mockk<VoiceInstructions>()

            tripSession.lastVoiceInstruction = mockVoiceInstruction
            tripSession.updateLegIndex(idx, mockk(relaxUnitFun = true))

            coVerifyOrder {
                bannerInstructionEvent.latestInstructionWrapper
                navigator.updateLegIndex(idx)
                bannerInstructionEvent
                    .invalidateLatestBannerInstructions(mockLatestInstructionWrapper)
                tripSession.lastVoiceInstruction = null
            }
            assertNull(tripSession.lastVoiceInstruction)
        }

    @After
    fun cleanUp() {
        unmockkStatic("com.mapbox.navigation.core.navigator.NavigatorMapper")
        unmockkStatic("com.mapbox.navigation.core.navigator.LocationEx")
        unmockkStatic("com.mapbox.navigation.navigator.internal.utils.TripStatusEx")
        unmockkObject(BannerInstructionEvent.Companion)
        unmockkObject(RoadObjectFactory)
        unmockkStatic(LocationServiceFactory::class)
    }

    private fun mockLocation(): Location = mockk(relaxed = true)

    private suspend fun updateLocationAndJoin() {
        locationUpdateAnswers.invoke(location)
        navigatorObserverImplSlot.captured.onStatus(navigationStatusOrigin, navigationStatus)
        parentJob.cancelAndJoin()
    }
}

private fun mockNavigationRoute(
    roadObjects: List<UpcomingRoadObject> = listOf(mockk(relaxed = true)),
): NavigationRoute = mockk(relaxed = true) {
    every { upcomingRoadObjects } returns roadObjects
}

fun createSetRouteError(
    error: String = "test error",
) = ExpectedFactory.createError<String, SetRoutesResult>(error)

fun createSetRouteResult(
    nativeAlternatives: List<RouteAlternative> = emptyList(),
) = ExpectedFactory.createValue<String, SetRoutesResult>(SetRoutesResult(null, nativeAlternatives))
