package com.mapbox.navigation.ui.maps.route.arrow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Point
import com.mapbox.maps.Image
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.common.ShadowValueConverter
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowValueConverter::class])
@RunWith(RobolectricTestRunner::class)
class RouteArrowUtilsTest {

    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun obtainArrowPointsFromWhenCurrentArrowLessThan2Points() {
        val upcomingPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
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
            Point.fromLngLat(-122.4784726, 37.8587617)
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
            Point.fromLngLat(-122.4784726, 37.8587617)
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
            Point.fromLngLat(-122.4784726, 37.8587617)
        )
        val routeStepPoints = listOf(
            Point.fromLngLat(-122.477395, 37.859513),
            Point.fromLngLat(-122.4784726, 37.8587617)
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
            every { fullyLoaded } returns true
            every { styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID) } returns true
        }

        val result = RouteArrowUtils.layersAreInitialized(style)

        assertTrue(result)
        verify { style.fullyLoaded }
        verify { style.styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) }
        verify { style.styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) }
        verify { style.styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID) }
    }

    @Test
    fun initializeLayers_whenStyleNotLoaded() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { fullyLoaded } returns false
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleSourceExists(any()) }
    }

    @Test
    fun initializeLayers_whenLayersAreInitialized() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val style = mockk<Style> {
            every { styleLayers } returns listOf()
            every { fullyLoaded } returns true
            every { styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns true
            every { styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID) } returns true
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify(exactly = 0) { style.styleLayers }
        verify(exactly = 0) { style.addStyleSource(any(), any()) }
    }

    @Test
    fun initializeLayers() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val shaftSourceValueSlot = slot<Value>()
        val headSourceValueSlot = slot<Value>()
        val addStyleLayerSlots = mutableListOf<Value>()
        val addStyleLayerPositionSlots = mutableListOf<LayerPosition>()
        val style = getFullMockedStyle()

        RouteArrowUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                RouteConstants.ARROW_SHAFT_SOURCE_ID,
                capture(shaftSourceValueSlot)
            )
        }
        assertEquals(
            "geojson",
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)["type"]!!.contents
        )
        assertEquals(
            16L,
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)["maxzoom"]!!.contents
        )
        assertEquals(
            "null",
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]!!.contents
        )
        assertEquals(
            RouteConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(RouteConstants.ARROW_HEAD_SOURCE_ID, capture(headSourceValueSlot))
        }
        assertEquals(
            "geojson",
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)["type"]!!.contents
        )
        assertEquals(
            16L,
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)["maxzoom"]!!.contents
        )
        assertEquals(
            "null",
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]!!.contents
        )
        assertEquals(
            RouteConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify { style.removeStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING) }
        verify { style.addImage(RouteConstants.ARROW_HEAD_ICON_CASING, any<Bitmap>()) }

        verify { style.removeStyleImage(RouteConstants.ARROW_HEAD_ICON) }
        verify { style.addImage(RouteConstants.ARROW_HEAD_ICON, any<Bitmap>()) }

        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_LAYER_ID) }

        verify {
            style.addStyleLayer(capture(addStyleLayerSlots), capture(addStyleLayerPositionSlots))
        }
        assertEquals(
            "mapbox-navigation-arrow-shaft-casing-layer",
            (addStyleLayerSlots[0].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-arrow-head-casing-layer",
            (addStyleLayerSlots[1].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-layer",
            (addStyleLayerSlots[2].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-arrow-head-layer",
            (addStyleLayerSlots[3].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-route-traffic-layer",
            addStyleLayerPositionSlots[0].above
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-casing-layer",
            addStyleLayerPositionSlots[1].above
        )
        assertEquals(
            "mapbox-navigation-arrow-head-casing-layer",
            addStyleLayerPositionSlots[2].above
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-layer",
            addStyleLayerPositionSlots[3].above
        )
    }

    @Test
    fun initializeLayers_whenAboveLayerNotExists() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val shaftSourceValueSlot = slot<Value>()
        val headSourceValueSlot = slot<Value>()
        val addStyleLayerSlots = mutableListOf<Value>()
        val addStyleLayerPositionSlots = mutableListOf<LayerPosition>()
        val style = getFullMockedStyle()
        every {
            style.styleLayerExists("mapbox-navigation-route-traffic-layer")
        } returns false

        RouteArrowUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                RouteConstants.ARROW_SHAFT_SOURCE_ID,
                capture(shaftSourceValueSlot)
            )
        }
        assertEquals(
            "geojson",
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)["type"]!!.contents
        )
        assertEquals(
            16L,
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)["maxzoom"]!!.contents
        )
        assertEquals(
            "null",
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]!!.contents
        )
        assertEquals(
            RouteConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(RouteConstants.ARROW_HEAD_SOURCE_ID, capture(headSourceValueSlot))
        }
        assertEquals(
            "geojson",
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)["type"]!!.contents
        )
        assertEquals(
            16L,
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)["maxzoom"]!!.contents
        )
        assertEquals(
            "null",
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]!!.contents
        )
        assertEquals(
            RouteConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify { style.removeStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING) }
        verify { style.addImage(RouteConstants.ARROW_HEAD_ICON_CASING, any<Bitmap>()) }

        verify { style.removeStyleImage(RouteConstants.ARROW_HEAD_ICON) }
        verify { style.addImage(RouteConstants.ARROW_HEAD_ICON, any<Bitmap>()) }

        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_LAYER_ID) }

        verify {
            style.addStyleLayer(capture(addStyleLayerSlots), capture(addStyleLayerPositionSlots))
        }
        assertEquals(
            "mapbox-navigation-arrow-shaft-casing-layer",
            (addStyleLayerSlots[0].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-arrow-head-casing-layer",
            (addStyleLayerSlots[1].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-layer",
            (addStyleLayerSlots[2].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            "mapbox-navigation-arrow-head-layer",
            (addStyleLayerSlots[3].contents as HashMap<String, Value>)["id"]!!.contents
        )
        assertEquals(
            null,
            addStyleLayerPositionSlots[0].above
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-casing-layer",
            addStyleLayerPositionSlots[1].above
        )
        assertEquals(
            "mapbox-navigation-arrow-head-casing-layer",
            addStyleLayerPositionSlots[2].above
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-layer",
            addStyleLayerPositionSlots[3].above
        )
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
        }
        val style = getFullMockedStyle()

        RouteArrowUtils.initializeLayers(style, mockOptions)

        verify(exactly = 0) { style.addImage(RouteConstants.ARROW_HEAD_ICON, any<Bitmap>()) }
        verify(exactly = 0) { style.addImage(RouteConstants.ARROW_HEAD_ICON, any<Image>()) }
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
        }
        val style = getFullMockedStyle()

        RouteArrowUtils.initializeLayers(style, mockOptions)

        verify(exactly = 0) { style.addImage(RouteConstants.ARROW_HEAD_ICON, any<Bitmap>()) }
        verify(exactly = 0) { style.addImage(RouteConstants.ARROW_HEAD_ICON, any<Image>()) }
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
        }
        val style = getFullMockedStyle()

        RouteArrowUtils.initializeLayers(style, mockOptions)

        verify(exactly = 0) { style.addImage(RouteConstants.ARROW_HEAD_ICON_CASING, any<Bitmap>()) }
        verify(exactly = 0) { style.addImage(RouteConstants.ARROW_HEAD_ICON_CASING, any<Image>()) }
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
        }
        val style = getFullMockedStyle()

        RouteArrowUtils.initializeLayers(style, mockOptions)

        verify(exactly = 0) { style.addImage(RouteConstants.ARROW_HEAD_ICON_CASING, any<Bitmap>()) }
        verify(exactly = 0) { style.addImage(RouteConstants.ARROW_HEAD_ICON_CASING, any<Image>()) }
    }

    private fun getFullMockedStyle(): Style {
        return mockk<Style> {
            every { fullyLoaded } returns true
            every { styleLayers } returns listOf()
            every { styleSourceExists(RouteConstants.ARROW_SHAFT_SOURCE_ID) } returns false
            every { styleSourceExists(RouteConstants.ARROW_HEAD_SOURCE_ID) } returns false
            every {
                styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID) } returns true
            every { styleLayerExists("mapbox-navigation-route-traffic-layer") } returns true
            every {
                addStyleSource(RouteConstants.ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteConstants.ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING) } returns mockk()
            every {
                removeStyleImage(RouteConstants.ARROW_HEAD_ICON_CASING)
            } returns ExpectedFactory.createNone()
            every {
                addImage(RouteConstants.ARROW_HEAD_ICON_CASING, any<Bitmap>())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(RouteConstants.ARROW_HEAD_ICON) } returns mockk()
            every {
                removeStyleImage(RouteConstants.ARROW_HEAD_ICON)
            } returns ExpectedFactory.createNone()
            every {
                addImage(RouteConstants.ARROW_HEAD_ICON, any<Bitmap>())
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID)
            } returns ExpectedFactory.createNone()
            every {
                removeStyleLayer(RouteLayerConstants.ARROW_HEAD_LAYER_ID)
            } returns ExpectedFactory.createNone()
            every { addStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
        }
    }
}
