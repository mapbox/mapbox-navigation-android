package com.mapbox.navigation.ui.androidauto.preview

import androidx.car.app.CarContext
import androidx.car.app.SurfaceContainer
import com.mapbox.maps.MapSurface
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.internal.AndroidAutoLog
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarRouteLineRendererTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

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
}
