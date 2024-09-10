package com.mapbox.navigation.ui.maps.route.arrow.model

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.model.FadingConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteArrowOptionsTest {

    private val ctx: Context = mockk()

    @Before
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true) {
            every { intrinsicWidth } returns 24
            every { intrinsicHeight } returns 24
        }
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
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
            .withArrowHeadIconDrawable(RouteLayerConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE)
            .build()

        assertNotNull(options.arrowHeadIcon)
    }

    @Test
    fun withArrowHeadIconCasingDrawableTest() {
        val options = RouteArrowOptions.Builder(ctx)
            .withArrowHeadIconCasingDrawable(RouteLayerConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE)
            .build()

        assertNotNull(options.arrowHeadIconCasing)
    }

    @Test
    fun withAboveLayerIdTest() {
        val options = RouteArrowOptions.Builder(ctx).withAboveLayerId("someLayerId").build()

        assertEquals("someLayerId", options.aboveLayerId)
    }

    @Test
    fun withSlotNameTest() {
        val options = RouteArrowOptions.Builder(ctx).withSlotName("someSlotName").build()

        assertEquals("someSlotName", options.slotName)
    }

    @Test
    fun withToleranceTest() {
        val options = RouteArrowOptions.Builder(ctx).withTolerance(.111).build()

        assertEquals(.111, options.tolerance, 0.0)
    }

    @Test
    fun withArrowShaftScalingExpressionTest() {
        val expression = mockk<Expression>()

        val options = RouteArrowOptions.Builder(ctx)
            .withArrowShaftScalingExpression(expression)
            .build()

        assertEquals(expression, options.arrowShaftScaleExpression)
    }

    @Test
    fun withArrowShaftCasingScalingExpressionTest() {
        val expression = mockk<Expression>()

        val options = RouteArrowOptions.Builder(ctx)
            .withArrowShaftCasingScalingExpression(expression)
            .build()

        assertEquals(expression, options.arrowShaftCasingScaleExpression)
    }

    @Test
    fun withArrowheadScalingExpressionTest() {
        val expression = mockk<Expression>()

        val options = RouteArrowOptions.Builder(ctx)
            .withArrowheadScalingExpression(expression)
            .build()

        assertEquals(expression, options.arrowHeadScaleExpression)
    }

    @Test
    fun withArrowheadCasingScalingExpressionTest() {
        val expression = mockk<Expression>()

        val options = RouteArrowOptions.Builder(ctx)
            .withArrowheadCasingScalingExpression(expression)
            .build()

        assertEquals(expression, options.arrowHeadCasingScaleExpression)
    }

    @Test
    fun defaultShaftScaleExpression() {
        val expression = Expression.interpolate {
            linear()
            zoom()
            stop {
                literal(RouteLayerConstants.MIN_ARROW_ZOOM)
                literal(RouteLayerConstants.MIN_ZOOM_ARROW_SHAFT_SCALE)
            }
            stop {
                literal(RouteLayerConstants.MAX_ARROW_ZOOM)
                literal(RouteLayerConstants.MAX_ZOOM_ARROW_SHAFT_SCALE)
            }
        }

        val options = RouteArrowOptions.Builder(ctx).build()

        assertEquals(expression, options.arrowShaftScaleExpression)
    }

    @Test
    fun defaultShaftCasingScaleExpression() {
        val expression = Expression.interpolate {
            linear()
            zoom()
            stop {
                literal(RouteLayerConstants.MIN_ARROW_ZOOM)
                literal(RouteLayerConstants.MIN_ZOOM_ARROW_SHAFT_CASING_SCALE)
            }
            stop {
                literal(RouteLayerConstants.MAX_ARROW_ZOOM)
                literal(RouteLayerConstants.MAX_ZOOM_ARROW_SHAFT_CASING_SCALE)
            }
        }

        val options = RouteArrowOptions.Builder(ctx).build()

        assertEquals(expression, options.arrowShaftCasingScaleExpression)
    }

    @Test
    fun defaultHeadScaleExpression() {
        val expression = Expression.interpolate {
            linear()
            zoom()
            stop {
                literal(RouteLayerConstants.MIN_ARROW_ZOOM)
                literal(RouteLayerConstants.MIN_ZOOM_ARROW_HEAD_SCALE)
            }
            stop {
                literal(RouteLayerConstants.MAX_ARROW_ZOOM)
                literal(RouteLayerConstants.MAX_ZOOM_ARROW_HEAD_SCALE)
            }
        }

        val options = RouteArrowOptions.Builder(ctx).build()

        assertEquals(expression, options.arrowHeadScaleExpression)
    }

    @Test
    fun defaultHeadCasingScaleExpression() {
        val expression = Expression.interpolate {
            linear()
            zoom()
            stop {
                literal(RouteLayerConstants.MIN_ARROW_ZOOM)
                literal(RouteLayerConstants.MIN_ZOOM_ARROW_HEAD_CASING_SCALE)
            }
            stop {
                literal(RouteLayerConstants.MAX_ARROW_ZOOM)
                literal(RouteLayerConstants.MAX_ZOOM_ARROW_HEAD_CASING_SCALE)
            }
        }

        val options = RouteArrowOptions.Builder(ctx).build()

        assertEquals(expression, options.arrowHeadCasingScaleExpression)
    }

    @Test
    fun defaultFadeOnHighZoomsConfig() {
        val options = RouteArrowOptions.Builder(ctx).build()

        assertNull(options.fadeOnHighZoomsConfig)
    }

    @Test
    fun fadeOnHighZoomsConfigStartISLessThanFinish() {
        val config = FadingConfig.Builder(16.0, 16.1).build()

        val options = RouteArrowOptions.Builder(ctx).withFadeOnHighZoomsConfig(config).build()

        assertEquals(config, options.fadeOnHighZoomsConfig)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fadeOnHighZoomsConfigStartIsGreaterThanFinish() {
        val config = FadingConfig.Builder(16.1, 16.0).build()

        RouteArrowOptions.Builder(ctx).withFadeOnHighZoomsConfig(config).build()
    }

    @Test
    fun fadeOnHighZoomsConfigStartIsEqualToFinish() {
        val config = FadingConfig.Builder(16.0, 16.0).build()

        val options = RouteArrowOptions.Builder(ctx).withFadeOnHighZoomsConfig(config).build()

        assertEquals(config, options.fadeOnHighZoomsConfig)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fadeOnHighZoomsConfigStartIsLessThan14() {
        val config = FadingConfig.Builder(13.9, 14.1).build()

        RouteArrowOptions.Builder(ctx).withFadeOnHighZoomsConfig(config).build()
    }

    @Test
    fun toBuilder() {
        val shaftExpression = mockk<Expression>()
        val shaftCasingExpression = mockk<Expression>()
        val headExpression = mockk<Expression>()
        val headCasingExpression = mockk<Expression>()
        val fadingConfig = FadingConfig.Builder(16.0, 16.1).build()

        val options = RouteArrowOptions.Builder(ctx)
            .withArrowColor(1)
            .withAboveLayerId("someLayerId")
            .withArrowCasingColor(2)
            .withArrowHeadIconDrawable(RouteLayerConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE)
            .withArrowHeadIconCasingDrawable(RouteLayerConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE)
            .withTolerance(.111)
            .withArrowShaftScalingExpression(shaftExpression)
            .withArrowShaftCasingScalingExpression(shaftCasingExpression)
            .withArrowheadScalingExpression(headExpression)
            .withArrowheadCasingScalingExpression(headCasingExpression)
            .withFadeOnHighZoomsConfig(fadingConfig)
            .build()
            .toBuilder(ctx)
            .build()

        assertEquals(1, options.arrowColor)
        assertEquals(2, options.arrowCasingColor)
        assertEquals("someLayerId", options.aboveLayerId)
        assertNotNull(options.arrowHeadIcon)
        assertNotNull(options.arrowHeadIconCasing)
        assertEquals(.111, options.tolerance, 0.0)
        assertEquals(shaftExpression, options.arrowShaftScaleExpression)
        assertEquals(shaftCasingExpression, options.arrowShaftCasingScaleExpression)
        assertEquals(headExpression, options.arrowHeadScaleExpression)
        assertEquals(headCasingExpression, options.arrowHeadCasingScaleExpression)
        assertEquals(fadingConfig, options.fadeOnHighZoomsConfig)
    }
}
