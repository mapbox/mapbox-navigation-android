package com.mapbox.navigation.dropin.component.routeline

import com.mapbox.android.gestures.Utils
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteLineViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun routesUpdated() {
        val route = getRoute()
        val mockValue = mockk<RouteSetValue>()
        val expectedResult: Expected<RouteLineError, RouteSetValue> =
            ExpectedFactory.createValue(mockValue)
        val callBackSlot = slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>>()
        val routesSlot = slot<List<RouteLine>>()
        val routeLineApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { setRoutes(capture(routesSlot), capture(callBackSlot)) } answers {
                callBackSlot.captured.accept(expectedResult)
            }
        }
        val routeLineView = mockk<MapboxRouteLineView>(relaxed = true)
        val style = mockk<Style>()
        val routeUpdate = mockk<RoutesUpdatedResult> {
            every { routes } returns listOf(route)
        }

        RouteLineViewModel(routeLineApi, routeLineView).routesUpdated(routeUpdate, style)

        verify { routeLineApi.setRoutes(any(), any()) }
        verify { routeLineView.renderRouteDrawData(style, expectedResult) }
    }

    @Test
    fun routesUpdated_errorEmitted() = coroutineRule.runBlockingTest {
        val route = getRoute()
        val style = mockk<Style>()
        val mockValue = mockk<RouteLineError>()
        val expectedResult: Expected<RouteLineError, RouteSetValue> =
            ExpectedFactory.createError(mockValue)
        val callBackSlot = slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>>()
        val routesSlot = slot<List<RouteLine>>()
        val routeLineApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { setRoutes(capture(routesSlot), capture(callBackSlot)) } answers {
                callBackSlot.captured.accept(expectedResult)
            }
        }
        val routeLineView = mockk<MapboxRouteLineView>(relaxed = true)
        val routeUpdate = mockk<RoutesUpdatedResult> {
            every { routes } returns listOf(route)
        }
        val viewModel = RouteLineViewModel(routeLineApi, routeLineView)
        val def = async {
            viewModel.routeLineErrors.first()
        }

        viewModel.routesUpdated(routeUpdate, style)
        val viewModelResult = def.await()

        verify { routeLineApi.setRoutes(any(), any()) }
        assertEquals(expectedResult.error, viewModelResult)
        verify { routeLineView.renderRouteDrawData(style, expectedResult) }
    }

    @Test
    fun routeProgressUpdated() {
        val mockValue = mockk<RouteLineUpdateValue>()
        val style = mockk<Style>()
        val expectedResult: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createValue(mockValue)
        val routeProgress = mockk<RouteProgress>()
        val callBackSlot =
            slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>>()
        val routeLineApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { updateWithRouteProgress(routeProgress, capture(callBackSlot)) } answers {
                callBackSlot.captured.accept(expectedResult)
            }
        }
        val routeLineView = mockk<MapboxRouteLineView>(relaxed = true)

        RouteLineViewModel(routeLineApi, routeLineView).routeProgressUpdated(routeProgress, style)

        verify { routeLineApi.updateWithRouteProgress(routeProgress, any()) }
        verify { routeLineView.renderRouteLineUpdate(style, expectedResult) }
    }

    @Test
    fun routeProgressUpdated_errorEmitted() = coroutineRule.runBlockingTest {
        val mockValue = mockk<RouteLineError>()
        val style = mockk<Style>()
        val expectedResult: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createError(mockValue)
        val routeProgress = mockk<RouteProgress>()
        val callBackSlot =
            slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>>()
        val routeLineApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { updateWithRouteProgress(routeProgress, capture(callBackSlot)) } answers {
                callBackSlot.captured.accept(expectedResult)
            }
        }
        val routeLineView = mockk<MapboxRouteLineView>(relaxed = true)
        val viewModel = RouteLineViewModel(routeLineApi, routeLineView)
        val def = async {
            viewModel.routeLineErrors.first()
        }

        viewModel.routeProgressUpdated(routeProgress, style)
        val viewModelResult = def.await()

        verify { routeLineApi.updateWithRouteProgress(routeProgress, any()) }
        assertEquals(expectedResult.error, viewModelResult)
        verify { routeLineView.renderRouteLineUpdate(style, expectedResult) }
    }

    @Test
    fun positionChanged() = coroutineRule.runBlockingTest {
        val style = mockk<Style>()
        val point = Point.fromLngLat(-44.0, -33.0)
        val mockValue = mockk<RouteLineUpdateValue>()
        val expectedResult: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createValue(mockValue)
        val routeLineApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { updateTraveledRouteLine(point) } returns expectedResult
        }
        val routeLineView = mockk<MapboxRouteLineView>(relaxed = true)

        RouteLineViewModel(routeLineApi, routeLineView).positionChanged(point, style)

        verify { routeLineApi.updateTraveledRouteLine(point) }
        verify { routeLineView.renderRouteLineUpdate(style, expectedResult) }
    }

    @Test
    fun positionChanged_errorEmitted() = coroutineRule.runBlockingTest {
        val style = mockk<Style>()
        val point = Point.fromLngLat(-44.0, -33.0)
        val mockValue = mockk<RouteLineError>()
        val expectedResult: Expected<RouteLineError, RouteLineUpdateValue> =
            ExpectedFactory.createError(mockValue)
        val routeLineApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { updateTraveledRouteLine(point) } returns expectedResult
        }
        val routeLineView = mockk<MapboxRouteLineView>(relaxed = true)
        val viewModel = RouteLineViewModel(routeLineApi, routeLineView)
        val def = async {
            viewModel.routeLineErrors.first()
        }

        viewModel.positionChanged(point, style)
        val viewModelResult = def.await()

        verify { routeLineApi.updateTraveledRouteLine(point) }
        assertEquals(expectedResult.error, viewModelResult)
        verify { routeLineView.renderRouteLineUpdate(style, expectedResult) }
    }

    @Test
    fun mapStyleUpdated() {
        val style = mockk<Style>()
        val mockValue = mockk<RouteSetValue>()
        val expectedResult: Expected<RouteLineError, RouteSetValue> =
            ExpectedFactory.createValue(mockValue)
        val callBackSlot =
            slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>>()
        val routeLineApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { getRouteDrawData(capture(callBackSlot)) } answers {
                callBackSlot.captured.accept(expectedResult)
            }
        }
        val routeLineView = mockk<MapboxRouteLineView>(relaxed = true)
        val viewModel = RouteLineViewModel(routeLineApi, routeLineView)

        viewModel.mapStyleUpdated(style)

        verify { routeLineView.initializeLayers(style) }
        verify { routeLineView.renderRouteDrawData(style, expectedResult) }
    }

    @Test
    fun mapStyleUpdated_error() = coroutineRule.runBlockingTest {
        val style = mockk<Style>()
        val mockValue = mockk<RouteLineError>()
        val expectedResult: Expected<RouteLineError, RouteSetValue> =
            ExpectedFactory.createError(mockValue)
        val callBackSlot =
            slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>>()
        val routeLineApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { getRouteDrawData(capture(callBackSlot)) } answers {
                callBackSlot.captured.accept(expectedResult)
            }
        }
        val routeLineView = mockk<MapboxRouteLineView>(relaxed = true)
        val viewModel = RouteLineViewModel(routeLineApi, routeLineView)
        val def = async {
            viewModel.routeLineErrors.first()
        }

        viewModel.mapStyleUpdated(style)
        val viewModelResult = def.await()

        verify { routeLineView.initializeLayers(style) }
        verify { routeLineView.renderRouteDrawData(style, expectedResult) }
        assertEquals(expectedResult.error, viewModelResult)
    }

    @Test
    fun routeResets() = coroutineRule.runBlockingTest {
        mockkStatic(Utils::class)
        every { Utils.dpToPx(30f) } returns 30f
        val style = mockk<Style>()
        val map = mockk<MapboxMap> {
            every { getStyle() } returns style
        }
        val initialPrimaryRoute = getRoute()
        val initialAltRoute = loadRoute("multileg_route.json")
        val initialRoutes = listOf(initialPrimaryRoute, initialAltRoute)
        val closestRouteValue = mockk<ClosestRouteValue> {
            every { route } returns initialAltRoute
        }
        val callBackSlot =
            slot<MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>>()
        val routeLineApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { findClosestRoute(any(), any(), any(), capture(callBackSlot)) } answers {
                callBackSlot.captured.accept(ExpectedFactory.createValue(closestRouteValue))
            }
            every { getRoutes() } returns initialRoutes
        }
        val routeLineView = mockk<MapboxRouteLineView> {
            every { getPrimaryRouteVisibility(style) } returns Visibility.VISIBLE
            every { getAlternativeRoutesVisibility(style) } returns Visibility.VISIBLE
        }
        val viewModel = RouteLineViewModel(routeLineApi, routeLineView)
        val def = async {
            viewModel.routeResets.first()
        }

        viewModel.mapClick(Point.fromLngLat(-122.444359, 37.736351), map)
        val viewModelResult = def.await()

        assertEquals(2, viewModelResult.size)
        assertEquals(initialAltRoute, viewModelResult.first())
        assertEquals(initialPrimaryRoute, viewModelResult[1])
        unmockkStatic(Utils::class)
    }

    private fun getRoute(): DirectionsRoute {
        return loadRoute("short_route.json")
    }

    private fun loadRoute(routeFileName: String): DirectionsRoute {
        val routeAsJson = FileUtils.loadJsonFixture(routeFileName)
        return DirectionsRoute.fromJson(routeAsJson)
    }
}
