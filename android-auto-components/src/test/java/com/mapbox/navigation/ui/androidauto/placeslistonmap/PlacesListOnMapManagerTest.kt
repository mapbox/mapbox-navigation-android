package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.test.core.app.ApplicationProvider
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
    private val locationWithinTimeout = CompletableDeferred<Location?>()
    private var placeLimit = 100
    private var listLimit = 100
    private val carMapSurface: MapboxCarMapSurface = mockk(relaxed = true) {
        // The limit follows the template that shows the list, never the host API level.
        every { carContext.carAppApiLevel } returns 7
        // The distance units without a MapboxNavigation come from the device locale.
        every { carContext.applicationContext } returns ApplicationProvider.getApplicationContext()
        every { carContext.resources } returns
            ApplicationProvider.getApplicationContext<android.content.Context>().resources
        every {
            carContext.getCarService(ConstraintManager::class.java)
        } returns mockk {
            every {
                getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)
            } answers { placeLimit }
            every {
                getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
            } answers { listLimit }
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
        name: String = "name",
        description: String? = null,
        coordinate: Point? = Point.fromLngLat(-122.4, 37.8),
    ) = PlaceRecord(
        id = id,
        name = name,
        coordinate = coordinate,
        description = description,
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
        coEvery {
            locationProvider.waitForLocationOrNull(any())
        } coAnswers { locationWithinTimeout.await() }
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
    fun `places loaded before the first location fix wait a short time for a location`() {
        every { locationProvider.lastLocation() } returns null
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))

        manager.onAttached(carMapSurface)

        assertEquals(PlacesListState.Loading, manager.state.value)

        locationWithinTimeout.complete(location)

        val state = manager.state.value
        assertTrue(state is PlacesListState.Loaded)
        assertEquals(1, (state as PlacesListState.Loaded).itemList.items.size)
        assertEquals(state.itemList, manager.itemList.value)
        assertFalse((state.itemList.items.single() as Row).texts.isEmpty())
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `places are listed without distances when no location arrives in time`() {
        manager.showsInListTemplate = true
        every { locationProvider.lastLocation() } returns null
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))

        manager.onAttached(carMapSurface)
        locationWithinTimeout.complete(null)

        val state = manager.state.value
        assertTrue(state is PlacesListState.Loaded)
        assertTrue(((state as PlacesListState.Loaded).itemList.items.single() as Row).texts.isEmpty())

        validLocation.complete(location)

        val withDistance = manager.state.value as PlacesListState.Loaded
        assertFalse((withDistance.itemList.items.single() as Row).texts.isEmpty())
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `a place list template stays valid until a location arrives`() {
        every { locationProvider.lastLocation() } returns null
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))

        manager.onAttached(carMapSurface)
        locationWithinTimeout.complete(null)

        // A place list template rejects rows without a distance.
        val withoutLocation = manager.state.value as PlacesListState.Loaded
        assertTrue(withoutLocation.itemList.items.isEmpty())

        validLocation.complete(location)

        val withLocation = manager.state.value as PlacesListState.Loaded
        assertEquals(1, withLocation.itemList.items.size)
        @Suppress("DEPRECATION")
        PlaceListNavigationTemplate.Builder().setItemList(withLocation.itemList).build()
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `places are listed while no navigation is attached`() {
        carAppTestRule.onDetached(mapboxNavigation)
        every { locationProvider.lastLocation() } returns location
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))

        manager.onAttached(carMapSurface)

        assertTrue(manager.state.value is PlacesListState.Loaded)
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `a provider that throws is reported as a failure`() {
        every { locationProvider.lastLocation() } returns location
        listOf(
            IllegalStateException("broken provider"),
            NotImplementedError("provider not implemented"),
            CancellationException("provider timed out"),
        ).forEach { error ->
            coEvery { placesProvider.getPlaces() } throws error
            val manager = PlacesListOnMapManager(placesProvider)

            manager.onAttached(carMapSurface)

            assertEquals(error.toString(), PlacesListState.Failed, manager.state.value)
            manager.onDetached(carMapSurface)
        }
    }

    @Test
    fun `a place without a name is titled by its description, or skipped`() {
        every { locationProvider.lastLocation() } returns location
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(
            listOf(
                place(id = "described", name = " ", description = "1 Main St"),
                place(id = "unnamed", name = ""),
            ),
        )

        manager.onAttached(carMapSurface)

        val rows = (manager.state.value as PlacesListState.Loaded).itemList.items
        assertEquals(listOf("1 Main St"), rows.map { (it as Row).title.toString() })
        // A place without a row gets no marker either.
        assertEquals(listOf("described"), manager.placeRecords.value.map { it.id })
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `the list and the map get the same places within the host place limit by default`() {
        placeLimit = 2
        listLimit = 1
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
    fun `places shown in a list template are limited by the host list limit`() {
        manager.showsInListTemplate = true
        placeLimit = 1
        listLimit = 2
        every { locationProvider.lastLocation() } returns location
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(
            listOf(place(id = "first"), place(id = "second"), place(id = "third")),
        )

        manager.onAttached(carMapSurface)

        assertEquals(listOf("first", "second"), manager.placeRecords.value.map { it.id })
        assertEquals(2, manager.itemList.value.items.size)
        manager.onDetached(carMapSurface)
    }

    @Test
    fun `rows show the marker icon as their image only in a list template`() {
        every { locationProvider.lastLocation() } returns location
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(
            listOf(place(id = "first")),
        )

        manager.onAttached(carMapSurface)
        assertNull((manager.itemList.value.items.single() as Row).image)
        manager.onDetached(carMapSurface)

        manager.showsInListTemplate = true
        manager.onAttached(carMapSurface)
        assertNotNull((manager.itemList.value.items.single() as Row).image)
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
    fun `a list on screen is kept when navigation detaches, and rebuilt when it attaches`() {
        every { locationProvider.lastLocation() } returns location
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))
        manager.onAttached(carMapSurface)
        val shownList = manager.itemList.value

        carAppTestRule.onDetached(mapboxNavigation)

        assertSame(shownList, manager.itemList.value)

        // The navigation attached next measures in miles, so a rebuilt list shows miles.
        every {
            mapboxNavigation.navigationOptions.distanceFormatterOptions.unitType
        } returns UnitType.IMPERIAL
        carAppTestRule.onAttached(mapboxNavigation)

        assertEquals(Distance.UNIT_MILES, firstRowDistanceUnit(manager.itemList.value))
        manager.onDetached(carMapSurface)
    }

    private fun firstRowDistanceUnit(itemList: ItemList): Int {
        val text = (itemList.items.first() as Row).texts.first()
        val span = text.spans.single { it.carSpan is DistanceSpan }.carSpan
        return (span as DistanceSpan).distance!!.displayUnit
    }

    @Test
    fun `a list still loading when navigation detaches is built without it`() {
        every { locationProvider.lastLocation() } returns null
        coEvery { placesProvider.getPlaces() } returns ExpectedFactory.createValue(listOf(place))
        manager.showsInListTemplate = true
        manager.onAttached(carMapSurface)

        carAppTestRule.onDetached(mapboxNavigation)
        locationWithinTimeout.complete(location)

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
