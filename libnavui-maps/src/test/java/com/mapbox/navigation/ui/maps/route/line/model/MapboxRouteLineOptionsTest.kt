package com.mapbox.navigation.ui.maps.route.line.model

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineOptionsTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun withRouteLineResourceProvider() {
        val routeLineResources = RouteLineResources.Builder().build()

        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineResources(routeLineResources)
            .build()

        assertEquals(routeLineResources, options.resourceProvider)
    }

    @Test
    fun withRouteLineBelowLayerId() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineBelowLayerId("someLayerId")
            .build()

        assertEquals("someLayerId", options.routeLineBelowLayerId)
    }

    @Test
    fun withTolerance() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withTolerance(.111)
            .build()

        assertEquals(.111, options.tolerance, 0.0)
    }

    @Test
    fun withRouteStyleDescriptors() {
        val routeStyleDescriptors =
            listOf(RouteStyleDescriptor("foobar", Color.CYAN, Color.YELLOW))
        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteStyleDescriptors(routeStyleDescriptors)
            .build()

        assertEquals(routeStyleDescriptors, options.routeLayerProvider.routeStyleDescriptors)
    }

    @Test
    fun displayRestrictedRoadSections() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .displayRestrictedRoadSections(true)
            .build()

        assertTrue(options.displayRestrictedRoadSections)
    }

    @Test
    fun styleInactiveRouteLegsIndependently() {
        val options = MapboxRouteLineOptions.Builder(ctx)
            .styleInactiveRouteLegsIndependently(true)
            .build()

        assertTrue(options.styleInactiveRouteLegsIndependently)
    }

    @Test
    fun toBuilder() {
        val routeLineResources = RouteLineResources.Builder().build()
        val routeStyleDescriptors =
            listOf(RouteStyleDescriptor("foobar", Color.CYAN, Color.YELLOW))

        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineResources(routeLineResources)
            .withVanishingRouteLineEnabled(true)
            .withRouteLineBelowLayerId("someLayerId")
            .withTolerance(.111)
            .withRouteStyleDescriptors(routeStyleDescriptors)
            .displayRestrictedRoadSections(true)
            .styleInactiveRouteLegsIndependently(true)
            .build()
            .toBuilder(ctx)
            .build()

        assertEquals(routeLineResources, options.resourceProvider)
        assertEquals("someLayerId", options.routeLineBelowLayerId)
        assertNotNull(options.vanishingRouteLine)
        assertEquals(.111, options.tolerance, 0.0)
        assertEquals(routeStyleDescriptors, options.routeLayerProvider.routeStyleDescriptors)
        assertTrue(options.displayRestrictedRoadSections)
        assertTrue(options.styleInactiveRouteLegsIndependently)
    }
}
