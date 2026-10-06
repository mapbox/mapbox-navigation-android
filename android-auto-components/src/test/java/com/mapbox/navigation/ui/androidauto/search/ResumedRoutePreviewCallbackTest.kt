package com.mapbox.navigation.ui.androidauto.search

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.common.dispatchers.SdkDispatchersTestRule
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequest
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// The callback under test pushes the deprecated MapboxScreen.ROUTE_PREVIEW of the legacy flow.
@Suppress("DEPRECATION")
@OptIn(ExperimentalCoroutinesApi::class)
class ResumedRoutePreviewCallbackTest {

    // Posted work waits in the queue until runPosted(), so the tests can tell what runs inside
    // the lifecycle callback and what runs after it.
    @get:Rule
    val sdkDispatchersRule = SdkDispatchersTestRule(StandardTestDispatcher())

    private val lifecycleRegistry = LifecycleRegistry.createUnsafe(mockk<LifecycleOwner>())
    private val mapboxScreenManager: MapboxScreenManager = mockk {
        every { isScreenBelowTop(any()) } returns false
    }
    private val previewedPlace = MutableStateFlow<PlaceRecord?>(null)
    private val routePreviewRequest: CarRoutePreviewRequest = mockk(relaxed = true) {
        every { repository } returns mockk {
            every { placeRecord } returns previewedPlace
        }
    }
    private val errors = mutableListOf<Int>()

    private val sut = ResumedRoutePreviewCallback(
        lifecycleRegistry,
        mapboxScreenManager,
        routePreviewRequest,
    ) { errors.add(it) }

    @Before
    fun setup() {
        mockkObject(MapboxScreenManager)
        every { MapboxScreenManager.push(any()) } just runs
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    private fun routesReady(place: PlaceRecord = mockk()) {
        previewedPlace.value = place
        sut.onRoutesReady(place, listOf(mockk()))
    }

    private fun runPosted() {
        sdkDispatchersRule.dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `routes ready while resumed pushes the route preview immediately`() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        routesReady()

        verify(exactly = 1) { MapboxScreenManager.push(MapboxScreen.ROUTE_PREVIEW) }
    }

    @Test
    fun `routes ready while resumed above navigation goes back instead of pushing`() {
        every { mapboxScreenManager.isScreenBelowTop(MapboxScreen.NAVIGATION) } returns true
        every { mapboxScreenManager.goBack() } returns true
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        routesReady()

        verify(exactly = 1) { mapboxScreenManager.goBack() }
        verify(exactly = 0) { MapboxScreenManager.push(any()) }
    }

    @Test
    fun `routes ready while not resumed is applied only when the screen resumes`() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        routesReady()
        verify(exactly = 0) { MapboxScreenManager.push(any()) }

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        runPosted()
        verify(exactly = 1) { MapboxScreenManager.push(MapboxScreen.ROUTE_PREVIEW) }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        runPosted()
        verify(exactly = 1) { MapboxScreenManager.push(MapboxScreen.ROUTE_PREVIEW) }
    }

    @Test
    fun `a kept result is applied after the resume callback, not inside it`() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        routesReady()

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        verify(exactly = 0) { MapboxScreenManager.push(any()) }

        runPosted()
        verify(exactly = 1) { MapboxScreenManager.push(MapboxScreen.ROUTE_PREVIEW) }
    }

    @Test
    fun `a kept result stays kept when the screen is paused before it is applied`() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        routesReady()

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        runPosted()
        verify(exactly = 0) { MapboxScreenManager.push(any()) }

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        runPosted()
        verify(exactly = 1) { MapboxScreenManager.push(MapboxScreen.ROUTE_PREVIEW) }
    }

    @Test
    fun `only the latest result received while not resumed is applied`() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        routesReady()
        sut.onNoRoutesFound()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        runPosted()

        verify(exactly = 0) { MapboxScreenManager.push(any()) }
        assertEquals(listOf(R.string.car_search_no_results), errors)
    }

    @Test
    fun `destroying the screen drops the pending result and cancels its request`() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        routesReady()

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        sut.onNetworkFailure()

        verify(exactly = 1) { routePreviewRequest.cancelRequest(sut) }
        verify(exactly = 0) { MapboxScreenManager.push(any()) }
        assertEquals(emptyList<Int>(), errors)
    }

    @Test
    fun `each failure maps to a message`() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        sut.onUnknownCurrentLocation()
        sut.onDestinationLocationUnknown()
        sut.onNoRoutesFound()
        sut.onNetworkFailure()
        sut.onRoutingFailure(emptyList())

        assertEquals(
            listOf(
                R.string.car_search_unknown_current_location,
                R.string.car_search_unknown_search_location,
                R.string.car_search_no_results,
                R.string.car_search_error,
                R.string.car_search_error,
            ),
            errors,
        )
    }

    @Test
    fun `a kept result is dropped when another place was previewed meanwhile`() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        routesReady()

        previewedPlace.value = mockk()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        runPosted()

        verify(exactly = 0) { MapboxScreenManager.push(any()) }
    }

    @Test
    fun `a kept error is dropped when another place was previewed meanwhile`() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        sut.onNoRoutesFound()

        previewedPlace.value = mockk()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        runPosted()

        assertEquals(emptyList<Int>(), errors)
    }

    @Test
    fun `showError follows the same rule as route errors`() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        sut.showError(R.string.car_search_error)
        assertEquals(listOf(R.string.car_search_error), errors)

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        sut.showError(R.string.car_search_unknown_search_location)
        assertEquals(listOf(R.string.car_search_error), errors)

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        runPosted()
        assertEquals(
            listOf(R.string.car_search_error, R.string.car_search_unknown_search_location),
            errors,
        )
    }
}
