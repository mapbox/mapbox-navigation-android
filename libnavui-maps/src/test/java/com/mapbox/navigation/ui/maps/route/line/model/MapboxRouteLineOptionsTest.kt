package com.mapbox.navigation.ui.maps.route.line.model

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.mapbox.base.common.logger.Logger
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRouteLineOptionsTest {

    lateinit var ctx: Context

    private val logger: Logger = mockk()

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun withRouteLineResourceProvider() {
        val routeLineResources = RouteLineResources.Builder().build()

        val options = MapboxRouteLineOptions.Builder(ctx, logger)
            .withRouteLineResources(routeLineResources)
            .build()

        assertEquals(routeLineResources, options.resourceProvider)
    }

    @Test
    fun withRouteLineBelowLayerId() {
        val options = MapboxRouteLineOptions.Builder(ctx, logger)
            .withRouteLineBelowLayerId("someLayerId")
            .build()

        assertEquals("someLayerId", options.routeLineBelowLayerId)
    }

    @Test
    fun withTolerance() {
        val options = MapboxRouteLineOptions.Builder(ctx, logger)
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
    fun toBuilder() {
        val routeLineResources = RouteLineResources.Builder().build()
        val routeStyleDescriptors =
            listOf(RouteStyleDescriptor("foobar", Color.CYAN, Color.YELLOW))

        val options = MapboxRouteLineOptions.Builder(ctx, logger)
            .withRouteLineResources(routeLineResources)
            .withVanishingRouteLineEnabled(true)
            .withRouteLineBelowLayerId("someLayerId")
            .withTolerance(.111)
            .withRouteStyleDescriptors(routeStyleDescriptors)
            .build()
            .toBuilder(ctx, logger)
            .build()

        assertEquals(routeLineResources, options.resourceProvider)
        assertEquals("someLayerId", options.routeLineBelowLayerId)
        assertNotNull(options.vanishingRouteLine)
        assertEquals(.111, options.tolerance, 0.0)
        assertEquals(routeStyleDescriptors, options.routeLayerProvider.routeStyleDescriptors)
    }
}
