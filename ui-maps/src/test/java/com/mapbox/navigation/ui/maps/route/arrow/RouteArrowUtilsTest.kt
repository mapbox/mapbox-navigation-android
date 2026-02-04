package com.mapbox.navigation.ui.maps.route.arrow

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.Image
import com.mapbox.maps.MapboxExperimental
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
import com.mapbox.navigation.ui.maps.util.StyleManager
import com.mapbox.navigation.ui.maps.util.sdkStyleManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class RouteArrowUtilsTest {

    private val ctx: Context = mockk {
        every { resources } returns mockk {
            every { configuration } returns Configuration()
        }
        every { createConfigurationContext(any()) } returns mockk()
    }

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Before
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        mockkStatic(Style::sdkStyleManager)
        mockkStatic(DrawableCompat::class)
        mockkStatic(Drawable::toBitmap)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk {
            every { intrinsicWidth } returns 24
            every { intrinsicHeight } returns 24
            every { mutate() } returns this
        }
        every { DrawableCompat.wrap(any()) } answers { firstArg() }
        every { any<Drawable>().toBitmap(any(), any(), any()) } answers {
            val width = secondArg<Int>()
            val height = thirdArg<Int>()
            mockk<Bitmap> {
                every { this@mockk.width } returns width
                every { this@mockk.height } returns height
            }
        }
    }

    @After
    fun cleanUp() {
        unmockkStatic(AppCompatResources::class)
        unmockkStatic(Style::sdkStyleManager)
        unmockkStatic(DrawableCompat::class)
        unmockkStatic(Drawable::toBitmap)
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
            every { stepIndex } returns 0
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
            every { routeLeg } returns mockk(relaxed = true)
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
            every { stepIndex } returns 0
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
            every { routeLeg } returns mockk(relaxed = true)
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
            every { stepIndex } returns 0
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
            every { routeLeg } returns mockk(relaxed = true)
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
            every { stepIndex } returns 0
        }
        val routeLegProgress = mockk<RouteLegProgress> {
            every { currentStepProgress } returns stepProgress
            every { routeLeg } returns mockk(relaxed = true)
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
            every { pixelRatio } returns 1.0f
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
            every { pixelRatio } returns 1.0f
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
            every { pixelRatio } returns 1.0f
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
            every { pixelRatio } returns 1.0f
        }

        RouteArrowUtils.initializeLayers(style, mockOptions)

        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON_CASING, any<Bitmap>()) }
        verify(exactly = 0) { style.addImage(ARROW_HEAD_ICON_CASING, any<Image>()) }
    }

    @Test
    fun `styles sources are removed after style layers and images`() {
        val style = mockk<StyleManager> {
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

    @Test
    fun `isArrivalStep returns true when next step is arrival`() {
        val arriveManeuver = mockk<StepManeuver>(relaxed = true)
        every { arriveManeuver.type() } returns StepManeuver.ARRIVE

        val nextStep = mockk<LegStep>(relaxed = true)
        every { nextStep.maneuver() } returns arriveManeuver

        val routeLeg = mockk<RouteLeg>(relaxed = true)
        every { routeLeg.steps() } returns listOf(mockk(relaxed = true), nextStep)

        val stepProgress = mockk<RouteStepProgress>(relaxed = true)
        every { stepProgress.stepIndex } returns 0

        val routeLegProgress = mockk<RouteLegProgress>(relaxed = true)
        every { routeLegProgress.currentStepProgress } returns stepProgress
        every { routeLegProgress.routeLeg } returns routeLeg

        val routeProgress = mockk<RouteProgress>(relaxed = true)
        every { routeProgress.currentLegProgress } returns routeLegProgress

        val result = RouteArrowUtils.isArrivalStep(routeProgress)

        assertTrue(result)
    }

    @Test
    fun `isArrivalStep returns false when next step is not arrival`() {
        val turnManeuver = mockk<StepManeuver>(relaxed = true) {
            every { type() } returns StepManeuver.TURN
        }
        val nextStep = mockk<LegStep>(relaxed = true) {
            every { maneuver() } returns turnManeuver
        }
        val mockRouteLeg = mockk<RouteLeg>(relaxed = true) {
            every { steps() } returns listOf(mockk(relaxed = true), nextStep)
        }
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { stepIndex } returns 0
        }
        val routeLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
            every { routeLeg } returns mockRouteLeg
        }
        val routeProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns routeLegProgress
        }

        val result = RouteArrowUtils.isArrivalStep(routeProgress)

        assertFalse(result)
    }

    @Test
    fun `isArrivalStep returns false when there is no next step`() {
        val mockRouteLeg = mockk<RouteLeg>(relaxed = true) {
            every { steps() } returns listOf(mockk(relaxed = true))
        }
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { stepIndex } returns 0
        }
        val routeLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
            every { routeLeg } returns mockRouteLeg
        }
        val routeProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns routeLegProgress
        }

        val result = RouteArrowUtils.isArrivalStep(routeProgress)

        assertFalse(result)
    }

    @Test
    fun `isArrivalStep returns false when current leg progress is null`() {
        val routeProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns null
        }

        val result = RouteArrowUtils.isArrivalStep(routeProgress)

        assertFalse(result)
    }

    @Test
    fun `isArrivalStep returns false when current step progress is null`() {
        val routeLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns null
            every { routeLeg } returns mockk(relaxed = true)
        }
        val routeProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns routeLegProgress
        }

        val result = RouteArrowUtils.isArrivalStep(routeProgress)

        assertFalse(result)
    }

    @Test
    fun `isArrivalStep returns false when next step maneuver type is null`() {
        val maneuver = mockk<StepManeuver>(relaxed = true) {
            every { type() } returns null
        }
        val nextStep = mockk<LegStep>(relaxed = true) {
            every { maneuver() } returns maneuver
        }
        val mockRouteLeg = mockk<RouteLeg>(relaxed = true) {
            every { steps() } returns listOf(mockk(relaxed = true), nextStep)
        }
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { stepIndex } returns 0
        }
        val routeLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
            every { routeLeg } returns mockRouteLeg
        }
        val routeProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns routeLegProgress
        }

        val result = RouteArrowUtils.isArrivalStep(routeProgress)

        assertFalse(result)
    }

    @Test
    fun `obtainArrowPointsFrom returns empty list when on arrival step`() {
        val arriveManeuver = mockk<StepManeuver>(relaxed = true) {
            every { type() } returns StepManeuver.ARRIVE
        }
        val nextStep = mockk<LegStep>(relaxed = true) {
            every { maneuver() } returns arriveManeuver
        }
        val mockRouteLeg = mockk<RouteLeg>(relaxed = true) {
            every { steps() } returns listOf(mockk(relaxed = true), nextStep)
        }
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { stepPoints } returns routeStepPoints
            every { stepIndex } returns 0
        }
        val routeLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
            every { routeLeg } returns mockRouteLeg
        }
        val routeProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `obtainArrowPointsFrom processes arrow when not on arrival step`() {
        val turnManeuver = mockk<StepManeuver>(relaxed = true) {
            every { type() } returns StepManeuver.TURN
        }
        val nextStep = mockk<LegStep>(relaxed = true) {
            every { maneuver() } returns turnManeuver
        }
        val mockRouteLeg = mockk<RouteLeg>(relaxed = true) {
            every { steps() } returns listOf(mockk(relaxed = true), nextStep)
        }
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617),
        )
        val stepProgress = mockk<RouteStepProgress>(relaxed = true) {
            every { stepPoints } returns routeStepPoints
            every { stepIndex } returns 0
        }
        val routeLegProgress = mockk<RouteLegProgress>(relaxed = true) {
            every { currentStepProgress } returns stepProgress
            every { routeLeg } returns mockRouteLeg
        }
        val routeProgress = mockk<RouteProgress>(relaxed = true) {
            every { currentLegProgress } returns routeLegProgress
            every { upcomingStepPoints } returns upcomingPoints
        }

        val result = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)

        // Should process arrow points normally
        assertEquals(4, result.size)
    }

    @OptIn(MapboxExperimental::class)
    @Test
    fun `initializeLayers scales arrow head bitmap by pixelRatio`() {
        val pixelRatio = 2.0f
        val intrinsicWidth = 24
        val intrinsicHeight = 24
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style>(relaxed = true) {
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every { addStyleSource(any(), any()) } returns ExpectedFactory.createNone()
            every { styleSlots } returns listOf()
            every { this@mockk.pixelRatio } returns pixelRatio
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify {
            any<Drawable>().toBitmap(
                (intrinsicWidth * pixelRatio).roundToInt(),
                (intrinsicHeight * pixelRatio).roundToInt(),
                any(),
            )
        }
        verify {
            style.addImage(ARROW_HEAD_ICON, any<Bitmap>())
        }
        verify {
            style.addImage(ARROW_HEAD_ICON_CASING, any<Bitmap>())
        }
    }
}
