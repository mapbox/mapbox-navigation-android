package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.CarContext
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.Rounding
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.internal.telemetry.AndroidAutoEvent
import com.mapbox.navigation.core.internal.telemetry.postAndroidAutoEvent
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.internal.AndroidAutoLog
import com.mapbox.navigation.ui.androidauto.navigation.maneuver.CarManeuverMapper
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxCarNavigationManagerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val navigationManagerCallbackSlot = slot<NavigationManagerCallback>()
    private val navigationManager: NavigationManager = mockk(relaxed = true) {
        every { setNavigationManagerCallback(capture(navigationManagerCallbackSlot)) } just Runs
        every { navigationStarted() } answers {
            every {
                clearNavigationManagerCallback()
            } throws IllegalStateException("Removing callback while navigating")
            every { updateTrip(any()) } just Runs
        }
        every { navigationEnded() } answers {
            every { clearNavigationManagerCallback() } just Runs
            every { updateTrip(any()) } throws IllegalStateException("Navigation is not started")
        }
    }
    private val carContext: CarContext = mockk {
        every { getCarService(NavigationManager::class.java) } returns navigationManager
    }

    private val sut = MapboxCarNavigationManager(carContext)

    @Before
    fun setup() {
        mockkStatic(MapboxScreenManager::class)
        mockkObject(MapboxScreenManager)
        every { MapboxScreenManager.current() } returns null
        mockkObject(CarManeuverMapper)
        every { CarManeuverMapper.from(any<RouteProgress>(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onAttached should set the NavigationManagerCallback`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onAttached(mapboxNavigation)

        verify { navigationManager.setNavigationManagerCallback(any()) }
        assertTrue(navigationManagerCallbackSlot.isCaptured)
    }

    @Test
    fun `onDetached should call clearNavigationManagerCallback`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onDetached(mapboxNavigation)

        verify { navigationManager.clearNavigationManagerCallback() }
    }

    @Test
    fun `onAttached should trigger AndroidAuto CONNECTED telemetry event`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onAttached(mapboxNavigation)

        verify(exactly = 1) {
            mapboxNavigation.postAndroidAutoEvent(AndroidAutoEvent.CONNECTED)
        }
    }

    @Test
    fun `onAttached should trigger AndroidAuto DISCONNECTED telemetry event`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onDetached(mapboxNavigation)

        verify(exactly = 1) {
            mapboxNavigation.postAndroidAutoEvent(AndroidAutoEvent.DISCONNECTED)
        }
    }

    @Test
    fun `non-empty routes should trigger navigationStarted`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val routesObserverSlot = slot<RoutesObserver>()
        every {
            mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        } just Runs
        sut.onAttached(mapboxNavigation)

        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf(mockk()) },
        )

        verify { navigationManager.navigationStarted() }
    }

    @Test
    fun `non-empty routes should not trigger navigationStarted second time`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val routesObserverSlot = slot<RoutesObserver>()
        every {
            mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        } just Runs
        sut.onAttached(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf(mockk()) },
        )
        clearAllMocks(answers = false)

        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf(mockk()) },
        )

        verify(exactly = 0) { navigationManager.navigationStarted() }
    }

    @Test
    fun `empty routes should trigger navigationEnded if in active navigation`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val routesObserverSlot = slot<RoutesObserver>()
        every {
            mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        } just Runs
        sut.onAttached(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf(mockk()) },
        )
        clearAllMocks(answers = false)

        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns emptyList() },
        )

        verify { navigationManager.navigationEnded() }
    }

    @Test
    fun `empty routes should not trigger navigationEnded if not in active navigation`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val routesObserverSlot = slot<RoutesObserver>()
        every {
            mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        } just Runs
        sut.onAttached(mapboxNavigation)

        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns emptyList() },
        )

        verify(exactly = 0) { navigationManager.navigationEnded() }
    }

    @Test
    fun `RouteProgress should trigger updateTrip`() {
        val routesSlot = mutableListOf<RoutesObserver>()
        val routeProgressObserverSlot = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mapboxNavigationMock(
            routesSlot,
            routeProgressObserverSlot,
        )

        sut.onAttached(mapboxNavigation)
        mapboxNavigation.setNavigationRoutes(listOf(mockk()))
        val routeProgress = mockk<RouteProgress> {
            every { durationRemaining } returns 100.0
            every { distanceRemaining } returns 500.0f
        }
        routeProgressObserverSlot.forEach { it.onRouteProgressChanged(routeProgress) }

        verify { navigationManager.updateTrip(any()) }
    }

    @Test
    fun `onStopNavigation should trigger clearing routes`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onAttached(mapboxNavigation)

        navigationManagerCallbackSlot.captured.onStopNavigation()

        verify { mapboxNavigation.setNavigationRoutes(emptyList()) }
    }

    @Test
    fun `onStopNavigation ends host navigation before clearing routes`() {
        val routesObserverSlot = slot<RoutesObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerRoutesObserver(capture(routesObserverSlot)) } just Runs
        }
        sut.onAttached(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf(mockk()) },
        )

        navigationManagerCallbackSlot.captured.onStopNavigation()

        verifyOrder {
            navigationManager.navigationEnded()
            mapboxNavigation.setNavigationRoutes(emptyList())
        }
    }

    @Test
    fun `onStopNavigation should trigger entering FreeDrive`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onAttached(mapboxNavigation)

        navigationManagerCallbackSlot.captured.onStopNavigation()

        verify { MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE) }
    }

    @Test
    fun `onStopNavigation should remain on unified navigation screen`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        every { MapboxScreenManager.current() } returns mockk {
            every { key } returns MapboxScreen.NAVIGATION
        }
        sut.onAttached(mapboxNavigation)

        navigationManagerCallbackSlot.captured.onStopNavigation()

        verify { mapboxNavigation.setNavigationRoutes(emptyList()) }
        verify(exactly = 0) { MapboxScreenManager.replaceTop(any()) }
    }

    @Test
    fun `onAutoDriveEnabled updates the autoDriveEnabledFlow state`() = coroutineRule.runBlockingTest {
        val resultsSlot = mutableListOf<Boolean>()
        val results = async { sut.autoDriveEnabledFlow.collect { resultsSlot.add(it) } }
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onAttached(mapboxNavigation)

        navigationManagerCallbackSlot.captured.onAutoDriveEnabled()

        results.cancelAndJoin()
        assertEquals(2, resultsSlot.size)
        assertFalse(resultsSlot[0])
        assertTrue(resultsSlot[1])
    }

    @Test
    fun `the state of autoDriveEnabledFlow can be observed after the event`() = coroutineRule.runBlockingTest {
        val resultsSlot = mutableListOf<Boolean>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)

        sut.onAttached(mapboxNavigation)
        navigationManagerCallbackSlot.captured.onAutoDriveEnabled()
        val results = async { sut.autoDriveEnabledFlow.collect { resultsSlot.add(it) } }

        results.cancelAndJoin()
        assertEquals(1, resultsSlot.size)
        assertTrue(resultsSlot[0])
    }

    @Test
    fun `updateTrip failure is logged without restarting navigation`() {
        val routes = mutableListOf<RoutesObserver>()
        val routeProgressObserverSlot = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mapboxNavigationMock(
            routes,
            routeProgressObserverSlot,
        )

        mapboxNavigation.setNavigationRoutes(listOf(mockk()))
        sut.onAttached(mapboxNavigation)
        navigationManager.navigationEnded()
        routeProgressObserverSlot.forEach { it.onRouteProgressChanged(mockk()) }
        sut.onDetached(mapboxNavigation)

        val expectedErrorMessage = "MapboxCarNavigationManager updateTrip failed"
        verifyOrder {
            navigationManager.navigationStarted()
            navigationManager.updateTrip(any())
            AndroidAutoLog.logAndroidAutoFailure(expectedErrorMessage, any())
        }
        verify(exactly = 1) { navigationManager.navigationStarted() }
        verify(exactly = 1) { navigationManager.updateTrip(any()) }
    }

    @Test
    fun `updateTrip should not be called while detaching MapboxNavigation`() = coroutineRule.runBlockingTest {
        val routesSlot = mutableListOf<RoutesObserver>()
        val routeProgressObserverSlot = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mapboxNavigationMock(
            routesSlot,
            routeProgressObserverSlot,
        )

        mapboxNavigation.setNavigationRoutes(listOf(mockk()))
        sut.onAttached(mapboxNavigation)
        routeProgressObserverSlot.forEach { it.onRouteProgressChanged(mockk()) }
        sut.onDetached(mapboxNavigation)

        verify(exactly = 1) { navigationManager.updateTrip(any()) }
    }

    @Test
    fun `updateTrip will not happen when MapboxNavigation emits progress while stopped`() = coroutineRule.runBlockingTest {
        val routesSlot = mutableListOf<RoutesObserver>()
        val routeProgressObserverSlot = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mapboxNavigationMock(
            routesSlot,
            routeProgressObserverSlot,
        )

        mapboxNavigation.setNavigationRoutes(listOf(mockk()))
        sut.onAttached(mapboxNavigation)
        routeProgressObserverSlot.forEach { it.onRouteProgressChanged(mockk()) }
        mapboxNavigation.setNavigationRoutes(emptyList())
        routeProgressObserverSlot.forEach { it.onRouteProgressChanged(mockk()) }
        sut.onDetached(mapboxNavigation)

        verify(exactly = 1) { navigationManager.updateTrip(any()) }
    }

    @Test
    fun `identical trips do not trigger redundant updates`() {
        val routesSlot = mutableListOf<RoutesObserver>()
        val progressObservers = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation = mapboxNavigationMock(routesSlot, progressObservers)
        val trip = trip(100)
        val routeProgress = routeProgress()
        every { CarManeuverMapper.from(any<RouteProgress>(), any()) } returns trip

        sut.onAttached(mapboxNavigation)
        mapboxNavigation.setNavigationRoutes(listOf(mockk()))
        progressObservers.forEach { it.onRouteProgressChanged(routeProgress) }
        progressObservers.forEach { it.onRouteProgressChanged(routeProgress) }

        verify(exactly = 1) { navigationManager.updateTrip(trip) }
        verify(exactly = 1) { CarManeuverMapper.from(any<RouteProgress>(), any()) }
    }

    @Test
    fun `non-meaningful estimate changes are rate limited`() {
        val routesSlot = mutableListOf<RoutesObserver>()
        val progressObservers = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation = mapboxNavigationMock(routesSlot, progressObservers)
        var now = 0L
        val manager = MapboxCarNavigationManager(carContext) { now }
        every { CarManeuverMapper.from(any<RouteProgress>(), any()) } returns trip(100)
        val routeProgresses = listOf(
            routeProgress(durationRemaining = 100.0),
            routeProgress(durationRemaining = 99.0),
            routeProgress(durationRemaining = 98.0),
        )

        manager.onAttached(mapboxNavigation)
        mapboxNavigation.setNavigationRoutes(listOf(mockk()))
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[0]) }
        now = 999L
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[1]) }
        now = 1_000L
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[2]) }

        verify(exactly = 2) { navigationManager.updateTrip(any()) }
        verify(exactly = 2) { CarManeuverMapper.from(any<RouteProgress>(), any()) }
    }

    @Test
    fun `meaningful estimate changes bypass rate limiting`() {
        val routesSlot = mutableListOf<RoutesObserver>()
        val progressObservers = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation = mapboxNavigationMock(routesSlot, progressObservers)
        var now = 0L
        val manager = MapboxCarNavigationManager(carContext) { now }
        every { CarManeuverMapper.from(any<RouteProgress>(), any()) } returns trip(100)
        val routeProgresses = listOf(
            routeProgress(durationRemaining = 100.0),
            routeProgress(durationRemaining = 90.0),
        )

        manager.onAttached(mapboxNavigation)
        mapboxNavigation.setNavigationRoutes(listOf(mockk()))
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[0]) }
        now = 1L
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[1]) }

        verify(exactly = 2) { navigationManager.updateTrip(any()) }
        verify(exactly = 2) { CarManeuverMapper.from(any<RouteProgress>(), any()) }
    }

    @Test
    fun `formatted distance changes bypass rate limiting`() {
        val routesSlot = mutableListOf<RoutesObserver>()
        val progressObservers = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation = mapboxNavigationMock(routesSlot, progressObservers)
        var now = 0L
        val manager = MapboxCarNavigationManager(carContext) { now }
        every { CarManeuverMapper.from(any<RouteProgress>(), any()) } returns trip(100)
        val routeProgresses = listOf(
            routeProgress(distanceRemaining = 200.0f),
            routeProgress(distanceRemaining = 199.0f),
            routeProgress(distanceRemaining = 180.0f),
            routeProgress(distanceRemaining = 149.0f),
        )

        manager.onAttached(mapboxNavigation)
        mapboxNavigation.setNavigationRoutes(listOf(mockk()))
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[0]) }
        now = 1L
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[1]) }
        now = 2L
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[2]) }
        now = 3L
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[3]) }

        verify(exactly = 3) { navigationManager.updateTrip(any()) }
        verify(exactly = 3) { CarManeuverMapper.from(any<RouteProgress>(), any()) }
    }

    @Test
    fun `maneuver transitions bypass rate limiting`() {
        val routesSlot = mutableListOf<RoutesObserver>()
        val progressObservers = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation = mapboxNavigationMock(routesSlot, progressObservers)
        var now = 0L
        val manager = MapboxCarNavigationManager(carContext) { now }
        every { CarManeuverMapper.from(any<RouteProgress>(), any()) } returns trip(100)
        val routeProgresses = listOf(
            routeProgress(stepIndex = 0),
            routeProgress(stepIndex = 1),
        )

        manager.onAttached(mapboxNavigation)
        mapboxNavigation.setNavigationRoutes(listOf(mockk()))
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[0]) }
        now = 1L
        progressObservers.forEach { it.onRouteProgressChanged(routeProgresses[1]) }

        verify(exactly = 2) { navigationManager.updateTrip(any()) }
        verify(exactly = 2) { CarManeuverMapper.from(any<RouteProgress>(), any()) }
    }

    @Test
    fun `detaching active navigation ends it before clearing its callback`() {
        val routesSlot = mutableListOf<RoutesObserver>()
        val progressObservers = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation = mapboxNavigationMock(routesSlot, progressObservers)
        sut.onAttached(mapboxNavigation)
        mapboxNavigation.setNavigationRoutes(listOf(mockk()))

        sut.onDetached(mapboxNavigation)

        verifyOrder {
            navigationManager.navigationEnded()
            navigationManager.clearNavigationManagerCallback()
        }
    }

    private fun trip(
        remainingTimeSeconds: Long,
        tripSteps: List<Step> = emptyList(),
    ): Trip {
        val estimate = mockk<TravelEstimate> {
            every { remainingDistance } returns null
            every { getRemainingTimeSeconds() } returns remainingTimeSeconds
        }
        return mockk {
            every { steps } returns tripSteps
            every { destinations } returns emptyList()
            every { stepTravelEstimates } returns listOf(estimate)
            every { destinationTravelEstimates } returns listOf(estimate)
        }
    }

    private fun routeProgress(
        distanceRemaining: Float = 200.0f,
        durationRemaining: Double = 100.0,
        stepIndex: Int = 0,
    ): RouteProgress {
        val stepProgress = mockk<RouteStepProgress> {
            every { this@mockk.stepIndex } returns stepIndex
            every { instructionIndex } returns 0
            every { this@mockk.distanceRemaining } returns distanceRemaining
            every { this@mockk.durationRemaining } returns durationRemaining
        }
        val legProgress = mockk<RouteLegProgress> {
            every { legIndex } returns 0
            every { currentStepProgress } returns stepProgress
        }
        return mockk {
            every { navigationRoute } returns mockk<NavigationRoute> {
                every { id } returns "route"
            }
            every { currentLegProgress } returns legProgress
            every { bannerInstructions } returns null
            every { this@mockk.distanceRemaining } returns distanceRemaining
            every { this@mockk.durationRemaining } returns durationRemaining
        }
    }

    private fun mapboxNavigationMock(
        routesSlot: MutableList<RoutesObserver>,
        routeProgressObserverSlot: MutableList<RouteProgressObserver>,
    ): MapboxNavigation {
        val distanceFormatterOptions = mockk<DistanceFormatterOptions> {
            every { roundingIncrement } returns Rounding.INCREMENT_DISTANCE_DEPENDENT
            every { unitType } returns UnitType.METRIC
            every { locale } returns Locale.US
        }
        val navigationOptions = mockk<NavigationOptions> {
            every { this@mockk.distanceFormatterOptions } returns distanceFormatterOptions
        }
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { this@mockk.navigationOptions } returns navigationOptions
            every { registerRoutesObserver(any()) } answers {
                routesSlot.add(firstArg())
                firstArg<RoutesObserver>().onRoutesChanged(
                    mockk { every { navigationRoutes } returns getNavigationRoutes() },
                )
            }
            every { unregisterRoutesObserver(any()) } answers {
                routesSlot.remove(firstArg())
            }
            every { registerRouteProgressObserver(any()) } answers {
                routeProgressObserverSlot.add(firstArg())
            }
            every { unregisterRouteProgressObserver(any()) } answers {
                routeProgressObserverSlot.remove(firstArg())
            }
            every { unregisterRoutesObserver(any()) } answers {
                // Correct ordering when unregistering the observer will make it so
                // routeProgressObserverSlot is empty.
                routeProgressObserverSlot.forEach { it.onRouteProgressChanged(mockk()) }
                routesSlot.remove(firstArg())
            }
            every { setNavigationRoutes(any()) } answers {
                every { getNavigationRoutes() } returns firstArg()
                routesSlot.forEach {
                    it.onRoutesChanged(mockk { every { navigationRoutes } returns firstArg() })
                }
            }
        }
        routesSlot.add { routes ->
            every { mapboxNavigation.getNavigationRoutes() } returns routes.navigationRoutes
        }
        return mapboxNavigation
    }
}
