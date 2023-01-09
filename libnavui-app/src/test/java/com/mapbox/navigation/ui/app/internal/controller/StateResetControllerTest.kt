package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class StateResetControllerTest {

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var store: TestStore
    private lateinit var sut: StateResetController

    @Before
    fun setUp() {
        mapboxNavigation = mockk(relaxed = true)
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
    fun `onDetached should reset Store state`() {
        sut.onAttached(mapboxNavigation)
        sut.onDetached(mapboxNavigation)

        assertEquals(State(), store.state.value)
    }
}
