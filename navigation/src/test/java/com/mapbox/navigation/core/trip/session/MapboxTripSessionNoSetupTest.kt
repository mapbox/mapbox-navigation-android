package com.mapbox.navigation.core.trip.session

import android.content.Context
import android.os.Looper
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.location.DeviceLocationProvider
import com.mapbox.common.location.ExtendedLocationProviderParameters
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationProvider
import com.mapbox.common.location.LocationProviderRequest
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory
import com.mapbox.navigation.base.internal.factory.RoadObjectFactory.toUpcomingRoadObjects
import com.mapbox.navigation.base.options.LocationOptions
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.infra.TestLocationProvider
import com.mapbox.navigation.core.infra.recorders.BannerInstructionsObserverRecorder
import com.mapbox.navigation.core.infra.recorders.OffRouteObserverRecorder
import com.mapbox.navigation.core.infra.recorders.RouteProgressObserverRecorder
import com.mapbox.navigation.core.infra.recorders.VoiceInstructionsObserverRecorder
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.StatusWithVoiceInstructionUpdateUtil.LONGITUDE_FOR_VOICE_INSTRUCTION_1
import com.mapbox.navigation.core.trip.session.StatusWithVoiceInstructionUpdateUtil.LONGITUDE_FOR_VOICE_INSTRUCTION_2
import com.mapbox.navigation.core.trip.session.StatusWithVoiceInstructionUpdateUtil.LONGITUDE_FOR_VOICE_INSTRUCTION_NULL
import com.mapbox.navigation.core.utils.ThreadUtils
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.factories.createBannerInstruction
import com.mapbox.navigation.testing.factories.createBannerSection
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createLocation
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createNavigationStatus
import com.mapbox.navigation.testing.factories.createVoiceInstruction
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouteInfo
import com.mapbox.navigator.RouteState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxTripSessionNoSetupTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        mockkObject(RoadObjectFactory)
        mockkStatic(Looper::class)
        mockkStatic(LocationServiceFactory::class)
        every { LocationServiceFactory.getOrCreate() } returns mockk {
            every {
                getDeviceLocationProvider(
                    extendedParameters = any<ExtendedLocationProviderParameters>(),
                    request = any<LocationProviderRequest>(),
                )
            } returns ExpectedFactory.createValue(mockk(relaxed = true))
            every { setUserDefinedDeviceLocationProviderFactory(any()) } answers {}
        }
        every { Looper.getMainLooper() } returns mockk()
        mockkObject(ThreadUtils)
        every { ThreadUtils.prepareHandlerThread(any(), any()) } returns mockk {
            every { looper } returns mockk()
        }
    }

    @After
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun voiceInstructionsFallbacksToPreviousValue() = coroutineRule.runBlockingTest {
        // arrange
        val voiceInstructionsObserver = VoiceInstructionsObserverRecorder()
        val routeProgressObserver = RouteProgressObserverRecorder()
        val nativeNavigator = createNativeNavigatorMock()
        StatusWithVoiceInstructionUpdateUtil.triggerStatusUpdatesOnLocationUpdate(nativeNavigator)
        val locationEngine = TestLocationProvider.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationProvider = locationEngine,
        )
        tripSession.start(true)
        tripSession.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            SetRoutes.NewRoutes(0),
        )
        // act
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL),
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_2))
        // assert
        val announcementsFromVoiceInstructionObserver = voiceInstructionsObserver.records
            .takeLast(3) // take only events triggered by location updates
            .map { it.announcement() }
        assertEquals(listOf("1", "2"), announcementsFromVoiceInstructionObserver)
        val announcementsFromRouteProgress = routeProgressObserver.records
            .takeLast(3) // take only events triggered by location updates
            .map { it.voiceInstructions?.announcement() }
        assertEquals(listOf("1", "1", "2"), announcementsFromRouteProgress)
    }

    @Test
    fun addingVoiceInstructionsObserversInTheMiddleOfNavigation() = coroutineRule.runBlockingTest {
        // arrange
        val nativeNavigator = createNativeNavigatorMock()
        StatusWithVoiceInstructionUpdateUtil.triggerStatusUpdatesOnLocationUpdate(nativeNavigator)
        val locationEngine = TestLocationProvider.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationProvider = locationEngine,
        )
        tripSession.start(true)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            SetRoutes.NewRoutes(0),
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        val routeProgressObserver = RouteProgressObserverRecorder()
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        // act
        val voiceInstructionsObserver = VoiceInstructionsObserverRecorder()
        tripSession.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL),
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_2))
        // assert
        val voiceInstructionsAnnouncements = voiceInstructionsObserver.records
            .takeLast(2) // take only events triggered by location updates
            .map { it.announcement() }
        assertEquals(listOf("2"), voiceInstructionsAnnouncements)
    }

    @Test
    fun noVoiceInstructionFallbackForFreshRoute() = coroutineRule.runBlockingTest {
        // arrange
        val nativeNavigator = createNativeNavigatorMock()
        StatusWithVoiceInstructionUpdateUtil.triggerStatusUpdatesOnLocationUpdate(nativeNavigator)
        val locationEngine = TestLocationProvider.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationProvider = locationEngine,
        )
        val routeProgressObserver = RouteProgressObserverRecorder()
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        tripSession.start(true)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            SetRoutes.NewRoutes(0),
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        // act
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            SetRoutes.NewRoutes(0),
        )
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL),
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_2))
        // assert
        val voiceInstructionsAnnouncements = routeProgressObserver.records
            .takeLast(2) // take only events triggered by location updates
            .map { it.voiceInstructions?.announcement() }
        assertEquals(listOf(null, "2"), voiceInstructionsAnnouncements)
    }

    @Test
    fun noVoiceInstructionFallbackAfterLegIndexUpdate() = coroutineRule.runBlockingTest {
        // arrange
        val nativeNavigator = createNativeNavigatorMock()
        StatusWithVoiceInstructionUpdateUtil.triggerStatusUpdatesOnLocationUpdate(nativeNavigator)
        coEvery { nativeNavigator.updateLegIndex(any()) } returns true
        val locationEngine = TestLocationProvider.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationProvider = locationEngine,
        )
        val routeProgressObserver = RouteProgressObserverRecorder()
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        tripSession.start(true)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            SetRoutes.NewRoutes(0),
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        // act
        tripSession.updateLegIndex(1) { }
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL),
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_2))
        // assert
        val voiceInstructionsAnnouncements = routeProgressObserver.records
            .takeLast(2) // take only events triggered by location updates
            .map { it.voiceInstructions?.announcement() }
        assertEquals(listOf(null, "2"), voiceInstructionsAnnouncements)
    }

    @Test
    fun voiceInstructionFallbackAfterUnsuccessfulLegIndexUpdate() = coroutineRule.runBlockingTest {
        // arrange
        val nativeNavigator = createNativeNavigatorMock()
        coEvery { nativeNavigator.updateLegIndex(any()) } returns false
        StatusWithVoiceInstructionUpdateUtil.triggerStatusUpdatesOnLocationUpdate(nativeNavigator)
        val locationEngine = TestLocationProvider.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationProvider = locationEngine,
        )
        val routeProgressObserver = RouteProgressObserverRecorder()
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        tripSession.start(true)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            SetRoutes.NewRoutes(0),
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        // act
        tripSession.updateLegIndex(1) { }
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL),
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_2))
        // assert
        val voiceInstructionsAnnouncements = routeProgressObserver.records
            .takeLast(2) // take only events triggered by location updates
            .map { it.voiceInstructions?.announcement() }
        assertEquals(listOf("1", "2"), voiceInstructionsAnnouncements)
    }

    @Test
    fun newVoiceInstructionsTriggerEvenIfTheyAreTheSameAsPreviousOne() =
        coroutineRule.runBlockingTest {
            // arrange
            val nativeNavigator = createNativeNavigatorMock()
            StatusWithVoiceInstructionUpdateUtil.triggerStatusUpdatesOnLocationUpdate(
                nativeNavigator,
            )
            val locationEngine = TestLocationProvider.create()
            val tripSession = buildTripSession(
                nativeNavigator = nativeNavigator,
                locationProvider = locationEngine,
            )
            val voiceInstructionObserver = VoiceInstructionsObserverRecorder()
            tripSession.registerVoiceInstructionsObserver(voiceInstructionObserver)
            tripSession.start(true)
            tripSession.setRoutes(
                listOf(createNavigationRoute()),
                SetRoutes.NewRoutes(0),
            )
            locationEngine.updateLocation(
                createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1),
            )
            // act
            locationEngine.updateLocation(
                createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1),
            )
            // assert
            val newVoiceInstructions = voiceInstructionObserver.records
                .takeLast(2) // take only events triggered by location updates
                .map { it.announcement() }
            assertEquals(listOf("1", "1"), newVoiceInstructions)
        }

    @Test
    fun `banner instruction remaining distance is transferred from NN to SDK status update`() =
        coroutineRule.runBlockingTest {
            // arrange
            val nativeNavigator = createNativeNavigatorMock()
            val statusUpdateListeners = recordNavigatorObservers(nativeNavigator)
            val locationEngine = TestLocationProvider.create()
            val tripSession = buildTripSession(
                nativeNavigator = nativeNavigator,
                locationProvider = locationEngine,
            )
            val routeProgressRecorder = RouteProgressObserverRecorder()
            tripSession.registerRouteProgressObserver(routeProgressRecorder)
            tripSession.start(true)
            tripSession.setRoutes(
                listOf(createNavigationRoute()),
                SetRoutes.NewRoutes(0),
            )
            // act
            locationEngine.updateLocation(createLocation())
            statusUpdateListeners.onStatus(
                NavigationStatusOrigin.LOCATION_UPDATE,
                createNavigationStatus(
                    primaryRouteId = "id#0",
                    bannerInstruction = createBannerInstruction(
                        index = 0,
                        primary = createBannerSection(),
                    ),
                ),
            )

            // assert
            val bannerInstructions = routeProgressRecorder.records
                .takeLast(1) // take only events triggered by location updates
                .map { it.bannerInstructions?.distanceAlongGeometry() }
            assertEquals(
                listOf(50.0),
                bannerInstructions,
            )
        }

    @Test
    fun `fallback to the next waypoint index 1 when navigator doesn't think we reached it and returns 0`() =
        coroutineRule.runBlockingTest {
            // arrange
            val nativeNavigator = createNativeNavigatorMock()
            val navigatorObservers = recordNavigatorObservers(nativeNavigator)
            val tripSession = buildTripSession(
                nativeNavigator = nativeNavigator,
            )
            tripSession.start(false)
            tripSession.setRoutes(
                listOf(createNavigationRoute()),
                SetRoutes.NewRoutes(0),
            )
            // act
            navigatorObservers.onStatus(
                NavigationStatusOrigin.UNCONDITIONAL,
                createNavigationStatus(
                    primaryRouteId = "id#0",
                    nextWaypointIndex = 0,
                    routeState = RouteState.OFF_ROUTE,
                ),
            )
            // assert
            val remainingWaypoints = tripSession.getRouteProgress()?.remainingWaypoints
            assertEquals(1, remainingWaypoints)
        }

    @Test
    fun `location updates doesn't change state when trip session is stopped`() {
        val testLocationProvider = TestLocationProvider.create()
        val tripSession = buildTripSession(
            locationProvider = testLocationProvider,
        )

        tripSession.start(false)
        tripSession.stop()
        testLocationProvider.updateLocation(createLocation())

        assertNull(tripSession.getRawLocation())
    }

    @Test
    fun `trip session works with a synchronous location update`() {
        val testLocation = createLocation(latitude = 99.0)
        val locationProviderMock = mockk<DeviceLocationProvider>(relaxed = true)
        triggerLocationEngineUpdateOnSubscribe(locationProviderMock, testLocation)
        val nativeNavigator = createNativeNavigatorMock()
        triggerStatusUpdateOnEachLocationUpdate(nativeNavigator)
        val tripSession = buildTripSession(
            locationProvider = locationProviderMock,
            nativeNavigator = nativeNavigator,
        )

        tripSession.start(false)

        assertEquals(testLocation, tripSession.getRawLocation())
    }

    @Test
    fun `session doesn't override route reference if native routes update fails`() =
        coroutineRule.runBlockingTest {
            // arrange
            val initialRoute = createNavigationRoute()
            val failingRoute = createNavigationRoute(createDirectionsRoute(requestUuid = "fail"))
            val routeProgressObserver = RouteProgressObserverRecorder()
            val nativeNavigator = createNativeNavigatorMock()
            StatusWithVoiceInstructionUpdateUtil.triggerStatusUpdatesOnLocationUpdate(
                nativeNavigator,
            )
            val locationEngine = TestLocationProvider.create()
            val tripSession = buildTripSession(
                nativeNavigator = nativeNavigator,
                locationProvider = locationEngine,
            )
            tripSession.start(true)
            tripSession.registerRouteProgressObserver(routeProgressObserver)
            // act
            tripSession.setRoutes(
                listOf(initialRoute),
                SetRoutes.NewRoutes(0),
            )
            coEvery {
                nativeNavigator.setRoutes(failingRoute, any(), any(), any())
            } returns createSetRouteError()
            tripSession.setRoutes(
                listOf(failingRoute),
                SetRoutes.NewRoutes(0),
            )
            locationEngine.updateLocation(
                createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1),
            )
            locationEngine.updateLocation(
                createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL),
            )
            locationEngine.updateLocation(
                createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_2),
            )
            // assert
            assertTrue(
                routeProgressObserver.records
                    .takeLast(3)
                    .all { it.navigationRoute == initialRoute },
            )
            assertEquals(initialRoute, tripSession.primaryRoute)
        }

    @Test
    fun `session doesn't clear off route state if native routes update fails`() =
        coroutineRule.runBlockingTest {
            // arrange
            val initialRoute = createNavigationRoute()
            val failingRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "fail"),
            )
            val observer = OffRouteObserverRecorder()
            val nativeNavigator = createNativeNavigatorMock()
            StatusWithOffRouteUpdateUtil.triggerStatusUpdatesOnLocationUpdate(
                nativeNavigator,
            )
            val locationEngine = TestLocationProvider.create()
            val tripSession = buildTripSession(
                nativeNavigator = nativeNavigator,
                locationProvider = locationEngine,
            )
            tripSession.start(true)
            tripSession.registerOffRouteObserver(observer)
            // act
            tripSession.setRoutes(
                listOf(initialRoute),
                SetRoutes.NewRoutes(0),
            )
            locationEngine.updateLocation(createLocation())
            coEvery {
                nativeNavigator.setRoutes(failingRoute, any(), any(), any())
            } returns createSetRouteError()
            tripSession.setRoutes(
                listOf(failingRoute),
                SetRoutes.NewRoutes(0),
            )
            // assert
            assertEquals(true, observer.records.last())
        }

    @Test
    fun `session doesn't clear route progress state if native routes update fails`() =
        coroutineRule.runBlockingTest {
            // arrange
            val initialRoute = createNavigationRoute()
            val failingRoute = createNavigationRoute(
                createDirectionsRoute(requestUuid = "fail"),
            )
            val nativeNavigator = createNativeNavigatorMock()
            StatusWithOffRouteUpdateUtil.triggerStatusUpdatesOnLocationUpdate(
                nativeNavigator,
            )
            val locationEngine = TestLocationProvider.create()
            val tripSession = buildTripSession(
                nativeNavigator = nativeNavigator,
                locationProvider = locationEngine,
            )
            tripSession.start(true)
            // act
            tripSession.setRoutes(
                listOf(initialRoute),
                SetRoutes.NewRoutes(0),
            )
            locationEngine.updateLocation(createLocation())
            coEvery {
                nativeNavigator.setRoutes(failingRoute, any(), any(), any())
            } returns createSetRouteError()
            tripSession.setRoutes(
                listOf(failingRoute),
                SetRoutes.NewRoutes(0),
            )
            // assert
            assertNotNull(tripSession.getRouteProgress())
        }

    @Test
    fun `session doesn't clear voice and banner state if native routes update fails`() =
        coroutineRule.runBlockingTest {
            // arrange
            val initialRoute = createNavigationRoute()
            val failingRoute = createNavigationRoute(createDirectionsRoute(requestUuid = "fail"))
            val voiceInstructionObserver = VoiceInstructionsObserverRecorder()
            val bannerInstructionObserver = BannerInstructionsObserverRecorder()
            val nativeNavigator = createNativeNavigatorMock()
            StatusWithVoiceInstructionUpdateUtil.triggerStatusUpdatesOnLocationUpdate(
                nativeNavigator,
            )
            val locationEngine = TestLocationProvider.create()
            val tripSession = buildTripSession(
                nativeNavigator = nativeNavigator,
                locationProvider = locationEngine,
            )
            tripSession.start(true)
            tripSession.registerVoiceInstructionsObserver(voiceInstructionObserver)
            tripSession.registerBannerInstructionsObserver(bannerInstructionObserver)
            // act
            tripSession.setRoutes(
                listOf(initialRoute),
                SetRoutes.NewRoutes(0),
            )
            locationEngine.updateLocation(
                createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1),
            )
            coEvery {
                nativeNavigator.setRoutes(failingRoute, any(), any(), any())
            } returns createSetRouteError()
            tripSession.setRoutes(
                listOf(failingRoute),
                SetRoutes.NewRoutes(0),
            )
            locationEngine.updateLocation(
                createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1),
            )
            // assert
            assertEquals(
                tripSession.getRouteProgress()!!.voiceInstructions,
                voiceInstructionObserver.records.last(),
            )
            assertEquals(1, bannerInstructionObserver.records.size)
            assertEquals(
                tripSession.getRouteProgress()!!.bannerInstructions,
                bannerInstructionObserver.records.last(),
            )
        }
}

private fun buildTripSession(
    nativeNavigator: MapboxNativeNavigator = createNativeNavigatorMock(),
    locationProvider: DeviceLocationProvider = TestLocationProvider.create(),
): MapboxTripSession {
    val tripService: TripService = mockk(relaxUnitFun = true) {
        every { hasServiceStarted() } returns false
    }

    val directionsSession: DirectionsSession = mockk(relaxUnitFun = true)

    val parentJob = SupervisorJob()
    val testScope = CoroutineScope(parentJob + TestCoroutineDispatcher())
    val threadController = spyk<ThreadController>()
    every { threadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)

    every { LocationServiceFactory.getOrCreate() } returns mockk {
        every {
            getDeviceLocationProvider(
                extendedParameters = any<ExtendedLocationProviderParameters>(),
                request = any<LocationProviderRequest>(),
            )
        } returns ExpectedFactory.createValue(locationProvider)
    }
    return MapboxTripSession(
        tripService,
        directionsSession,
        TripSessionLocationEngine(LocationOptions.Builder().build()),
        nativeNavigator,
        threadController,
        eHorizonSubscriptionManager = mockk(relaxed = true),
    )
}

private fun createMockContext(): Context {
    val context: Context = mockk(relaxed = true)
    mockkStatic(context::inferDeviceLocale)
    every {
        context.inferDeviceLocale()
    } returns Locale.US
    return context
}

object StatusWithVoiceInstructionUpdateUtil {

    const val LONGITUDE_FOR_VOICE_INSTRUCTION_1 = 1.0
    const val LONGITUDE_FOR_VOICE_INSTRUCTION_NULL = 1.5
    const val LONGITUDE_FOR_VOICE_INSTRUCTION_2 = 2.0

    fun triggerStatusUpdatesOnLocationUpdate(nativeNavigator: MapboxNativeNavigator) {
        val navigatorObservers = recordNavigatorObservers(nativeNavigator)
        coEvery {
            nativeNavigator.updateLocation(
                match { it.coordinate.longitude() == LONGITUDE_FOR_VOICE_INSTRUCTION_1 },
            )
        } answers {
            navigatorObservers.onStatus(
                NavigationStatusOrigin.LOCATION_UPDATE,
                createNavigationStatus(
                    primaryRouteId = "id#0",
                    location = firstArg(),
                    voiceInstruction = createVoiceInstruction("1"),
                ),
            )
            true
        }
        coEvery {
            nativeNavigator.updateLocation(
                match { it.coordinate.longitude() == LONGITUDE_FOR_VOICE_INSTRUCTION_NULL },
            )
        } answers {
            navigatorObservers.onStatus(
                NavigationStatusOrigin.LOCATION_UPDATE,
                createNavigationStatus(
                    primaryRouteId = "id#0",
                    location = firstArg(),
                    voiceInstruction = null,
                ),
            )
            true
        }
        coEvery {
            nativeNavigator.updateLocation(
                match { it.coordinate.longitude() == LONGITUDE_FOR_VOICE_INSTRUCTION_2 },
            )
        } answers {
            navigatorObservers.onStatus(
                NavigationStatusOrigin.LOCATION_UPDATE,
                createNavigationStatus(
                    primaryRouteId = "id#0",
                    location = firstArg(),
                    voiceInstruction = createVoiceInstruction("2"),
                ),
            )
            true
        }
    }
}

object StatusWithOffRouteUpdateUtil {

    fun triggerStatusUpdatesOnLocationUpdate(nativeNavigator: MapboxNativeNavigator) {
        val navigatorObservers = recordNavigatorObservers(nativeNavigator)
        coEvery {
            nativeNavigator.updateLocation(
                any(),
            )
        } answers {
            navigatorObservers.onStatus(
                NavigationStatusOrigin.LOCATION_UPDATE,
                createNavigationStatus(
                    primaryRouteId = "id#0",
                    location = firstArg(),
                    voiceInstruction = createVoiceInstruction("1"),
                    routeState = RouteState.OFF_ROUTE,
                ),
            )
            true
        }
    }
}

private fun createNativeNavigatorMock(): MapboxNativeNavigator {
    val nativeNavigator = mockk<MapboxNativeNavigator>(relaxed = true)
    coEvery {
        nativeNavigator.setRoutes(any(), any(), any(), any())
    } returns createSetRouteResult()
    return nativeNavigator
}

private fun recordNavigatorObservers(
    nativeNavigator: MapboxNativeNavigator,
): List<NavigatorObserver> {
    val navigatorObservers = mutableListOf<NavigatorObserver>()
    every {
        nativeNavigator.addNavigatorObserver(capture(navigatorObservers))
    } returns Unit
    return navigatorObservers
}

private fun List<NavigatorObserver>.onStatus(
    statusOrigin: NavigationStatusOrigin,
    status: NavigationStatus,
) {
    this.forEach {
        it.onStatus(statusOrigin, status)
    }
}

private fun triggerStatusUpdateOnEachLocationUpdate(
    nativeNavigator: MapboxNativeNavigator,
) {
    val navigatorObservers = recordNavigatorObservers(nativeNavigator)
    coEvery {
        nativeNavigator.updateLocation(any())
    } answers {
        navigatorObservers.onStatus(
            NavigationStatusOrigin.LOCATION_UPDATE,
            createNavigationStatus(
                primaryRouteId = "id#0",
                location = firstArg(),
                voiceInstruction = null,
            ),
        )
        true
    }
}

private fun triggerLocationEngineUpdateOnSubscribe(
    locationProviderMock: LocationProvider,
    location: Location,
) {
    every {
        locationProviderMock.addLocationObserver(any(), any())
    } answers {
        firstArg<com.mapbox.common.location.LocationObserver>()
            .onLocationUpdateReceived(listOf(location))
    }
}

private fun createMockRouteInfo(
    roadObjects: List<UpcomingRoadObject> = listOf(mockk()),
): RouteInfo = mockk {
    every { alerts.toUpcomingRoadObjects() } returns roadObjects
}

private fun createMockUpcomingRoadObject(): UpcomingRoadObject = mockk {}
