package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class TripSessionStarterStateControllerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var testStore: TestStore
    private lateinit var sut: TripSessionStarterStateController

    @Before
    fun setup() {
        testStore = spyk(TestStore())
        sut = TripSessionStarterStateController(testStore)
    }

    @Test
    fun `on OnLocationPermission action should update TripSessionStarterState`() {
        testStore.dispatch(TripSessionStarterAction.OnLocationPermission(true))

        assertEquals(true, testStore.state.value.tripSession.isLocationPermissionGranted)
    }

    @Test
    fun `on EnableReplayTripSession action should update TripSessionStarterState`() {
        testStore.dispatch(TripSessionStarterAction.EnableReplayTripSession)

        assertEquals(true, testStore.state.value.tripSession.isReplayEnabled)
    }

    @Test
    fun `on EnableTripSession action should update TripSessionStarterState`() {
        testStore.dispatch(TripSessionStarterAction.EnableTripSession)

        assertEquals(false, testStore.state.value.tripSession.isReplayEnabled)
    }
}
