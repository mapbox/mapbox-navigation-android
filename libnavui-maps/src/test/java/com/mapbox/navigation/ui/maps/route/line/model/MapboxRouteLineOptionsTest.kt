package com.mapbox.navigation.ui.maps.route.line.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun toBuilder() {
        val routeLineResources = RouteLineResources.Builder().build()

        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineResources(routeLineResources)
            .withVanishingRouteLineEnabled(true)
            .withRouteLineBelowLayerId("someLayerId")
            .withTolerance(.111)
            .build()
            .toBuilder(ctx)
            .build()

        assertEquals(routeLineResources, options.resourceProvider)
        assertEquals("someLayerId", options.routeLineBelowLayerId)
        assertNotNull(options.vanishingRouteLine)
        assertEquals(.111, options.tolerance, 0.0)
    }
}
