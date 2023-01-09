package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.trip.MapboxTripStarter
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class TripSessionStarterStateControllerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var mapboxTripStarter: MapboxTripStarter
    private lateinit var sut: TripSessionStarterStateController
    private val store: Store = mockk(relaxed = true)

    @Before
    fun setup() {
        mapboxTripStarter = mockk(relaxed = true)

        mockkObject(MapboxTripStarter.Companion)
        every { MapboxTripStarter.getRegisteredInstance() } returns mapboxTripStarter
        sut = TripSessionStarterStateController(store)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `constructor will register to the store`() {
        verify(exactly = 1) { store.register(sut) }
    }

    @Test
    fun `on OnLocationPermission action should update TripSessionStarterState`() {
        sut.process(mockk(), TripSessionStarterAction.RefreshLocationPermissions)

        verify(exactly = 1) { mapboxTripStarter.refreshLocationPermissions() }
    }

    @Test
    fun `on EnableReplayTripSession action should update TripSessionStarterState`() {
        sut.process(mockk(), TripSessionStarterAction.EnableReplayTripSession)

        verify(exactly = 1) { mapboxTripStarter.enableReplayRoute() }
    }

    @Test
    fun `on EnableTripSession action should update TripSessionStarterState`() {
        sut.process(mockk(), TripSessionStarterAction.EnableTripSession)

        verify(exactly = 1) { mapboxTripStarter.enableMapMatching() }
    }
}
