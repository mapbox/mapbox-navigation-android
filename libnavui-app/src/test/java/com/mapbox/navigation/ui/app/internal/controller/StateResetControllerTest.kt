package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class StateResetControllerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var store: TestStore
    private lateinit var sut: StateResetController

    @Before
    fun setUp() {
        mapboxNavigation = mockk(relaxed = true) {
            every { getTripSessionState() } returns TripSessionState.STOPPED
        }
        store = spyk(TestStore())
        store.setState(
            State(
                destination = Destination(Point.fromLngLat(1.0, 2.0)),
                navigation = NavigationState.ActiveNavigation
            )
        )
        sut = StateResetController(store)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAttached, should only reset store State when TripSessionState transitions from STARTED to STOPPED`() =
        coroutineRule.runBlockingTest {
            val observer = slot<TripSessionStateObserver>()
            every {
                mapboxNavigation.registerTripSessionStateObserver(capture(observer))
            } returns Unit
            sut.onAttached(mapboxNavigation)

            // STOPPED -> STOPPED
            observer.captured.onSessionStateChanged(TripSessionState.STOPPED)
            coroutineRule.testDispatcher.advanceTimeBy(200)
            verify(exactly = 0) { store.reset() }

            // STOPPED -> STARTED
            observer.captured.onSessionStateChanged(TripSessionState.STARTED)
            coroutineRule.testDispatcher.advanceTimeBy(200)
            verify(exactly = 0) { store.reset() }

            // STARTED -> STOPPED
            observer.captured.onSessionStateChanged(TripSessionState.STOPPED)
            coroutineRule.testDispatcher.advanceTimeBy(200)
            verify(exactly = 1) { store.reset() }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `onAttached, should only reset NavigationRoutes when TripSessionState transitions from STARTED to STOPPED`() =
        coroutineRule.runBlockingTest {
            val observer = slot<TripSessionStateObserver>()
            every {
                mapboxNavigation.registerTripSessionStateObserver(capture(observer))
            } returns Unit
            sut.onAttached(mapboxNavigation)

            // STOPPED -> STOPPED
            observer.captured.onSessionStateChanged(TripSessionState.STOPPED)
            coroutineRule.testDispatcher.advanceTimeBy(200)
            verify(exactly = 0) { mapboxNavigation.setNavigationRoutes(emptyList()) }

            // STOPPED -> STARTED
            observer.captured.onSessionStateChanged(TripSessionState.STARTED)
            coroutineRule.testDispatcher.advanceTimeBy(200)
            verify(exactly = 0) { mapboxNavigation.setNavigationRoutes(emptyList()) }

            // STARTED -> STOPPED
            observer.captured.onSessionStateChanged(TripSessionState.STOPPED)
            coroutineRule.testDispatcher.advanceTimeBy(200)
            verify(exactly = 1) { mapboxNavigation.setNavigationRoutes(emptyList()) }
        }

    @Test
    fun `onDetached should reset Store state`() {
        sut.onAttached(mapboxNavigation)
        sut.onDetached(mapboxNavigation)

        assertEquals(State(), store.state.value)
    }
}
