package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequest
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequestCallback
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.search.SearchCarContext
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesListOnMapScreenTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val toast = mockk<CarToast>(relaxUnitFun = true)
    private val aCarContext = mockk<CarContext>(relaxed = true) {
        every { getString(R.string.car_search_no_results) } returns "No results"
        every { getCarService(AppManager::class.java) } returns mockk(relaxed = true)
    }
    private val routePreviewRequest = mockk<CarRoutePreviewRequest>(relaxed = true)
    private val searchCarContext: SearchCarContext = mockk(relaxed = true) {
        every { carContext } returns aCarContext
        every { routePreviewRequest } returns this@PlacesListOnMapScreenTest.routePreviewRequest
    }
    private val placeClicks = MutableSharedFlow<PlaceRecord>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val manager = mockk<PlacesListOnMapManager>(relaxed = true) {
        every { placeRecords } returns MutableStateFlow(emptyList())
        every { placeClicks } returns this@PlacesListOnMapScreenTest.placeClicks
        every { state } returns MutableStateFlow(PlacesListState.Loading)
    }
    private val sut = PlacesListOnMapScreen(
        searchCarContext,
        mockk(relaxed = true),
        MapboxScreen.SEARCH,
        manager,
    )
    private val lifecycle = sut.lifecycle as LifecycleRegistry

    @Before
    fun setUp() {
        mockkStatic(CarToast::class)
        every { CarToast.makeText(any(), any<CharSequence>(), any()) } returns toast
    }

    @Test
    fun `every tap requests the route, including a repeated tap on the same place`() {
        lifecycle.currentState = Lifecycle.State.RESUMED
        val place = mockk<PlaceRecord>()

        placeClicks.tryEmit(place)
        placeClicks.tryEmit(place)

        verify(exactly = 2) { routePreviewRequest.request(place, any()) }
    }

    @Test
    fun `a tap is not requested again when the screen resumes`() {
        lifecycle.currentState = Lifecycle.State.RESUMED
        val place = mockk<PlaceRecord>()
        placeClicks.tryEmit(place)

        lifecycle.currentState = Lifecycle.State.STARTED
        lifecycle.currentState = Lifecycle.State.RESUMED

        verify(exactly = 1) { routePreviewRequest.request(place, any()) }
    }

    @Test
    fun `a route request failure is shown as a toast`() {
        val callback = slot<CarRoutePreviewRequestCallback>()
        every { routePreviewRequest.request(any(), capture(callback)) } just Runs
        lifecycle.currentState = Lifecycle.State.RESUMED
        placeClicks.tryEmit(mockk())

        callback.captured.onNoRoutesFound()

        verify { CarToast.makeText(aCarContext, "No results", CarToast.LENGTH_LONG) }
        verify { toast.show() }
    }
}
