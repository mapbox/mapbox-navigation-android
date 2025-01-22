package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedRenderedFeature
import com.mapbox.maps.Style
import com.mapbox.navigation.base.extensions.coordinates
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.building.api.MapboxBuildingsApi
import com.mapbox.navigation.ui.maps.building.model.BuildingError
import com.mapbox.navigation.ui.maps.building.model.BuildingValue
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions
import com.mapbox.navigation.ui.maps.building.view.MapboxBuildingView
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuildingHighlightComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var buildingsApi: MapboxBuildingsApi
    private lateinit var buildingView: MapboxBuildingView
    private lateinit var options: MapboxBuildingHighlightOptions
    private lateinit var map: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var sut: BuildingHighlightComponent
    private lateinit var mapStyle: Style

    @Before
    fun setUp() {
        buildingsApi = mockk(relaxed = true)
        buildingView = mockk(relaxed = true)
        options = MapboxBuildingHighlightOptions.Builder().build()
        mapStyle = mockk()
        map = mockk {
            every { getStyle() } returns mapStyle
        }
        mapboxNavigation = mockk(relaxed = true)

        sut = BuildingHighlightComponent(map, options, buildingsApi, buildingView)
    }

    @Test
    fun `onAttached - should highlight building onFinalDestinationArrival`() = runBlockingTest {
        val destination = Point.fromLngLat(1.0, 1.0)
        val routeProgress = routeProgressWith(
            routeOptions(
                origin = Point.fromLngLat(0.0, 0.0),
                destination = destination,
            ),
        )
        val buildings = listOf<QueriedRenderedFeature>(mockk())
        givenArrivalObserverAnswer {
            onFinalDestinationArrival(routeProgress)
        }
        givenBuildingsApiAnswer(givenDestination = destination) {
            accept(ExpectedFactory.createValue(BuildingValue(buildings)))
        }

        sut.onAttached(mapboxNavigation)

        verify { buildingView.highlightBuilding(mapStyle, buildings, options) }
    }

    @Test
    fun `onAttached - should remove building highlight onNextRouteLegStart`() = runBlockingTest {
        val routeLegProgress = mockk<RouteLegProgress>()
        givenArrivalObserverAnswer {
            onNextRouteLegStart(routeLegProgress)
        }

        sut.onAttached(mapboxNavigation)

        verify { buildingView.removeBuildingHighlight(mapStyle, options) }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `onAttached - should remove building highlight when NavigationRoutes are empty`() =
        runBlockingTest {
            givenRoutesObserverAnswer {
                onRoutesChanged(
                    mockk {
                        every { navigationRoutes } returns emptyList()
                    },
                )
            }

            sut.onAttached(mapboxNavigation)

            verify { buildingView.removeBuildingHighlight(mapStyle, options) }
        }

    @Test
    fun `onDetached - should remove building highlight`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)

        sut.onDetached(mapboxNavigation)

        verify { buildingView.removeBuildingHighlight(mapStyle, options) }
    }

    @Test
    fun `onDetached - should cancel any MapboxBuildingsApi operations`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)

        sut.onDetached(mapboxNavigation)

        verify { buildingsApi.cancel() }
    }

    @Test
    fun `onDetached - should clear any MapboxBuildingsView`() = runBlockingTest {
        sut.onAttached(mapboxNavigation)

        sut.onDetached(mapboxNavigation)

        verify { buildingView.clear(mapStyle) }
    }

    private fun givenRoutesObserverAnswer(block: RoutesObserver.() -> Unit) {
        val routesObserver = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } answers {
            routesObserver.captured.apply(block)
        }
    }

    private fun givenArrivalObserverAnswer(block: ArrivalObserver.() -> Unit) {
        val arrivalObserver = slot<ArrivalObserver>()
        every { mapboxNavigation.registerArrivalObserver(capture(arrivalObserver)) } answers {
            arrivalObserver.captured.apply(block)
        }
    }

    private fun givenBuildingsApiAnswer(
        givenDestination: Point,
        block: MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>.() -> Unit,
    ) {
        val callback = slot<MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>>()
        every {
            buildingsApi.queryBuildingToHighlight(
                givenDestination,
                capture(callback),
            )
        } answers {
            callback.captured.apply(block)
        }
    }

    private fun routeProgressWith(options: RouteOptions) = mockk<RouteProgress> {
        every { navigationRoute } returns mockk(relaxed = true) {
            every { routeOptions } returns options
        }
    }

    private fun routeOptions(origin: Point, destination: Point): RouteOptions =
        RouteOptions.builder()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .coordinates(
                origin = origin,
                destination = destination,
            )
            .build()
}
