package com.mapbox.navigation.ui.androidauto.preview

import androidx.car.app.CarContext
import androidx.car.app.SurfaceContainer
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Point
import com.mapbox.maps.MapSurface
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.LowMemoryManager
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.internal.AndroidAutoLog
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.ClearArrowsValue
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, MapboxExperimental::class)
class CarRouteLineRendererTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val styleMock: Style = mockk(relaxed = true)
    private val mapboxMapMock: MapboxMap = mockk(relaxed = true)
    private val locationComponentMock: LocationComponentPlugin = mockk(relaxed = true)
    private val mapSurfaceMock: MapSurface = mockk(relaxed = true)
    private val carContextMock: CarContext = mockk(relaxed = true)
    private val surfaceContainerMock: SurfaceContainer = mockk(relaxed = true)
    private val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)
    private val noRoutesProvider = object : CarRoutesProvider {
        override val navigationRoutes = emptyFlow<List<NavigationRoute>>()
    }

    init {
        every { mapboxMapMock.style } returns styleMock
        // Both the deprecated method (used by the style-loaded listener registration) and the
        // non-deprecated property (used to read the current style) must resolve to the same map.
        every { mapSurfaceMock.getMapboxMap() } returns mapboxMapMock
        every { mapSurfaceMock.mapboxMap } returns mapboxMapMock
        // The `location` extension property force-casts whatever getPlugin() returns; without an
        // explicit stub it resolves to a generic, untyped relaxed mock that fails that cast.
        every {
            mapSurfaceMock.getPlugin<LocationComponentPlugin>(any())
        } returns locationComponentMock
        every { mapboxCarMapSurface.mapSurface } returns mapSurfaceMock
        every { mapboxCarMapSurface.carContext } returns carContextMock
        every { mapboxCarMapSurface.surfaceContainer } returns surfaceContainerMock
    }

    @Test
    fun `a provider that throws does not crash onAttached, and is logged`() {
        val options = CarRouteLineRendererOptions.Builder()
            .routeLineViewOptionsProvider { throw IllegalStateException("customization bug") }
            .build()
        val sut = CarRouteLineRenderer(options, noRoutesProvider)
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        carAppTestRule.onAttached(mapboxNavigation)

        // Must not throw.
        sut.onAttached(mapboxCarMapSurface)

        verify { AndroidAutoLog.logAndroidAutoFailure(any(), any()) }
    }

    @Test
    fun `a provider that throws leaves the renderer safe to detach`() {
        val options = CarRouteLineRendererOptions.Builder()
            .routeArrowApiProvider { throw IllegalStateException("customization bug") }
            .build()
        val sut = CarRouteLineRenderer(options, noRoutesProvider)
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)

        // With no successfully-built style, the position/route-progress observers guard on a
        // null style and must not reach the (never-initialized) route line/arrow renderers.
        sut.onDetached(mapboxCarMapSurface)
    }

    @Test
    fun `preview routes ignore guidance progress and puck updates until guidance is restored`() {
        mockkObject(LowMemoryManager.Companion)
        every { LowMemoryManager.create() } returns mockk(relaxed = true)
        every { AndroidAutoLog.logAndroidAutoFailure(any(), any()) } answers {
            throw AssertionError(firstArg<String>(), secondArg<Throwable?>())
        }
        mockkConstructor(
            MapboxRouteLineApi::class,
            MapboxRouteLineView::class,
            MapboxRouteArrowView::class,
        )
        val activeRoute = mockk<NavigationRoute> { every { id } returns "active" }
        val previewRoute = mockk<NavigationRoute> { every { id } returns "favorite" }
        var displayedRoute: NavigationRoute? = null
        every {
            anyConstructed<MapboxRouteLineApi>().getPrimaryNavigationRoute()
        } answers { displayedRoute }
        every {
            anyConstructed<MapboxRouteLineApi>().setNavigationRoutes(
                any(),
                any<List<AlternativeRouteMetadata>>(),
                any(),
            )
        } answers { displayedRoute = firstArg<List<NavigationRoute>>().firstOrNull() }
        every {
            anyConstructed<MapboxRouteLineApi>().updateWithRouteProgress(any(), any())
        } just runs
        every {
            anyConstructed<MapboxRouteLineApi>().updateTraveledRouteLine(any())
        } returns mockk(relaxed = true)
        every {
            anyConstructed<MapboxRouteLineView>().initializeLayers(any())
        } just runs
        every {
            anyConstructed<MapboxRouteLineView>().renderRouteLineUpdate(any(), any())
        } just runs
        every {
            anyConstructed<MapboxRouteArrowView>().render(any(), any<ClearArrowsValue>())
        } just runs
        every {
            anyConstructed<MapboxRouteArrowView>().renderManeuverUpdate(any(), any())
        } just runs
        val progressObserver = slot<RouteProgressObserver>()
        val positionListener = slot<OnIndicatorPositionChangedListener>()
        every {
            locationComponentMock.addOnIndicatorPositionChangedListener(capture(positionListener))
        } just runs
        val navigation = mockk<MapboxNavigation>(relaxed = true) {
            every { getNavigationRoutes() } returns listOf(activeRoute)
            every { registerRouteProgressObserver(capture(progressObserver)) } just runs
        }
        every { MapboxNavigationApp.current() } returns navigation
        carAppTestRule.onAttached(navigation)
        val routes = MutableStateFlow(listOf(activeRoute))
        val routesProvider = object : CarRoutesProvider {
            override val navigationRoutes = routes
        }
        val arrowApi = mockk<MapboxRouteArrowApi>(relaxed = true)
        val sut = CarRouteLineRenderer(
            CarRouteLineRendererOptions.Builder()
                .routeLineViewOptionsProvider { mockk(relaxed = true) }
                .routeArrowApiProvider { arrowApi }
                .routeArrowOptionsProvider { mockk(relaxed = true) }
                .build(),
            routesProvider,
        )
        sut.onAttached(mapboxCarMapSurface)
        val progress = mockk<RouteProgress> { every { navigationRoute } returns activeRoute }
        val point = Point.fromLngLat(21.0, 52.0)
        progressObserver.captured.onRouteProgressChanged(progress)
        verify { arrowApi.addUpcomingManeuverArrow(progress) }
        clearMocks(arrowApi, answers = false)

        routes.value = listOf(previewRoute)
        progressObserver.captured.onRouteProgressChanged(progress)
        positionListener.captured.onIndicatorPositionChanged(point)

        verify(exactly = 1) { arrowApi.clearArrows() }
        verify(exactly = 0) { arrowApi.addUpcomingManeuverArrow(any()) }
        verify(exactly = 1) {
            anyConstructed<MapboxRouteLineApi>().updateWithRouteProgress(progress, any())
        }
        verify(exactly = 0) {
            anyConstructed<MapboxRouteLineApi>().updateTraveledRouteLine(any())
        }
        routes.value = listOf(activeRoute)
        progressObserver.captured.onRouteProgressChanged(progress)
        positionListener.captured.onIndicatorPositionChanged(point)
        verify(exactly = 1) { arrowApi.addUpcomingManeuverArrow(progress) }
        verify(exactly = 2) {
            anyConstructed<MapboxRouteLineApi>().updateWithRouteProgress(progress, any())
        }
        verify(exactly = 1) {
            anyConstructed<MapboxRouteLineApi>().updateTraveledRouteLine(point)
        }
        sut.onDetached(mapboxCarMapSurface)
    }

    @Test
    fun `route clicks are received from the map while attached`() {
        val interaction: Cancelable = mockk(relaxed = true)
        every { mapboxMapMock.addInteraction(any()) } returns interaction
        val sut = CarRouteLineRenderer(
            CarRouteLineRendererOptions.Builder().build(),
            noRoutesProvider,
            onRouteClick = {},
        )
        sut.onAttached(mapboxCarMapSurface)

        sut.onDetached(mapboxCarMapSurface)

        verifyOrder {
            mapboxMapMock.addInteraction(any())
            interaction.cancel()
        }
    }

    @Test
    fun `no map click interaction is added without a route click callback`() {
        val sut = CarRouteLineRenderer(carRoutesProvider = noRoutesProvider)

        sut.onAttached(mapboxCarMapSurface)

        verify(exactly = 0) { mapboxMapMock.addInteraction(any()) }
    }

    @Test
    fun `the constructors of the previous release are kept for Java and Kotlin callers`() {
        val rendererClass = CarRouteLineRenderer::class.java

        rendererClass.getConstructor()
        rendererClass.getConstructor(
            CarRouteLineRendererOptions::class.java,
            CarRoutesProvider::class.java,
        )
        // Kotlin callers that rely on default arguments call this synthetic constructor.
        rendererClass.getConstructor(
            CarRouteLineRendererOptions::class.java,
            CarRoutesProvider::class.java,
            Int::class.javaPrimitiveType,
            Class.forName("kotlin.jvm.internal.DefaultConstructorMarker"),
        )
    }

    @Test
    fun `a null route click callback adds no map click interaction`() {
        val sut = CarRouteLineRenderer(
            CarRouteLineRendererOptions.Builder().build(),
            noRoutesProvider,
            onRouteClick = null,
        )

        sut.onAttached(mapboxCarMapSurface)

        verify(exactly = 0) { mapboxMapMock.addInteraction(any()) }
        sut.onDetached(mapboxCarMapSurface)
    }
}
