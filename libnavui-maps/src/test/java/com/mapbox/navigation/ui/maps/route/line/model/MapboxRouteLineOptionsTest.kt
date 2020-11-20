package com.mapbox.navigation.ui.maps.route.line.model

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.O_MR1])
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
    fun toBuilder() {
        val routeLineResources = RouteLineResources.Builder().build()

        val options = MapboxRouteLineOptions.Builder(ctx)
            .withRouteLineResources(routeLineResources)
            .withVanishingRouteLineEnabled(true)
            .withRouteLineBelowLayerId("someLayerId")
            .build()
            .toBuilder(ctx)
            .build()

        assertEquals(routeLineResources, options.resourceProvider)
        assertEquals("someLayerId", options.routeLineBelowLayerId)
        assertNotNull(options.vanishingRouteLine)
    }
}
