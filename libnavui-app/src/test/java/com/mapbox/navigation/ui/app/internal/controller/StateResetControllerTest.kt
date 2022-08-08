package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.navigation.NavigationStateAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

internal class StateResetControllerTest {

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var store: TestStore
    private lateinit var sut: StateResetController
    private lateinit var ignoreTripSessionUpdates: AtomicBoolean

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
        ignoreTripSessionUpdates = AtomicBoolean(false)
        sut = StateResetController(store, ignoreTripSessionUpdates)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAttached, should dispatch actions that end navigation only when TripSessionState transitions from STARTED to STOPPED`() {
        val observer = slot<TripSessionStateObserver>()
        every {
            mapboxNavigation.registerTripSessionStateObserver(capture(observer))
        } returns Unit
        sut.onAttached(mapboxNavigation)

        // STOPPED -> STOPPED
        observer.captured.onSessionStateChanged(TripSessionState.STOPPED)
        verify(exactly = 0) { store.dispatch(any()) }

        // STOPPED -> STARTED
        observer.captured.onSessionStateChanged(TripSessionState.STARTED)
        verify(exactly = 0) { store.dispatch(any()) }

        // STARTED -> STOPPED
        observer.captured.onSessionStateChanged(TripSessionState.STOPPED)
        verify(exactly = 1) {
            store.dispatch(RoutesAction.SetRoutes(emptyList()))
            store.dispatch(RoutePreviewAction.Ready(emptyList()))
            store.dispatch(DestinationAction.SetDestination(null))
            store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
        }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAttached, should NOT dispatch end navigation action when ignoreTripSessionUpdates is true`() {
        ignoreTripSessionUpdates.set(true)
        val observer = slot<TripSessionStateObserver>()
        every { mapboxNavigation.registerTripSessionStateObserver(capture(observer)) } returns Unit
        every { mapboxNavigation.getTripSessionState() } returns TripSessionState.STARTED
        sut.onAttached(mapboxNavigation)

        // STARTED -> STOPPED
        observer.captured.onSessionStateChanged(TripSessionState.STOPPED)
        verify(exactly = 0) {
            store.dispatch(RoutesAction.SetRoutes(emptyList()))
            store.dispatch(RoutePreviewAction.Ready(emptyList()))
            store.dispatch(DestinationAction.SetDestination(null))
            store.dispatch(NavigationStateAction.Update(NavigationState.FreeDrive))
        }
    }

    @Test
    fun `onDetached should reset Store state`() {
        sut.onAttached(mapboxNavigation)
        sut.onDetached(mapboxNavigation)

        assertEquals(State(), store.state.value)
    }
}
