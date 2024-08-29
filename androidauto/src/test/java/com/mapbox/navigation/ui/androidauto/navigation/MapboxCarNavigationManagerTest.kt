package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.CarContext
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.internal.telemetry.sendCustomEvent
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

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
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
        mockkObject(CarManeuverMapper)
        every { CarManeuverMapper.from(any<RouteProgress>(), any()) } returns mockk()
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
    fun `onAttached should trigger telemetry event that android auto started`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onAttached(mapboxNavigation)

        verify {
            mapboxNavigation.sendCustomEvent(
                "Android Auto : started",
                "analytics",
                "1.0.0",
            )
        }
    }

    @Test
    fun `onAttached should trigger telemetry event that android auto stopped`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onDetached(mapboxNavigation)

        verify {
            mapboxNavigation.sendCustomEvent(
                "Android Auto : stopped",
                "analytics",
                "1.0.0",
            )
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
    fun `onStopNavigation should trigger entering FreeDrive`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onAttached(mapboxNavigation)

        navigationManagerCallbackSlot.captured.onStopNavigation()

        verify { MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE) }
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
    fun `updateTrip throws IllegalStateException because navigation is not started`() {
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

        // Restart navigationStarted when the error is thrown.
        val expectedErrorMessage = "MapboxCarNavigationManager updateTrip failed"
        verifyOrder {
            navigationManager.navigationStarted()
            navigationManager.updateTrip(any())
            AndroidAutoLog.logAndroidAutoFailure(expectedErrorMessage, any())
            navigationManager.navigationStarted()
            navigationManager.updateTrip(any())
        }
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

    private fun mapboxNavigationMock(
        routesSlot: MutableList<RoutesObserver>,
        routeProgressObserverSlot: MutableList<RouteProgressObserver>,
    ): MapboxNavigation {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
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
