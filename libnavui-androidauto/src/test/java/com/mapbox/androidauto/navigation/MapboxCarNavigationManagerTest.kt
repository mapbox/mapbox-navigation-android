package com.mapbox.androidauto.navigation

import androidx.car.app.CarContext
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import com.mapbox.androidauto.navigation.maneuver.CarManeuverMapper
import com.mapbox.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.telemetry.sendCustomEvent
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
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
            every { clearNavigationManagerCallback() } throws IllegalStateException()
            every { updateTrip(any()) } just Runs
        }
        every { navigationEnded() } answers {
            every { clearNavigationManagerCallback() } just Runs
            every { updateTrip(any()) } throws IllegalStateException()
        }
    }
    private val carContext: CarContext = mockk {
        every { getCarService(NavigationManager::class.java) } returns navigationManager
    }

    private val sut = MapboxCarNavigationManager(carContext)

    @Before
    fun setup() {
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
                "1.0.0"
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
                "1.0.0"
            )
        }
    }

    @Test
    fun `TripSessionState STARTED should trigger navigationStarted`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val tripObserverSlot = slot<TripSessionStateObserver>()
        every {
            mapboxNavigation.registerTripSessionStateObserver(capture(tripObserverSlot))
        } just Runs
        sut.onAttached(mapboxNavigation)

        tripObserverSlot.captured.onSessionStateChanged(TripSessionState.STARTED)

        verify { navigationManager.navigationStarted() }
    }

    @Test
    fun `TripSessionState STOPPED should trigger navigationEnded`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val tripObserverSlot = slot<TripSessionStateObserver>()
        every {
            mapboxNavigation.registerTripSessionStateObserver(capture(tripObserverSlot))
        } just Runs
        sut.onAttached(mapboxNavigation)

        tripObserverSlot.captured.onSessionStateChanged(TripSessionState.STOPPED)

        verify { navigationManager.navigationEnded() }
    }

    @Test
    fun `RouteProgress should trigger updateTrip`() {
        val tripSessionStateSlot = mutableListOf<TripSessionStateObserver>()
        val routeProgressObserverSlot = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mapboxNavigationMock(
            tripSessionStateSlot,
            routeProgressObserverSlot
        )

        sut.onAttached(mapboxNavigation)
        mapboxNavigation.startTripSession()
        val routeProgress = mockk<RouteProgress> {
            every { durationRemaining } returns 100.0
            every { distanceRemaining } returns 500.0f
        }
        routeProgressObserverSlot.forEach { it.onRouteProgressChanged(routeProgress) }

        verify { navigationManager.updateTrip(any()) }
    }

    @Test
    fun `onStopNavigation should trigger stopTripSession`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        sut.onAttached(mapboxNavigation)

        navigationManagerCallbackSlot.captured.onStopNavigation()

        verify { mapboxNavigation.stopTripSession() }
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
    fun `updateTrip should not be called while detaching MapboxNavigation`() = coroutineRule.runBlockingTest {
        val tripSessionStateSlot = mutableListOf<TripSessionStateObserver>()
        val routeProgressObserverSlot = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mapboxNavigationMock(
            tripSessionStateSlot,
            routeProgressObserverSlot
        )

        mapboxNavigation.startTripSession()
        sut.onAttached(mapboxNavigation)
        routeProgressObserverSlot.forEach { it.onRouteProgressChanged(mockk()) }
        sut.onDetached(mapboxNavigation)

        verify(exactly = 1) { navigationManager.updateTrip(any()) }
    }

    @Test
    fun `updateTrip will not happen when MapboxNavigation emits progress while stopped`() = coroutineRule.runBlockingTest {
        val tripSessionStateSlot = mutableListOf<TripSessionStateObserver>()
        val routeProgressObserverSlot = mutableListOf<RouteProgressObserver>()
        val mapboxNavigation: MapboxNavigation = mapboxNavigationMock(
            tripSessionStateSlot,
            routeProgressObserverSlot
        )

        mapboxNavigation.startTripSession()
        sut.onAttached(mapboxNavigation)
        routeProgressObserverSlot.forEach { it.onRouteProgressChanged(mockk()) }
        mapboxNavigation.stopTripSession()
        routeProgressObserverSlot.forEach { it.onRouteProgressChanged(mockk()) }
        sut.onDetached(mapboxNavigation)

        verify(exactly = 1) { navigationManager.updateTrip(any()) }
    }

    private fun mapboxNavigationMock(
        tripSessionStateSlot: MutableList<TripSessionStateObserver>,
        routeProgressObserverSlot: MutableList<RouteProgressObserver>
    ): MapboxNavigation {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerTripSessionStateObserver(any()) } answers {
                tripSessionStateSlot.add(firstArg())
                firstArg<TripSessionStateObserver>().onSessionStateChanged(getTripSessionState())
            }
            every { unregisterTripSessionStateObserver(any()) } answers {
                tripSessionStateSlot.remove(firstArg())
            }
            every { registerRouteProgressObserver(any()) } answers {
                routeProgressObserverSlot.add(firstArg())
            }
            every { unregisterRouteProgressObserver(any()) } answers {
                routeProgressObserverSlot.remove(firstArg())
            }
            every { unregisterTripSessionStateObserver(any()) } answers {
                // Correct ordering when unregistering the observer will make it so
                // routeProgressObserverSlot is empty.
                routeProgressObserverSlot.forEach { it.onRouteProgressChanged(mockk()) }
                tripSessionStateSlot.remove(firstArg())
            }
            every { startTripSession() } answers {
                every { getTripSessionState() } returns TripSessionState.STARTED
                tripSessionStateSlot.forEach { it.onSessionStateChanged(TripSessionState.STARTED) }
            }
            every { stopTripSession() } answers {
                every { getTripSessionState() } returns TripSessionState.STOPPED
                tripSessionStateSlot.forEach { it.onSessionStateChanged(TripSessionState.STOPPED) }
            }
        }
        tripSessionStateSlot.add { tripSessionState ->
            every { mapboxNavigation.getTripSessionState() } returns tripSessionState
        }
        return mapboxNavigation
    }
}
