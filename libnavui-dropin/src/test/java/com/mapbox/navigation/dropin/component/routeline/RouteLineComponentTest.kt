package com.mapbox.navigation.dropin.component.routeline

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.android.gestures.Utils
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.map.MapEventProducer
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
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
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.JobControl
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RouteLineComponentTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val _mapClickSink: MutableSharedFlow<Point> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _mapStyleSink: MutableSharedFlow<Style> = MutableSharedFlow(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        extraBufferCapacity = 1
    )
    private val _positionChangeSink: MutableSharedFlow<Point> = MutableSharedFlow(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        extraBufferCapacity = 1
    )

    private val context = mockk<Context>() {
        every { resources } returns mockk()
    }
    private val mockMap = mockk<MapboxMap>(relaxed = true)
    private val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val options by lazy { MapboxRouteLineOptions.Builder(context).build() }
    private val mapEventProducer = mockk<MapEventProducer>() {
        every { mapClicks } returns _mapClickSink
        every { mapStyleUpdates } returns _mapStyleSink
        every { positionChanges } returns _positionChangeSink
    }
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
    }

    @After
    fun tearDown() {
        unmockkStatic(AppCompatResources::class)
        unmockkStatic(Utils::class)
    }

    @Test
    fun `onAttached subscribes to map clicks`() {
        val mockResponse = mockk<RouteNotFound>()
        val consumerSlot =
            slot<MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>>()
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { findClosestRoute(any(), any(), any(), capture(consumerSlot)) } returns Unit
        }
        val point = Point.fromLngLat(-119.27, 84.85)
        RouteLineComponent(mockMap, options, mapEventProducer, routesViewModel, mockApi)
            .onAttached(mockMapboxNavigation)

        _mapClickSink.tryEmit(point)
        consumerSlot.captured.accept(ExpectedFactory.createError(mockResponse))

        verify { mockApi.findClosestRoute(point, mockMap, any(), any()) }
    }

    @Test
    fun `onAttached subscribes to style updates`() {
        val style = mockk<Style>()
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true)
        val routeProgressObserverSlot = slot<RouteProgressObserver>()
        RouteLineComponent(mockMap, options, mapEventProducer, routesViewModel, mockApi)
            .onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRouteProgressObserver(capture(routeProgressObserverSlot))
        }

        _mapStyleSink.tryEmit(style)
        routeProgressObserverSlot.captured.onRouteProgressChanged(mockk())

        verify { mockApi.updateWithRouteProgress(any(), any()) }
    }

    @Test
    fun `onAttached subscribes to position changes`() {
        val point = Point.fromLngLat(-119.27, 84.85)
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { updateTraveledRouteLine(point) } returns ExpectedFactory.createError(mockk())
        }
        RouteLineComponent(mockMap, options, mapEventProducer, routesViewModel, mockApi)
            .onAttached(mockMapboxNavigation)

        _positionChangeSink.tryEmit(point)

        verify { mockApi.updateTraveledRouteLine(point) }
    }

    @Test
    fun routeProgressObserverTest() {
        val style = mockk<Style>()
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
        RouteLineComponent(mockMap, options, mapEventProducer, routesViewModel, mockApi, mockView)
            .onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRouteProgressObserver(capture(routeProgressObserverSlot))
        }
        _mapStyleSink.tryEmit(style)

        routeProgressObserverSlot.captured.onRouteProgressChanged(routeProgress)

        verify { mockApi.updateWithRouteProgress(routeProgress, capture(callbackSlot)) }
        callbackSlot.captured.accept(callbackResult)
        verify { mockView.renderRouteLineUpdate(style, callbackResult) }
    }

    @Test
    fun routesObserverTest() {
        val mockRoutes = listOf<NavigationRoute>(mockk())
        val style = mockk<Style>()
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
        RouteLineComponent(mockMap, options, mapEventProducer, routesViewModel, mockApi, mockView)
            .onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        }
        _mapStyleSink.tryEmit(style)

        routesObserverSlot.captured.onRoutesChanged(routesUpdateResult)

        verify { mockApi.setNavigationRouteLines(any(), capture(callbackSlot)) }
        callbackSlot.captured.accept(expectedMockError)
        verify { mockView.renderRouteDrawData(style, expectedMockError) }
    }

    @Test
    fun selectRouteTest() {
        val route1 = TestingUtil.loadRoute("short_route.json")
        val route2 = TestingUtil.loadRoute("multileg_route.json")
        val resultRoutesSlot = slot<RoutesAction.SetRoutes>()
        val mockResponse = mockk<ClosestRouteValue> {
            every { route } returns route2
        }
        val consumerSlot =
            slot<MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>>()
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true) {
            every { findClosestRoute(any(), any(), any(), capture(consumerSlot)) } returns Unit
            every { getRoutes() } returns listOf(route1, route2)
        }
        val point = Point.fromLngLat(-119.27, 84.85)
        RouteLineComponent(mockMap, options, mapEventProducer, routesViewModel, mockApi)
            .onAttached(mockMapboxNavigation)

        _mapClickSink.tryEmit(point)
        consumerSlot.captured.accept(ExpectedFactory.createValue(mockResponse))

        verify { mockApi.findClosestRoute(point, mockMap, any(), any()) }

        coVerify { routesViewModel.invoke(capture(resultRoutesSlot)) }
        assertEquals(route2.toNavigationRoute(), resultRoutesSlot.captured.routes.first())
        assertEquals(route1.toNavigationRoute(), resultRoutesSlot.captured.routes[1])
    }

    @Test
    fun `onDetached cancels route line API`() {
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true)

        RouteLineComponent(mockMap, options, mapEventProducer, routesViewModel, mockApi)
            .onDetached(mockMapboxNavigation)

        verify { mockApi.cancel() }
    }

    @Test
    fun `onDetached cancels route line view`() {
        val mockApi = mockk<MapboxRouteLineApi>(relaxed = true)
        val mockView = mockk<MapboxRouteLineView>(relaxed = true)

        RouteLineComponent(mockMap, options, mapEventProducer, routesViewModel, mockApi, mockView)
            .onDetached(mockMapboxNavigation)

        verify { mockView.cancel() }
    }

    @Test
    fun `onDetached cancels coroutines`() {
        mockkObject(InternalJobControlFactory)
        val mockParentJob = mockk<CompletableJob>(relaxed = true)
        val mockJobControl = mockk<JobControl> {
            every { job } returns mockParentJob
        }
        every { InternalJobControlFactory.createMainScopeJobControl() } returns mockJobControl

        RouteLineComponent(
            mockMap,
            options,
            mapEventProducer,
            routesViewModel
        ).onDetached(mockMapboxNavigation)

        verify { mockParentJob.cancelChildren() }
        unmockkObject(InternalJobControlFactory)
    }
}
