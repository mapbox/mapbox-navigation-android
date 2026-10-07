package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.dispatchers.SdkDispatchersTestRule
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.androidauto.internal.extensions.getStyle
import com.mapbox.navigation.ui.androidauto.internal.extensions.styleFlow
import com.mapbox.navigation.ui.androidauto.location.CarLocationProvider
import com.mapbox.navigation.ui.androidauto.search.GetPlacesError
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesListOnMapManagerTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val sdkDispatchersRule = SdkDispatchersTestRule()

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val placesProvider: PlacesListOnMapProvider = mockk(relaxed = true)
    private val locationProvider: CarLocationProvider = mockk()
    private val validLocation = CompletableDeferred<Location>()
    private var placeLimit = 100
    private val carMapSurface: MapboxCarMapSurface = mockk(relaxed = true) {
        every {
            carContext.getCarService(ConstraintManager::class.java)
        } returns mockk {
            every {
                getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)
            } answers { placeLimit }
        }
    }
    private val mapboxNavigation: MapboxNavigation = mockk {
        every { navigationOptions } returns mockk {
            every { distanceFormatterOptions } returns mockk {
                every { unitType } returns UnitType.METRIC
            }
        }
    }

    private val manager = PlacesListOnMapManager(placesProvider)

    private val place = place()

    private fun place(
        id: String = "id",
        coordinate: Point? = Point.fromLngLat(-122.4, 37.8),
    ) = PlaceRecord(
        id = id,
        name = "name",
        coordinate = coordinate,
    )

    private val location = Location.Builder().apply {
        latitude(37.80)
        longitude(-122.44)
    }.build()

    @Before
    fun setup() {
        mockkStatic(MapboxCarMapSurface::styleFlow, MapboxCarMapSurface::getStyle)
        every { carMapSurface.styleFlow() } returns emptyFlow()
        every { carMapSurface.getStyle() } returns null
        mockkObject(CarLocationProvider)
        every { CarLocationProvider.getRegisteredInstance() } returns locationProvider
        coEvery { locationProvider.validLocation() } coAnswers { validLocation.await() }
        mockkConstructor(PlaceMarkerRenderer::class)
        every { anyConstructed<PlaceMarkerRenderer>().renderMarker() } returns mockk {
            every { type } returns CarIcon.TYPE_CUSTOM
            every { icon } returns mockk {
                every { type } returns IconCompat.TYPE_BITMAP
            }
        }
        carAppTestRule.onAttached(mapboxNavigation)
    }

    @Test
    fun `places loaded before the first location fix stay loading until a location arrives`() {
        every { locationProvider.lastLocation() } returns null
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))

        manager.onAttached(carMapSurface)

        assertEquals(PlacesListState.Loading, manager.state.value)

        validLocation.complete(location)

        val state = manager.state.value
        assertTrue(state is PlacesListState.Loaded)
        assertEquals(1, (state as PlacesListState.Loaded).itemList.items.size)
        assertEquals(state.itemList, manager.itemList.value)
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `the list and the map get the same places within the host place limit`() {
        placeLimit = 2
        every { locationProvider.lastLocation() } returns location
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(
            listOf(
                place(id = "no coordinate", coordinate = null),
                place(id = "first"),
                place(id = "second"),
                place(id = "third"),
            ),
        )

        manager.onAttached(carMapSurface)

        assertEquals(listOf("first", "second"), manager.placeRecords.value.map { it.id })
        assertEquals(2, manager.itemList.value.items.size)
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `places are listed right away when a location is already known`() {
        every { locationProvider.lastLocation() } returns location
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))

        manager.onAttached(carMapSurface)

        assertTrue(manager.state.value is PlacesListState.Loaded)
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `a failure to load the places is reported`() {
        every { locationProvider.lastLocation() } returns location
        coEvery {
            placesProvider.getPlaces()
        } returns ExpectedFactory.createError(GetPlacesError("boom", null))

        manager.onAttached(carMapSurface)

        assertEquals(PlacesListState.Failed, manager.state.value)
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `a failure to reload the places keeps the places loaded earlier`() {
        every { locationProvider.lastLocation() } returns location
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))
        manager.onAttached(carMapSurface)
        manager.onDetached(carMapSurface)

        coEvery {
            placesProvider.getPlaces()
        } returns ExpectedFactory.createError(GetPlacesError("boom", null))
        manager.onAttached(carMapSurface)

        assertTrue(manager.state.value is PlacesListState.Loaded)
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `an empty places list is loaded without waiting for a location`() {
        every { locationProvider.lastLocation() } returns null
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(emptyList())

        manager.onAttached(carMapSurface)

        val state = manager.state.value
        assertTrue(state is PlacesListState.Loaded)
        assertTrue((state as PlacesListState.Loaded).itemList.items.isEmpty())
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `the list stops updating while navigation is detached`() {
        every { locationProvider.lastLocation() } returns null
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))
        manager.onAttached(carMapSurface)

        carAppTestRule.onDetached(mapboxNavigation)
        validLocation.complete(location)

        assertEquals(PlacesListState.Loading, manager.state.value)

        carAppTestRule.onAttached(mapboxNavigation)

        assertTrue(manager.state.value is PlacesListState.Loaded)
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `every tap on a place is emitted, including repeated taps on an equal place`() =
        runTest(sdkDispatchersRule.dispatcher) {
            val clicks = mutableListOf<PlaceRecord>()
            val job = launch { manager.placeClicks.collect { clicks.add(it) } }

            manager.placeClickListener.onItemClick(place)
            manager.placeClickListener.onItemClick(place())

            assertEquals(listOf(place, place), clicks)
            job.cancel()
        }

    @Test
    fun `a tap is not replayed to a collector that subscribes later`() =
        runTest(sdkDispatchersRule.dispatcher) {
            val firstCollector = launch { manager.placeClicks.collect { } }
            manager.placeClickListener.onItemClick(place)
            firstCollector.cancel()

            val clicks = mutableListOf<PlaceRecord>()
            val job = launch { manager.placeClicks.collect { clicks.add(it) } }

            assertEquals(emptyList<PlaceRecord>(), clicks)
            assertEquals(place, manager.placeSelected.value)
            job.cancel()
        }
}
