package com.mapbox.navigation.ui.maps.internal.ui

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.android.gestures.Utils
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.delegates.MapPluginProviderDelegate
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigator.RouteInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class RouteLineComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val context = mockk<Context> {
        every { resources } returns mockk()
    }
    private val mockGestures = mockk<GesturesPlugin>(relaxed = true)
    private val locationComponentPlugin = mockk<LocationComponentPlugin>(relaxed = true)
    private val mockStyle = mockk<Style>(relaxed = true)
    private val mockMap = mockk<MapboxMap>(relaxed = true) {
        every { getStyle() } returns mockStyle
    }
    private val mapPluginProvider = mockk<MapPluginProviderDelegate>(relaxed = true) {
        every { gestures } returns mockGestures
        every { location } returns locationComponentPlugin
    }
    private val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val options by lazy { MapboxRouteLineOptions.Builder(context).build() }

    @Before
    fun setUp() {
        mockkStatic(Utils::class)
        every { Utils.dpToPx(any()) } returns 50f
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true) {
            every { intrinsicHeight } returns 24
            every { intrinsicWidth } returns 24
        }
        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
        } answers {
            val routesCount =
                JSONObject(this.firstArg<String>())
                    .getJSONArray("routes")
                    .length()
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeId } returns "$it"
                            every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(AppCompatResources::class)
        unmockkStatic(Utils::class)
        unmockkObject(NativeRouteParserWrapper)
    }

    @Test
    fun `onAttached subscribes to map clicks`() {
        val mockResponse = mockk<RouteNotFound>()
        val mapClickSlot = slot<OnMapClickListener>()
        val consumerSlot =
            slot<MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>>()
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { findClosestRoute(any(), any(), any(), capture(consumerSlot)) } returns Unit
        }
        val point = Point.fromLngLat(-119.27, 84.85)
        RouteLineComponent(mockMap, mapPluginProvider, options, mockApi)
            .onAttached(mockMapboxNavigation)

        verify { mockGestures.addOnMapClickListener(capture(mapClickSlot)) }
        mapClickSlot.captured.onMapClick(point)
        consumerSlot.captured.accept(ExpectedFactory.createError(mockResponse))

        verify { mockApi.findClosestRoute(point, mockMap, any(), any()) }
    }

    @Test
    fun `onAttached subscribes to position changes`() {
        val positionChangeSlot = slot<OnIndicatorPositionChangedListener>()
        val point = Point.fromLngLat(-119.27, 84.85)
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { updateTraveledRouteLine(point) } returns ExpectedFactory.createError(mockk())
        }
        val mockView = mockk<MapboxRouteLineView>(relaxed = true)
        RouteLineComponent(mockMap, mapPluginProvider, options, mockApi, mockView)
            .onAttached(mockMapboxNavigation)
        verify {
            locationComponentPlugin.addOnIndicatorPositionChangedListener(
                capture(positionChangeSlot)
            )
        }

        positionChangeSlot.captured.onIndicatorPositionChanged(point)

        verify { mockApi.updateTraveledRouteLine(point) }
    }

    @Test
    fun routeProgressObserverTest() {
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true)
        val mockView = mockk<MapboxRouteLineView>(relaxed = true)
        val mockError = mockk<RouteLineError> {
            every { errorMessage } returns "error"
            every { throwable } returns null
        }
        val routeProgress = mockk<RouteProgress>()
        val callbackSlot =
            slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>>()
        val callbackResult =
            ExpectedFactory.createError<RouteLineError, RouteLineUpdateValue>(mockError)
        val routeProgressObserverSlot = slot<RouteProgressObserver>()
        RouteLineComponent(mockMap, mapPluginProvider, options, mockApi, mockView)
            .onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRouteProgressObserver(capture(routeProgressObserverSlot))
        }

        routeProgressObserverSlot.captured.onRouteProgressChanged(routeProgress)

        verify { mockApi.updateWithRouteProgress(routeProgress, capture(callbackSlot)) }
        callbackSlot.captured.accept(callbackResult)
        verify { mockView.renderRouteLineUpdate(mockStyle, callbackResult) }
    }

    @Test
    fun routesObserverTest() {
        val mockRoutes = listOf<NavigationRoute>(mockk())
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true)
        val mockView = mockk<MapboxRouteLineView>(relaxed = true)
        val mockError = mockk<RouteLineError> {
            every { errorMessage } returns "error"
            every { throwable } returns null
        }
        val expectedMockError =
            ExpectedFactory.createError<RouteLineError, RouteSetValue>(mockError)
        val routesUpdateResult = mockk<RoutesUpdatedResult> {
            every { navigationRoutes } returns mockRoutes
        }
        val callbackSlot = slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>>()
        val routesObserverSlot = slot<RoutesObserver>()
        RouteLineComponent(mockMap, mapPluginProvider, options, mockApi, mockView)
            .onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        }

        routesObserverSlot.captured.onRoutesChanged(routesUpdateResult)

        verify { mockApi.setNavigationRouteLines(any(), any(), capture(callbackSlot)) }
        callbackSlot.captured.accept(expectedMockError)
        verify { mockView.renderRouteDrawData(mockStyle, expectedMockError) }
    }

    @Test
    fun selectRouteTest() {
        val consumerSlot =
            slot<MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>>()
        val clickSlot = slot<OnMapClickListener>()
        val oldRoute = mockk<NavigationRoute> { every { directionsRoute } returns mockk() }
        val newRoute = mockk<NavigationRoute> { every { directionsRoute } returns mockk() }
        val clickPoint = Point.fromLngLat(0.0, 0.0)
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { getPrimaryNavigationRoute() } returns oldRoute
            every { getNavigationRoutes() } returns listOf(
                oldRoute,
                newRoute
            )
            every { findClosestRoute(clickPoint, any(), any(), capture(consumerSlot)) } answers {
                consumerSlot.captured.accept(
                    ExpectedFactory.createValue(
                        ClosestRouteValue(
                            newRoute
                        )
                    )
                )
            }
        }

        RouteLineComponent(mockMap, mapPluginProvider, options, mockApi)
            .onAttached(mockMapboxNavigation)
        verify { mockGestures.addOnMapClickListener(capture(clickSlot)) }

        clickSlot.captured.onMapClick(clickPoint)
        verify { mockMapboxNavigation.setNavigationRoutes(listOf(newRoute, oldRoute)) }
    }

    @Test
    fun `selectRoute should call custom contract`() {
        val consumerSlot =
            slot<MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>>()
        val clickSlot = slot<OnMapClickListener>()
        val oldRoute = mockk<NavigationRoute> { every { directionsRoute } returns mockk() }
        val newRoute = mockk<NavigationRoute> { every { directionsRoute } returns mockk() }
        val clickPoint = Point.fromLngLat(0.0, 0.0)
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { getPrimaryNavigationRoute() } returns oldRoute
            every { getNavigationRoutes() } returns listOf(
                oldRoute,
                newRoute
            )
            every { findClosestRoute(clickPoint, any(), any(), capture(consumerSlot)) } answers {
                consumerSlot.captured.accept(
                    ExpectedFactory.createValue(
                        ClosestRouteValue(
                            newRoute
                        )
                    )
                )
            }
        }

        val customContract = mockk<RouteLineComponentContract>()
        val sut = RouteLineComponent(mockMap, mapPluginProvider, options, mockApi) {
            customContract
        }
        sut.onAttached(mockMapboxNavigation)
        verify { mockGestures.addOnMapClickListener(capture(clickSlot)) }

        clickSlot.captured.onMapClick(clickPoint)
        verify { customContract.setRoutes(mockMapboxNavigation, listOf(newRoute, oldRoute)) }
    }

    @Test
    fun `onDetached cancels route line API`() {
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true)
        val component = RouteLineComponent(mockMap, mapPluginProvider, options, mockApi)
        component.onAttached(mockMapboxNavigation)

        component.onDetached(mockMapboxNavigation)

        verify { mockApi.cancel() }
    }

    @Test
    fun `onDetached cancels route line view`() {
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true)
        val mockView = mockk<MapboxRouteLineView>(relaxed = true)
        val component = RouteLineComponent(mockMap, mapPluginProvider, options, mockApi, mockView)
        component.onAttached(mockMapboxNavigation)

        component.onDetached(mockMapboxNavigation)

        verify { mockView.cancel() }
    }
}
