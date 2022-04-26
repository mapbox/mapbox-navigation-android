import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.android.gestures.Utils
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.component.routeline.RouteLineComponent
import com.mapbox.navigation.dropin.util.TestingUtil
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
import io.mockk.coVerify
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
@OptIn(ExperimentalCoroutinesApi::class)
class RouteLineComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val context = mockk<Context>() {
        every { resources } returns mockk()
    }
    private val mockGestures = mockk<GesturesPlugin>(relaxed = true)
    private val locationComponentPlugin = mockk<LocationComponentPlugin>(relaxed = true)
    private val mockStyle = mockk<Style>(relaxed = true)
    private val mockMap = mockk<MapboxMap>(relaxed = true) {
        every { getStyle() } returns mockStyle
    }
    private val mockMapView = mockk<MapView>(relaxed = true) {
        every { getMapboxMap() } returns mockMap
        every { gestures } returns mockGestures
        every { location } returns locationComponentPlugin
    }
    private val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val options by lazy { MapboxRouteLineOptions.Builder(context).build() }
    private val routesViewModel = mockk<RoutesViewModel>(relaxed = true)

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
        RouteLineComponent(mockMapView, options, routesViewModel, mockApi)
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
        RouteLineComponent(mockMapView, options, routesViewModel, mockApi, mockView)
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
        RouteLineComponent(mockMapView, options, routesViewModel, mockApi, mockView)
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
        RouteLineComponent(mockMapView, options, routesViewModel, mockApi, mockView)
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
        val route1 = TestingUtil.loadRoute("short_route.json").toNavigationRoute()
        val route2 = TestingUtil.loadRoute("multileg_route.json").toNavigationRoute()
        val resultRoutesSlot = slot<RoutesAction.SetRoutes>()
        val mockResponse = mockk<ClosestRouteValue> {
            every { navigationRoute } returns route2
        }
        val consumerSlot =
            slot<MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>>()
        val clickSlot = slot<OnMapClickListener>()
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { findClosestRoute(any(), any(), any(), capture(consumerSlot)) } returns Unit
            every { getNavigationRoutes() } returns listOf(route1, route2)
        }
        val point = Point.fromLngLat(-119.27, 84.85)
        RouteLineComponent(mockMapView, options, routesViewModel, mockApi)
            .onAttached(mockMapboxNavigation)
        verify { mockGestures.addOnMapClickListener(capture(clickSlot)) }

        clickSlot.captured.onMapClick(point)

        consumerSlot.captured.accept(ExpectedFactory.createValue(mockResponse))
        verify { mockApi.findClosestRoute(point, mockMap, any(), any()) }
        coVerify { routesViewModel.invoke(capture(resultRoutesSlot)) }
        assertEquals(route2, resultRoutesSlot.captured.routes.first())
        assertEquals(route1, resultRoutesSlot.captured.routes[1])
    }

    @Test
    fun `onDetached cancels route line API`() {
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true)
        val component = RouteLineComponent(mockMapView, options, routesViewModel, mockApi)
        component.onAttached(mockMapboxNavigation)

        component.onDetached(mockMapboxNavigation)

        verify { mockApi.cancel() }
    }

    @Test
    fun `onDetached cancels route line view`() {
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true)
        val mockView = mockk<MapboxRouteLineView>(relaxed = true)
        val component = RouteLineComponent(mockMapView, options, routesViewModel, mockApi, mockView)
        component.onAttached(mockMapboxNavigation)

        component.onDetached(mockMapboxNavigation)

        verify { mockView.cancel() }
    }
}
