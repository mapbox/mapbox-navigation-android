package com.mapbox.navigation.ui.maps.route.line.model

import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.model.FadingConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class MapboxRouteLineViewDynamicOptionsBuilderTest {

    @Before
    fun setUp() {
        mockkStatic("androidx.appcompat.content.res.AppCompatResources")
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.appcompat.content.res.AppCompatResources")
    }

    @Test
    fun allOriginalOptionsAreKept() {
        val originalColorResources = mockk<RouteLineColorResources>()
        val newColorResources = mockk<RouteLineColorResources>()
        val originalScaleExpressions = mockk<RouteLineScaleExpressions>()
        val newScaleExpressions = mockk<RouteLineScaleExpressions>()
        val oldFadingConfig = FadingConfig.Builder(16.0, 17.5).build()
        val newFadingConfig = FadingConfig.Builder(15.0, 16.5).build()
        val originalOptions = MapboxRouteLineViewOptions.Builder(mockk(relaxed = true))
            .routeLineBelowLayerId("some-layer-id")
            .originWaypointIcon(23)
            .routeLineColorResources(originalColorResources)
            .lineDepthOcclusionFactor(0.8)
            .destinationWaypointIcon(35)
            .displayRestrictedRoadSections(true)
            .displaySoftGradientForTraffic(true)
            .iconPitchAlignment(IconPitchAlignment.VIEWPORT)
            .restrictedRoadDashArray(listOf(0.2, 0.8))
            .restrictedRoadLineWidth(1.4)
            .restrictedRoadOpacity(0.3)
            .scaleExpressions(originalScaleExpressions)
            .shareLineGeometrySources(true)
            .softGradientTransition(20.0)
            .tolerance(5.0)
            .routeLineBlurEnabled(true)
            .applyTrafficColorsToRouteLineBlur(true)
            .routeLineBlurWidth(77.77)
            .slotName("foobar")
            .fadeOnHighZoomsConfig(oldFadingConfig)
            .build()

        val newOptions = MapboxRouteLineViewDynamicOptionsBuilder(originalOptions.toBuilder())
            .routeLineBelowLayerId("new-layer-id")
            .routeLineColorResources(newColorResources)
            .lineDepthOcclusionFactor(0.7)
            .scaleExpressions(newScaleExpressions)
            .displaySoftGradientForTraffic(false)
            .softGradientTransition(10.0)
            .routeLineBlurEnabled(false)
            .applyTrafficColorsToRouteLineBlur(false)
            .routeLineBlurWidth(33.3)
            .slotName("new-foobar")
            .fadingConfig(newFadingConfig)
            .build()

        val tolerance = 0.0000001
        assertEquals("new-layer-id", newOptions.routeLineBelowLayerId)
        assertEquals(23, newOptions.originIconId)
        assertEquals(newColorResources, newOptions.routeLineColorResources)
        assertEquals(0.7, newOptions.lineDepthOcclusionFactor, tolerance)
        assertEquals(35, newOptions.destinationIconId)
        assertEquals(true, newOptions.displayRestrictedRoadSections)
        assertEquals(false, newOptions.displaySoftGradientForTraffic)
        assertEquals(IconPitchAlignment.VIEWPORT, newOptions.iconPitchAlignment)
        assertEquals(listOf(0.2, 0.8), newOptions.restrictedRoadDashArray)
        assertEquals(1.4, newOptions.restrictedRoadLineWidth, tolerance)
        assertEquals(0.3, newOptions.restrictedRoadOpacity, tolerance)
        assertEquals(newScaleExpressions, newOptions.scaleExpressions)
        assertEquals(true, newOptions.shareLineGeometrySources)
        assertEquals(10.0, newOptions.softGradientTransition, tolerance)
        assertEquals(5.0, newOptions.tolerance, tolerance)
        assertEquals("new-foobar", newOptions.slotName)
        assertEquals(newFadingConfig, newOptions.fadeOnHighZoomsConfig)
        assertEquals(false, newOptions.routeLineBlurEnabled)
        assertEquals(false, newOptions.applyTrafficColorsToRouteLineBlur)
        assertEquals(33.3, newOptions.routeLineBlurWidth, tolerance)
    }
}
