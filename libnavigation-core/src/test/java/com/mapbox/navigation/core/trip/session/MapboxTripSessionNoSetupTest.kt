package com.mapbox.navigation.core.trip.session

import android.content.Context
import android.location.Location
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesExtra.ROUTES_UPDATE_REASON_NEW
import com.mapbox.navigation.core.infra.TestLocationEngine
import com.mapbox.navigation.core.infra.factories.createLocation
import com.mapbox.navigation.core.infra.factories.createNavigationRoute
import com.mapbox.navigation.core.infra.factories.createNavigationStatus
import com.mapbox.navigation.core.infra.factories.createVoiceInstruction
import com.mapbox.navigation.core.infra.recorders.RouteProgressObserverRecorder
import com.mapbox.navigation.core.infra.recorders.VoiceInstructionsObserverRecorder
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.StatusWithVoiceInstructionUpdateUtil.LONGITUDE_FOR_VOICE_INSTRUCTION_1
import com.mapbox.navigation.core.trip.session.StatusWithVoiceInstructionUpdateUtil.LONGITUDE_FOR_VOICE_INSTRUCTION_2
import com.mapbox.navigation.core.trip.session.StatusWithVoiceInstructionUpdateUtil.LONGITUDE_FOR_VOICE_INSTRUCTION_NULL
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.NavigationStatusOrigin
import com.mapbox.navigator.NavigatorObserver
import com.mapbox.navigator.RouteState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxTripSessionNoSetupTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

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
        val locationEngine = TestLocationEngine.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationEngine = locationEngine
        )
        tripSession.start(true)
        tripSession.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            0,
            RoutesExtra.ROUTES_UPDATE_REASON_NEW
        )
        // act
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL)
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
        val locationEngine = TestLocationEngine.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationEngine = locationEngine
        )
        tripSession.start(true)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            0,
            RoutesExtra.ROUTES_UPDATE_REASON_NEW
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        val routeProgressObserver = RouteProgressObserverRecorder()
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        // act
        val voiceInstructionsObserver = VoiceInstructionsObserverRecorder()
        tripSession.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL)
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
        val locationEngine = TestLocationEngine.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationEngine = locationEngine
        )
        val routeProgressObserver = RouteProgressObserverRecorder()
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        tripSession.start(true)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            0,
            ROUTES_UPDATE_REASON_NEW
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        // act
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            0,
            ROUTES_UPDATE_REASON_NEW
        )
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL)
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
        val locationEngine = TestLocationEngine.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationEngine = locationEngine
        )
        val routeProgressObserver = RouteProgressObserverRecorder()
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        tripSession.start(true)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            0,
            ROUTES_UPDATE_REASON_NEW
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        // act
        tripSession.updateLegIndex(1) { }
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL)
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
        val locationEngine = TestLocationEngine.create()
        val tripSession = buildTripSession(
            nativeNavigator = nativeNavigator,
            locationEngine = locationEngine
        )
        val routeProgressObserver = RouteProgressObserverRecorder()
        tripSession.registerRouteProgressObserver(routeProgressObserver)
        tripSession.start(true)
        tripSession.setRoutes(
            listOf(createNavigationRoute()),
            0,
            ROUTES_UPDATE_REASON_NEW
        )
        locationEngine.updateLocation(createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1))
        // act
        tripSession.updateLegIndex(1) { }
        locationEngine.updateLocation(
            createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_NULL)
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
                nativeNavigator
            )
            val locationEngine = TestLocationEngine.create()
            val tripSession = buildTripSession(
                nativeNavigator = nativeNavigator,
                locationEngine = locationEngine
            )
            val voiceInstructionObserver = VoiceInstructionsObserverRecorder()
            tripSession.registerVoiceInstructionsObserver(voiceInstructionObserver)
            tripSession.start(true)
            tripSession.setRoutes(
                listOf(createNavigationRoute()),
                0,
                ROUTES_UPDATE_REASON_NEW
            )
            locationEngine.updateLocation(
                createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1)
            )
            // act
            locationEngine.updateLocation(
                createLocation(longitude = LONGITUDE_FOR_VOICE_INSTRUCTION_1)
            )
            // assert
            val newVoiceInstructions = voiceInstructionObserver.records
                .takeLast(2) // take only events triggered by location updates
                .map { it.announcement() }
            assertEquals(listOf("1", "1"), newVoiceInstructions)
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
            tripSession.setRoutes(listOf(createNavigationRoute()), 0, ROUTES_UPDATE_REASON_NEW)
            // act
            navigatorObservers.onStatus(
                NavigationStatusOrigin.UNCONDITIONAL,
                createNavigationStatus(nextWaypointIndex = 0, routeState = RouteState.OFF_ROUTE)
            )
            // assert
            val remainingWaypoints = tripSession.getRouteProgress()?.remainingWaypoints
            assertEquals(1, remainingWaypoints)
        }

    @Test
    fun `location updates doesn't change state when trip session is stopped`() {
        val testLocationEngine = TestLocationEngine.create()
        val tripSession = buildTripSession(
            locationEngine = testLocationEngine,
        )

        tripSession.start(false)
        tripSession.stop()
        testLocationEngine.updateLocation(createLocation())

        assertNull(tripSession.getRawLocation())
    }

    @Test
    fun `trip session works with a synchronous location update`() {
        val testLocation = createLocation(latitude = 99.0)
        val locationEngineMock = mockk<LocationEngine>(relaxed = true)
        triggerLocationEngineUpdateOnSubscribe(locationEngineMock, testLocation)
        val nativeNavigator = createNativeNavigatorMock()
        triggerStatusUpdateOnEachLocationUpdate(nativeNavigator)
        val tripSession = buildTripSession(
            locationEngine = locationEngineMock,
            nativeNavigator = nativeNavigator,
        )

        tripSession.start(false)

        assertEquals(testLocation, tripSession.getRawLocation())
    }


}

private fun buildTripSession(
    nativeNavigator: MapboxNativeNavigator = createNativeNavigatorMock(),
    locationEngine: LocationEngine = TestLocationEngine.create(),
    context: Context = createMockContext()
): MapboxTripSession {
    val navigationOptions = NavigationOptions.Builder(context)
        .locationEngine(locationEngine)
        .build()

    val tripService: TripService = mockk(relaxUnitFun = true) {
        every { hasServiceStarted() } returns false
    }

    val parentJob = SupervisorJob()
    val testScope = CoroutineScope(parentJob + TestCoroutineDispatcher())
    val threadController = spyk<ThreadController>()
    every { threadController.getMainScopeAndRootJob() } returns JobControl(parentJob, testScope)

    return MapboxTripSession(
        tripService,
        TripSessionLocationEngine(navigationOptions),
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
                match { it.coordinate.longitude() == LONGITUDE_FOR_VOICE_INSTRUCTION_1 }
            )
        } answers {
            navigatorObservers.onStatus(
                NavigationStatusOrigin.LOCATION_UPDATE,
                createNavigationStatus(
                    location = firstArg(),
                    voiceInstruction = createVoiceInstruction("1")
                )
            )
            true
        }
        coEvery {
            nativeNavigator.updateLocation(
                match { it.coordinate.longitude() == LONGITUDE_FOR_VOICE_INSTRUCTION_NULL }
            )
        } answers {
            navigatorObservers.onStatus(
                NavigationStatusOrigin.LOCATION_UPDATE,
                createNavigationStatus(
                    location = firstArg(),
                    voiceInstruction = null
                )
            )
            true
        }
        coEvery {
            nativeNavigator.updateLocation(
                match { it.coordinate.longitude() == LONGITUDE_FOR_VOICE_INSTRUCTION_2 }
            )
        } answers {
            navigatorObservers.onStatus(
                NavigationStatusOrigin.LOCATION_UPDATE,
                createNavigationStatus(
                    location = firstArg(),
                    voiceInstruction = createVoiceInstruction("2")
                )
            )
            true
        }
    }
}

private fun createNativeNavigatorMock(): MapboxNativeNavigator {
    val nativeNavigator = mockk<MapboxNativeNavigator>(relaxed = true)
    coEvery {
        nativeNavigator.setRoutes(any(), any(), any())
    } returns createSetRouteResult()
    return nativeNavigator
}

private fun recordNavigatorObservers(
    nativeNavigator: MapboxNativeNavigator
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
                location = firstArg(),
                voiceInstruction = null
            )
        )
        true
    }
}

private fun triggerLocationEngineUpdateOnSubscribe(
    locationEngineMock: LocationEngine,
    location: Location
) {
    every {
        locationEngineMock.requestLocationUpdates(any(), any(), any())
    } answers {
        secondArg<LocationEngineCallback<LocationEngineResult>>().onSuccess(
            LocationEngineResult.create(location)
        )
    }
}
