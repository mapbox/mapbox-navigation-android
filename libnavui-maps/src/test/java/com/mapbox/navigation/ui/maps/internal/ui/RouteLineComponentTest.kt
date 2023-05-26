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
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteLineComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val nativeRouteParserRule = NativeRouteParserRule()

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
    }

    @After
    fun tearDown() {
        unmockkStatic(AppCompatResources::class)
        unmockkStatic(Utils::class)
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
        val routeProgress = mockk<RouteProgress>(relaxed = true)
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
        every { mockMapboxNavigation.currentLegIndex() } returns 3
        val callbackSlot = slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>>()
        val routesObserverSlot = slot<RoutesObserver>()
        RouteLineComponent(mockMap, mapPluginProvider, options, mockApi, mockView)
            .onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        }

        routesObserverSlot.captured.onRoutesChanged(routesUpdateResult)

        verifyOrder {
            mockApi.setNavigationRoutes(
                any(),
                3,
                any<List<AlternativeRouteMetadata>>(),
                capture(callbackSlot)
            )
        }
        callbackSlot.captured.accept(expectedMockError)
        verify { mockView.renderRouteDrawData(mockStyle, expectedMockError) }
    }

    @Test
    fun `routes observer when routes from mapbox navigation is empty and preview is null`() {
        val mockContract = mockk<RouteLineComponentContract>(relaxed = true) {
            coEvery { getRouteInPreview() } returns flowOf(null)
        }
        val mockRoutes = listOf<NavigationRoute>()
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
        every { mockMapboxNavigation.currentLegIndex() } returns 3
        val callbackSlot = slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>>()
        val routesObserverSlot = slot<RoutesObserver>()
        RouteLineComponent(mockMap, mapPluginProvider, options, mockApi, mockView) { mockContract }
            .onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        }

        routesObserverSlot.captured.onRoutesChanged(routesUpdateResult)

        verify(exactly = 1) {
            mockApi.setNavigationRoutes(
                any(),
                0,
                any<List<AlternativeRouteMetadata>>(),
                capture(callbackSlot)
            )
        }
        callbackSlot.captured.accept(expectedMockError)
        verify { mockView.renderRouteDrawData(mockStyle, expectedMockError) }
    }

    @Test
    fun `routes observer when routes from mapbox navigation is not empty and preview is null`() {
        val mockContract = mockk<RouteLineComponentContract>(relaxed = true) {
            coEvery { getRouteInPreview() } returns flowOf(null)
        }
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
        every { mockMapboxNavigation.currentLegIndex() } returns 3
        val callbackSlot = slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>>()
        val routesObserverSlot = slot<RoutesObserver>()
        RouteLineComponent(mockMap, mapPluginProvider, options, mockApi, mockView) { mockContract }
            .onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        }

        routesObserverSlot.captured.onRoutesChanged(routesUpdateResult)

        verifyOrder {
            mockApi.setNavigationRoutes(
                any(),
                3,
                any<List<AlternativeRouteMetadata>>(),
                capture(callbackSlot)
            )
        }
        callbackSlot.captured.accept(expectedMockError)
        verify { mockView.renderRouteDrawData(mockStyle, expectedMockError) }
    }

    @Test
    fun `routes observer when routes from mapbox navigation is empty and preview is not null`() {
        val previewRoutes = listOf<NavigationRoute>(mockk())
        val mockContract = mockk<RouteLineComponentContract>(relaxed = true) {
            coEvery { getRouteInPreview() } returns flowOf(previewRoutes)
        }
        val mockRoutes = listOf<NavigationRoute>()
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
        every { mockMapboxNavigation.currentLegIndex() } returns 3
        val callbackSlot = slot<MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>>()
        val routesObserverSlot = slot<RoutesObserver>()
        RouteLineComponent(mockMap, mapPluginProvider, options, mockApi, mockView) { mockContract }
            .onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        }

        routesObserverSlot.captured.onRoutesChanged(routesUpdateResult)

        verify {
            mockApi.setNavigationRoutes(
                any(),
                0,
                any<List<AlternativeRouteMetadata>>(),
                capture(callbackSlot)
            )
        }
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

        val customContract = mockk<RouteLineComponentContract>(relaxed = true)
        val sut = RouteLineComponent(mockMap, mapPluginProvider, options, mockApi) {
            customContract
        }
        sut.onAttached(mockMapboxNavigation)
        verify { mockGestures.addOnMapClickListener(capture(clickSlot)) }

        clickSlot.captured.onMapClick(clickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(newRoute, oldRoute), null)
        }
        verify(exactly = 0) { customContract.onMapClicked(any()) }
    }

    @Test
    fun `selectRoute use correct leg index`() {
        val clickSlot = slot<OnMapClickListener>()
        val oldRoute = mockk<NavigationRoute> {
            every { directionsRoute } returns mockk()
            every { id } returns "oldid"
        }
        val newRoute = mockk<NavigationRoute> {
            every { directionsRoute } returns mockk()
            every { id } returns "newid"
        }
        val newClickPoint = Point.fromLngLat(0.0, 0.0)
        val oldClickPoint = Point.fromLngLat(1.0, 1.0)
        var oldRouteIsPrimary = true

        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { getPrimaryNavigationRoute() } answers {
                if (oldRouteIsPrimary) {
                    oldRoute
                } else {
                    newRoute
                }
            }
            every { getNavigationRoutes() } answers {
                listOf(
                    oldRoute,
                    newRoute
                ).apply {
                    if (!oldRouteIsPrimary) reversed()
                }
            }
            every { findClosestRoute(newClickPoint, any(), any(), any()) } answers {
                val consumer = args[3]
                    as MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>
                consumer.accept(
                    ExpectedFactory.createValue(
                        ClosestRouteValue(
                            newRoute
                        )
                    )
                )
            }
            every { findClosestRoute(oldClickPoint, any(), any(), any()) } answers {
                val consumer = args[3]
                    as MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>
                consumer.accept(
                    ExpectedFactory.createValue(
                        ClosestRouteValue(
                            oldRoute
                        )
                    )
                )
            }
        }

        val customContract = mockk<RouteLineComponentContract>(relaxed = true)
        val sut = RouteLineComponent(mockMap, mapPluginProvider, options, mockApi) {
            customContract
        }
        val routesObserverSlot = slot<RoutesObserver>()
        val routeProgressObserverSlot = slot<RouteProgressObserver>()
        sut.onAttached(mockMapboxNavigation)
        verify { mockGestures.addOnMapClickListener(capture(clickSlot)) }
        val clickListener = clickSlot.captured
        verify { mockMapboxNavigation.registerRoutesObserver(capture(routesObserverSlot)) }
        verify {
            mockMapboxNavigation.registerRouteProgressObserver(
                capture(
                    routeProgressObserverSlot
                )
            )
        }

        clickListener.onMapClick(newClickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(newRoute, oldRoute), null)
        }

        routeProgressObserverSlot.captured.onRouteProgressChanged(
            mockk {
                every { currentRouteGeometryIndex } returns 2
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 1
                    every { geometryIndex } returns 3
                }
                every { currentState } returns RouteProgressState.TRACKING
                every {
                    internalAlternativeRouteIndices()
                } returns mapOf("oldid" to mockk(relaxed = true) { every { legIndex } returns 5 })
            }
        )

        oldRouteIsPrimary = false
        clearAllMocks(answers = false)
        clickListener.onMapClick(oldClickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(oldRoute, newRoute), 5)
        }

        every {
            mockMapboxNavigation.getAlternativeMetadataFor(any<NavigationRoute>())
        } returns null
        oldRouteIsPrimary = true
        clearAllMocks(answers = false)
        clickListener.onMapClick(newClickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(newRoute, oldRoute), null)
        }

        oldRouteIsPrimary = false
        clearAllMocks(answers = false)
        clickListener.onMapClick(oldClickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(oldRoute, newRoute), 5)
        }

        oldRouteIsPrimary = true
        clearAllMocks(answers = false)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(oldRoute, newRoute)
            }
        )
        every { mockMapboxNavigation.currentLegIndex() } returns 4
        clickListener.onMapClick(newClickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(newRoute, oldRoute), null)
        }

        routeProgressObserverSlot.captured.onRouteProgressChanged(
            mockk {
                every { currentRouteGeometryIndex } returns 2
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 1
                    every { geometryIndex } returns 3
                }
                every { currentState } returns RouteProgressState.OFF_ROUTE
            }
        )
        oldRouteIsPrimary = false
        clearAllMocks(answers = false)
        clickListener.onMapClick(oldClickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(oldRoute, newRoute), null)
        }

        routeProgressObserverSlot.captured.onRouteProgressChanged(
            mockk {
                every { currentRouteGeometryIndex } returns 2
                every { currentLegProgress } returns null
                every { currentState } returns RouteProgressState.TRACKING
            }
        )
        oldRouteIsPrimary = true
        clearAllMocks(answers = false)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(oldRoute, newRoute)
            }
        )
        every { mockMapboxNavigation.currentLegIndex() } returns 4
        clickListener.onMapClick(newClickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(newRoute, oldRoute), null)
        }

        routeProgressObserverSlot.captured.onRouteProgressChanged(
            mockk {
                every { currentRouteGeometryIndex } returns 2
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 1
                    every { geometryIndex } returns 3
                }
                every { currentState } returns RouteProgressState.TRACKING
                every {
                    internalAlternativeRouteIndices()
                } returns mapOf("oldid" to mockk(relaxed = true) { every { legIndex } returns 5 })
            }
        )
        oldRouteIsPrimary = false
        clearAllMocks(answers = false)
        clickListener.onMapClick(oldClickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(oldRoute, newRoute), 5)
        }

        oldRouteIsPrimary = true
        clearAllMocks(answers = false)
        sut.onDetached(mockMapboxNavigation)
        sut.onAttached(mockMapboxNavigation)
        clickListener.onMapClick(newClickPoint)
        verify(exactly = 1) {
            customContract.setRoutes(mockMapboxNavigation, listOf(newRoute, oldRoute), null)
        }
    }

    @Test
    fun `onMapClicked should be called if clicked not on a route`() {
        val consumerSlot =
            slot<MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>>()
        val clickSlot = slot<OnMapClickListener>()
        val clickPoint = mockk<Point>()
        val mockApi = mockk<MapboxRouteLineApi> {
            every { findClosestRoute(clickPoint, any(), any(), capture(consumerSlot)) } answers {
                consumerSlot.captured.accept(ExpectedFactory.createError(mockk()))
            }
        }

        val customContract = mockk<RouteLineComponentContract>(relaxed = true)
        val sut = RouteLineComponent(mockMap, mapPluginProvider, options, mockApi) {
            customContract
        }
        sut.onAttached(mockMapboxNavigation)
        verify { mockGestures.addOnMapClickListener(capture(clickSlot)) }

        clickSlot.captured.onMapClick(clickPoint)
        verify(exactly = 0) { customContract.setRoutes(any(), any(), any()) }
        verify(exactly = 1) { customContract.onMapClicked(clickPoint) }
    }

    @Test
    fun `onMapClicked should be called if clicked on a primary route`() {
        val consumerSlot =
            slot<MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>>()
        val clickSlot = slot<OnMapClickListener>()
        val primaryRoute = mockk<NavigationRoute>(relaxed = true)
        val alternativeRoute = mockk<NavigationRoute>(relaxed = true)
        val clickPoint = mockk<Point>()
        val mockApi = mockk<MapboxRouteLineApi> {
            every { getPrimaryNavigationRoute() } returns primaryRoute
            every { getNavigationRoutes() } returns listOf(primaryRoute, alternativeRoute)
            every { findClosestRoute(clickPoint, any(), any(), capture(consumerSlot)) } answers {
                val result = ClosestRouteValue(primaryRoute)
                consumerSlot.captured.accept(ExpectedFactory.createValue(result))
            }
        }

        val customContract = mockk<RouteLineComponentContract>(relaxed = true)
        val sut = RouteLineComponent(mockMap, mapPluginProvider, options, mockApi) {
            customContract
        }
        sut.onAttached(mockMapboxNavigation)
        verify { mockGestures.addOnMapClickListener(capture(clickSlot)) }

        clickSlot.captured.onMapClick(clickPoint)
        verify(exactly = 0) { customContract.setRoutes(any(), any(), any()) }
        verify(exactly = 1) { customContract.onMapClicked(clickPoint) }
    }

    @Test
    fun `onAttached should initialize route line layers on mapStyle load`() {
        val lineApi = mockk<MapboxRouteLineApi>(relaxed = true)
        val lineView = mockk<MapboxRouteLineView>(relaxed = true)
        val onStyleLoadedCallback = slot<Style.OnStyleLoaded>()
        val mapStyle = mockk<Style>()
        val sut = RouteLineComponent(mockMap, mapPluginProvider, options, lineApi, lineView)
        every { mockMap.getStyle(capture(onStyleLoadedCallback)) } returns Unit

        sut.onAttached(mockMapboxNavigation)
        onStyleLoadedCallback.captured.onStyleLoaded(mapStyle)

        verify { lineView.initializeLayers(mapStyle) }
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
