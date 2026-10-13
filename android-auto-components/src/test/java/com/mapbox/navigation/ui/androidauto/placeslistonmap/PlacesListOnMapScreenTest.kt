package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.internal.setHeaderCompat
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequest
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequestCallback
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.search.SearchCarContext
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class PlacesListOnMapScreenTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val toast = mockk<CarToast>(relaxUnitFun = true)
    private var apiLevel = 7
    private val appManager = mockk<AppManager>(relaxed = true)
    private val aCarContext = mockk<CarContext>(relaxed = true) {
        every { getString(R.string.car_search_no_results) } returns "No results"
        every { getString(R.string.car_search_error) } returns "Error"
        every { getCarService(AppManager::class.java) } returns appManager
        every { carAppApiLevel } answers { apiLevel }
    }
    private val actionStrip = ActionStrip.Builder()
        .addAction(Action.Builder().setTitle("feedback").build())
        .build()
    private val routePreviewRequest = mockk<CarRoutePreviewRequest>(relaxed = true)
    private val searchCarContext: SearchCarContext = mockk(relaxed = true) {
        every { carContext } returns aCarContext
        every { routePreviewRequest } returns this@PlacesListOnMapScreenTest.routePreviewRequest
        every {
            mapboxCarContext.options.actionStripProvider.getActionStrip(any(), any())
        } returns actionStrip
    }
    private val placeClicks = MutableSharedFlow<PlaceRecord>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val placesState = MutableStateFlow<PlacesListState>(PlacesListState.Loading)
    private val manager = mockk<PlacesListOnMapManager>(relaxed = true) {
        every { placeRecords } returns MutableStateFlow(emptyList())
        every { placeClicks } returns this@PlacesListOnMapScreenTest.placeClicks
        every { state } returns placesState
    }
    private val sut = PlacesListOnMapScreen(
        searchCarContext,
        mockk(relaxed = true),
        MapboxScreen.FAVORITES,
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

    @Test
    fun `loading places show a loading list over the map from Car API 7`() {
        val template = sut.onGetTemplate() as MapWithContentTemplate

        val content = template.contentTemplate as ListTemplate
        assertTrue(content.isLoading)
        assertEquals(Action.BACK, content.header!!.startHeaderAction)
        assertNull(content.header!!.title)
        assertEquals(actionStrip, template.actionStrip)
        verify {
            searchCarContext.mapboxCarContext.options.actionStripProvider
                .getActionStrip(sut, MapboxScreen.FAVORITES)
        }
    }

    @Test
    fun `loaded places are listed over the map from Car API 7`() {
        val itemList = placeItemList(withRowImages = true)
        placesState.value = PlacesListState.Loaded(itemList)

        val template = sut.onGetTemplate() as MapWithContentTemplate

        val content = template.contentTemplate as ListTemplate
        assertFalse(content.isLoading)
        assertEquals(itemList.items, content.singleList!!.items)
    }

    @Test
    fun `no places show the no results message from Car API 7`() {
        placesState.value = PlacesListState.Loaded(ItemList.Builder().build())

        val template = sut.onGetTemplate() as MapWithContentTemplate

        val content = template.contentTemplate as ListTemplate
        assertEquals("No results", content.singleList!!.noItemsMessage.toString())
    }

    @Test
    fun `failed places show the error message from Car API 7`() {
        placesState.value = PlacesListState.Failed

        val template = sut.onGetTemplate() as MapWithContentTemplate

        val content = template.contentTemplate as ListTemplate
        assertEquals("Error", content.singleList!!.noItemsMessage.toString())
    }

    @Suppress("DEPRECATION")
    @Test
    fun `loading places show the legacy place list below Car API 7`() {
        apiLevel = 6

        val template = sut.onGetTemplate() as PlaceListNavigationTemplate

        assertTrue(template.isLoading)
        assertEquals(Action.BACK, template.header!!.startHeaderAction)
        assertEquals(actionStrip, template.actionStrip)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `places use the legacy header action below Car API 5`() {
        apiLevel = 4
        val itemList = placeItemList(withRowImages = false)
        placesState.value = PlacesListState.Loaded(itemList)

        val template = sut.onGetTemplate() as PlaceListNavigationTemplate

        assertNull(template.header)
        assertEquals(Action.BACK, template.headerAction)
        assertEquals(itemList.items, template.itemList!!.items)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `loaded places use the header and the legacy back at Car API 5`() {
        apiLevel = 5
        val itemList = placeItemList(withRowImages = false)
        placesState.value = PlacesListState.Loaded(itemList)

        val template = sut.onGetTemplate() as PlaceListNavigationTemplate

        assertEquals(Action.BACK, template.header!!.startHeaderAction)
        assertEquals(Action.BACK, template.headerAction)
        assertEquals(itemList.items, template.itemList!!.items)
        assertEquals(actionStrip, template.actionStrip)
    }

    @Test
    fun `every places state change refreshes the template`() {
        lifecycle.currentState = Lifecycle.State.RESUMED
        clearMocks(appManager, answers = false)

        placesState.value = PlacesListState.Loaded(placeItemList(withRowImages = true))
        placesState.value = PlacesListState.Failed

        verify(exactly = 2) { appManager.invalidate() }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `failed places show the error message below Car API 7`() {
        apiLevel = 6
        placesState.value = PlacesListState.Failed

        val template = sut.onGetTemplate() as PlaceListNavigationTemplate

        assertEquals("Error", template.itemList!!.noItemsMessage.toString())
    }

    @Suppress("DEPRECATION")
    @Test
    fun `loaded places are listed in the legacy place list below Car API 7`() {
        apiLevel = 6
        val itemList = placeItemList(withRowImages = false)
        placesState.value = PlacesListState.Loaded(itemList)

        val template = sut.onGetTemplate() as PlaceListNavigationTemplate

        assertFalse(template.isLoading)
        assertEquals(itemList.items, template.itemList!!.items)
        assertEquals(Action.BACK, template.header!!.startHeaderAction)
        assertEquals(Action.BACK, template.headerAction)
        assertEquals(actionStrip, template.actionStrip)
    }

    // The list template builds a header from its legacy fields, so its built form cannot tell
    // the setters apart. The screen goes through the helper, which picks setHeader on Car API 7.
    @Test
    fun `the list over the map sets its back action through the header helper`() {
        mockkStatic(CAR_TEMPLATE_HEADERS)
        try {
            placesState.value = PlacesListState.Loaded(placeItemList(withRowImages = true))

            sut.onGetTemplate()

            verify {
                any<ListTemplate.Builder>().setHeaderCompat(aCarContext, null, Action.BACK)
            }
        } finally {
            unmockkStatic(CAR_TEMPLATE_HEADERS)
        }
    }

    @Test
    fun `the manager is told whether its places are shown in a list template`() {
        verify { manager.showsInListTemplate = true }

        apiLevel = 6
        val legacyManager = mockk<PlacesListOnMapManager>(relaxed = true)
        PlacesListOnMapScreen(
            searchCarContext,
            mockk(relaxed = true),
            MapboxScreen.FAVORITES,
            legacyManager,
        )

        verify { legacyManager.showsInListTemplate = false }
    }

    // The rows of the production mapper, so the templates validate them as they do on a host.
    private fun placeItemList(withRowImages: Boolean): ItemList {
        val placeMarkerRenderer = mockk<PlaceMarkerRenderer> {
            every { renderMarker() } returns mockk {
                every { type } returns CarIcon.TYPE_CUSTOM
                every { icon } returns mockk {
                    every { type } returns IconCompat.TYPE_BITMAP
                }
            }
        }
        val location = Location.Builder().apply {
            latitude(37.80)
            longitude(-122.44)
        }.build()
        val place = PlaceRecord("id", "Home", Point.fromLngLat(-122.45, 37.81), "Street", listOf())
        return PlacesListItemMapper(placeMarkerRenderer, UnitType.METRIC)
            .mapToItemList(location, listOf(place), mockk(relaxed = true), withRowImages)
    }
}

private const val CAR_TEMPLATE_HEADERS =
    "com.mapbox.navigation.ui.androidauto.internal.CarTemplateHeadersKt"
