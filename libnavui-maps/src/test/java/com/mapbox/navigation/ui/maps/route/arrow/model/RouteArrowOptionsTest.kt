package com.mapbox.navigation.ui.maps.route.arrow.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RouteArrowOptionsTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun withArrowColorTest() {
        val options = RouteArrowOptions.Builder(ctx).withArrowColor(5).build()

        assertEquals(5, options.arrowColor)
    }

    @Test
    fun withArrowCasingColorTest() {
        val options = RouteArrowOptions.Builder(ctx).withArrowCasingColor(5).build()

        assertEquals(5, options.arrowCasingColor)
    }

    @Test
    fun withArrowHeadIconDrawableTest() {
        val options = RouteArrowOptions.Builder(ctx)
            .withArrowHeadIconDrawable(RouteConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE)
            .build()

        assertNotNull(options.arrowHeadIcon)
    }

    @Test
    fun withArrowHeadIconCasingDrawableTest() {
        val options = RouteArrowOptions.Builder(ctx)
            .withArrowHeadIconCasingDrawable(RouteConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE)
            .build()

        assertNotNull(options.arrowHeadIconCasing)
    }

    @Test
    fun withAboveLayerIdTest() {
        val options = RouteArrowOptions.Builder(ctx).withAboveLayerId("someLayerId").build()

        assertEquals("someLayerId", options.aboveLayerId)
    }

    @Test
    fun withToleranceTest() {
        val options = RouteArrowOptions.Builder(ctx).withTolerance(.111).build()

        assertEquals(.111, options.tolerance, 0.0)
    }

    @Test
    fun toBuilder() {
        val options = RouteArrowOptions.Builder(ctx)
            .withArrowColor(1)
            .withAboveLayerId("someLayerId")
            .withArrowCasingColor(2)
            .withArrowHeadIconDrawable(RouteConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE)
            .withArrowHeadIconCasingDrawable(RouteConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE)
            .withTolerance(.111)
            .build()
            .toBuilder(ctx)
            .build()

        assertEquals(1, options.arrowColor)
        assertEquals(2, options.arrowCasingColor)
        assertEquals("someLayerId", options.aboveLayerId)
        assertNotNull(options.arrowHeadIcon)
        assertNotNull(options.arrowHeadIconCasing)
        assertEquals(.111, options.tolerance, 0.0)
    }
}
