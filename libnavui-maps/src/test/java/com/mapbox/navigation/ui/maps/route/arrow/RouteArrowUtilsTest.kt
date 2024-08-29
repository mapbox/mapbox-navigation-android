package com.mapbox.navigation.ui.maps.route.arrow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.Image
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_ICON
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_ICON_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_SHAFT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RouteArrowUtilsTest {

    private val ctx: Context = mockk()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

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
    fun obtainArrowPointsFromWhenCurrentArrowLessThan2Points() {
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val routeStepPoints = listOf(Point.fromLngLat(-122.477395, 37.859513))
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)

        assertTrue(result.isEmpty())
    }

    @Test
    fun obtainArrowPointsFromWhenUpComingArrowLineLessThan2Points() {
        val upcomingPoints = listOf(Point.fromLngLat(-122.477395, 37.859513))
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `obtainArrowPointsFrom does not crash when upcomming step geometry is null`() {
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns null
        }

        val result = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)

        assertTrue(result.isEmpty())
    }

    @Test
    fun obtainArrowPointsFrom() {
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val stepProgress = mockk<RouteStepProgress> {
            every { stepPoints } returns routeStepPoints
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
        }
        val routeProgress = mockk<RouteProgress> {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)

        assertEquals(4, result.size)
    }

    @Test
    fun layersAreInitialized() {
        val style = mockk<Style> {
            every { styleSourceExists(ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(ARROW_HEAD_SOURCE_ID) } returns true
            every {
                styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_HEAD_LAYER_ID) } returns true
        }

        val result = RouteArrowUtils.layersAreInitialized(style)

        assertTrue(result)
        verify { style.styleSourceExists(ARROW_SHAFT_SOURCE_ID) }
        verify { style.styleSourceExists(ARROW_HEAD_SOURCE_ID) }
        verify { style.styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID) }
        verify { style.styleLayerExists(ARROW_HEAD_CASING_LAYER_ID) }
        verify { style.styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID) }
        verify { style.styleLayerExists(ARROW_HEAD_LAYER_ID) }
    }

    @Test
    fun initializeLayers_whenLayersAreInitialized() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { styleLayers } returns listOf()
            every { styleSourceExists(ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(ARROW_HEAD_SOURCE_ID) } returns true
            every {
                styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_HEAD_LAYER_ID) } returns true
            every { styleSlots } returns listOf()
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleLayers }
        verify(exactly = 0) { style.addStyleSource(any(), any()) }
    }

    @Test
    fun initializeLayers_whenArrowHeadHeightZero() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val mockOptions = mockk<RouteArrowOptions> {
            every { aboveLayerId } returns options.aboveLayerId
            every { tolerance } returns options.tolerance
            every { arrowCasingColor } returns options.arrowCasingColor
            every { arrowColor } returns options.arrowColor
            every { arrowHeadIconCasing } returns options.arrowHeadIconCasing
            every { arrowHeadIcon } returns mockk<Drawable> {
                every { intrinsicHeight } returns 0
                every { intrinsicWidth } returns 1
            }
            every {
                arrowShaftCasingScaleExpression
            } returns options.arrowShaftCasingScaleExpression
            every {
                arrowHeadCasingScaleExpression
            } returns options.arrowHeadCasingScaleExpression
            every { arrowShaftScaleExpression } returns options.arrowShaftScaleExpression
            every { arrowHeadScaleExpression } returns options.arrowHeadScaleExpression
            every { slotName } returns options.slotName
        }
        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(ARROW_HEAD_SOURCE_ID) } returns true
            every {
                styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_HEAD_LAYER_ID) } returns true
            every { styleLayerExists(options.aboveLayerId) } returns true
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(ARROW_HEAD_ICON_CASING) } returns null
            every { getStyleImage(ARROW_HEAD_ICON) } returns null
        }

        RouteArrowUtils.initializeLayers(style, mockOptions)

        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON, any<Bitmap>()) }
        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON, any<Image>()) }
    }

    @Test
    fun initializeLayers_whenArrowHeadWidthZero() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val mockOptions = mockk<RouteArrowOptions> {
            every { aboveLayerId } returns options.aboveLayerId
            every { tolerance } returns options.tolerance
            every { arrowCasingColor } returns options.arrowCasingColor
            every { arrowColor } returns options.arrowColor
            every { arrowHeadIconCasing } returns options.arrowHeadIconCasing
            every { arrowHeadIcon } returns mockk<Drawable> {
                every { intrinsicHeight } returns 1
                every { intrinsicWidth } returns 0
            }
            every {
                arrowShaftCasingScaleExpression
            } returns options.arrowShaftCasingScaleExpression
            every {
                arrowHeadCasingScaleExpression
            } returns options.arrowHeadCasingScaleExpression
            every { arrowShaftScaleExpression } returns options.arrowShaftScaleExpression
            every { arrowHeadScaleExpression } returns options.arrowHeadScaleExpression
            every { slotName } returns options.slotName
        }
        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(ARROW_HEAD_SOURCE_ID) } returns true
            every {
                styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_HEAD_LAYER_ID) } returns true
            every { styleLayerExists(options.aboveLayerId) } returns true
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(ARROW_HEAD_ICON_CASING) } returns null
            every { getStyleImage(ARROW_HEAD_ICON) } returns null
        }

        RouteArrowUtils.initializeLayers(style, mockOptions)

        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON, any<Bitmap>()) }
        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON, any<Image>()) }
    }

    @Test
    fun initializeLayers_whenArrowHeadCasingHeightZero() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val mockOptions = mockk<RouteArrowOptions> {
            every { aboveLayerId } returns options.aboveLayerId
            every { tolerance } returns options.tolerance
            every { arrowCasingColor } returns options.arrowCasingColor
            every { arrowColor } returns options.arrowColor
            every { arrowHeadIcon } returns options.arrowHeadIcon
            every { arrowHeadIconCasing } returns mockk<Drawable> {
                every { intrinsicHeight } returns 0
                every { intrinsicWidth } returns 1
            }
            every {
                arrowShaftCasingScaleExpression
            } returns options.arrowShaftCasingScaleExpression
            every {
                arrowHeadCasingScaleExpression
            } returns options.arrowHeadCasingScaleExpression
            every { arrowShaftScaleExpression } returns options.arrowShaftScaleExpression
            every { arrowHeadScaleExpression } returns options.arrowHeadScaleExpression
            every { slotName } returns options.slotName
        }
        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(ARROW_HEAD_SOURCE_ID) } returns true
            every {
                styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_HEAD_LAYER_ID) } returns true
            every { styleLayerExists(options.aboveLayerId) } returns true
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(ARROW_HEAD_ICON_CASING) } returns null
            every { getStyleImage(ARROW_HEAD_ICON) } returns null
        }

        RouteArrowUtils.initializeLayers(style, mockOptions)

        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON_CASING, any<Bitmap>()) }
        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON_CASING, any<Image>()) }
    }

    @Test
    fun initializeLayers_whenArrowHeadCasingWidthZero() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val mockOptions = mockk<RouteArrowOptions> {
            every { aboveLayerId } returns options.aboveLayerId
            every { tolerance } returns options.tolerance
            every { arrowCasingColor } returns options.arrowCasingColor
            every { arrowColor } returns options.arrowColor
            every { arrowHeadIcon } returns options.arrowHeadIcon
            every { arrowHeadIconCasing } returns mockk<Drawable> {
                every { intrinsicHeight } returns 0
                every { intrinsicWidth } returns 1
            }
            every {
                arrowShaftCasingScaleExpression
            } returns options.arrowShaftCasingScaleExpression
            every {
                arrowHeadCasingScaleExpression
            } returns options.arrowHeadCasingScaleExpression
            every { arrowShaftScaleExpression } returns options.arrowShaftScaleExpression
            every { arrowHeadScaleExpression } returns options.arrowHeadScaleExpression
            every { slotName } returns options.slotName
        }
        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(ARROW_HEAD_SOURCE_ID) } returns true
            every {
                styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_HEAD_LAYER_ID) } returns true
            every { styleLayerExists(options.aboveLayerId) } returns true
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(ARROW_HEAD_ICON_CASING) } returns null
            every { getStyleImage(ARROW_HEAD_ICON) } returns null
        }

        RouteArrowUtils.initializeLayers(style, mockOptions)

        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON_CASING, any<Bitmap>()) }
        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON_CASING, any<Image>()) }
    }

    @Test
    fun `styles sources are removed after style layers and images`() {
        val style = mockk<Style> {
            every { removeStyleSource(ARROW_SHAFT_SOURCE_ID) } returns mockk()
            every { removeStyleSource(ARROW_HEAD_SOURCE_ID) } returns mockk()
            every { removeStyleLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID) } returns mockk()
            every { removeStyleLayer(ARROW_HEAD_CASING_LAYER_ID) } returns mockk()
            every { removeStyleLayer(ARROW_SHAFT_LINE_LAYER_ID) } returns mockk()
            every { removeStyleLayer(ARROW_HEAD_LAYER_ID) } returns mockk()
            every { removeStyleImage(ARROW_HEAD_ICON_CASING) } returns mockk()
            every { removeStyleImage(ARROW_HEAD_ICON) } returns mockk()
        }

        RouteArrowUtils.removeLayersAndSources(style)

        verifySequence {
            style.removeStyleImage(ARROW_HEAD_ICON_CASING)
            style.removeStyleImage(ARROW_HEAD_ICON)
            style.removeStyleLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID)
            style.removeStyleLayer(ARROW_HEAD_CASING_LAYER_ID)
            style.removeStyleLayer(ARROW_SHAFT_LINE_LAYER_ID)
            style.removeStyleLayer(ARROW_HEAD_LAYER_ID)
            style.removeStyleSource(ARROW_SHAFT_SOURCE_ID)
            style.removeStyleSource(ARROW_HEAD_SOURCE_ID)
        }
    }
}
