package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.ui.androidauto.preview.CarRouteLineRendererOptions.Builder.Companion.findRoadLabelsLayerId
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CarRouteLineRendererOptionsTest {

    private fun mockSurface(): MapboxCarMapSurface = mockk(relaxed = true)

    // region defaults that don't require a live Style/CarContext to exercise

    @Test
    fun `default routeLineApiOptionsProvider enables the vanishing route line`() {
        val options = CarRouteLineRendererOptions.Builder().build()

        val apiOptions = options.routeLineApiOptionsProvider(mockSurface())

        assertTrue(apiOptions.vanishingRouteLineEnabled)
    }

    @Test
    fun `default routeLineApiOptionsProvider ignores the surface it is given`() {
        val options = CarRouteLineRendererOptions.Builder().build()
        val surface: MapboxCarMapSurface = mockk()

        options.routeLineApiOptionsProvider(surface)

        verify(exactly = 0) { surface.carContext }
        verify(exactly = 0) { surface.mapSurface }
    }

    @Test
    fun `default routeArrowApiProvider returns a new MapboxRouteArrowApi and ignores the surface`() {
        val options = CarRouteLineRendererOptions.Builder().build()
        val surface: MapboxCarMapSurface = mockk()

        val first = options.routeArrowApiProvider(surface)
        val second = options.routeArrowApiProvider(surface)

        assertTrue(first is MapboxRouteArrowApi)
        assertTrue(second is MapboxRouteArrowApi)
        assertTrue(first !== second)
        verify(exactly = 0) { surface.carContext }
        verify(exactly = 0) { surface.mapSurface }
    }

    // endregion

    // region the road-label layer lookup: the edge case this refactor introduced, since Style
    // is no longer guaranteed non-null once providers are keyed off MapboxCarMapSurface instead

    @Test
    fun `findRoadLabelsLayerId returns the matching layer id when present`() {
        val style: Style = mockk {
            every { styleLayers } returns listOf(
                mockLayer("road-label-navigation"),
                mockLayer("water"),
            )
        }

        assertEquals("road-label-navigation", findRoadLabelsLayerId(style))
    }

    @Test
    fun `findRoadLabelsLayerId picks the first matching layer when several match`() {
        val style: Style = mockk {
            every { styleLayers } returns listOf(
                mockLayer("road-label-navigation"),
                mockLayer("road-label-navigation-2"),
            )
        }

        assertEquals("road-label-navigation", findRoadLabelsLayerId(style))
    }

    @Test
    fun `findRoadLabelsLayerId falls back to a default id when no layer matches`() {
        val style: Style = mockk {
            every { styleLayers } returns listOf(mockLayer("water"), mockLayer("building"))
        }

        assertEquals("road-label-navigation", findRoadLabelsLayerId(style))
    }

    @Test
    fun `findRoadLabelsLayerId falls back to a default id when the style is null`() {
        assertEquals("road-label-navigation", findRoadLabelsLayerId(null))
    }

    private fun mockLayer(layerId: String): StyleObjectInfo = StyleObjectInfo(layerId, "line")

    // endregion

    // region Builder wiring: defaults are installed, overrides replace them, and the surface
    // passed by the caller is what reaches a custom provider unchanged

    @Test
    fun `build with no customization installs the default providers`() {
        val options = CarRouteLineRendererOptions.Builder().build()

        assertSame(
            CarRouteLineRendererOptions.Builder.DEFAULT_ROUTE_LINE_API_OPTIONS_PROVIDER,
            options.routeLineApiOptionsProvider,
        )
        assertSame(
            CarRouteLineRendererOptions.Builder.DEFAULT_ROUTE_LINE_VIEW_OPTIONS_PROVIDER,
            options.routeLineViewOptionsProvider,
        )
        assertSame(
            CarRouteLineRendererOptions.Builder.DEFAULT_ROUTE_ARROW_API_PROVIDER,
            options.routeArrowApiProvider,
        )
        assertSame(
            CarRouteLineRendererOptions.Builder.DEFAULT_ROUTE_ARROW_OPTIONS_PROVIDER,
            options.routeArrowOptionsProvider,
        )
    }

    @Test
    fun `custom providers replace the defaults and receive the exact surface passed in`() {
        val surface = mockSurface()
        val customApiOptions: MapboxRouteLineApiOptions = mockk()
        val customViewOptions: MapboxRouteLineViewOptions = mockk()
        val customArrowApi: MapboxRouteArrowApi = mockk()
        val customArrowOptions: RouteArrowOptions = mockk()
        val receivedSurfaces = mutableListOf<MapboxCarMapSurface>()

        val options = CarRouteLineRendererOptions.Builder()
            .routeLineApiOptionsProvider {
                receivedSurfaces.add(it)
                customApiOptions
            }
            .routeLineViewOptionsProvider {
                receivedSurfaces.add(it)
                customViewOptions
            }
            .routeArrowApiProvider {
                receivedSurfaces.add(it)
                customArrowApi
            }
            .routeArrowOptionsProvider {
                receivedSurfaces.add(it)
                customArrowOptions
            }
            .build()

        assertSame(customApiOptions, options.routeLineApiOptionsProvider(surface))
        assertSame(customViewOptions, options.routeLineViewOptionsProvider(surface))
        assertSame(customArrowApi, options.routeArrowApiProvider(surface))
        assertSame(customArrowOptions, options.routeArrowOptionsProvider(surface))
        assertTrue(receivedSurfaces.all { it === surface })
    }

    // endregion
}
