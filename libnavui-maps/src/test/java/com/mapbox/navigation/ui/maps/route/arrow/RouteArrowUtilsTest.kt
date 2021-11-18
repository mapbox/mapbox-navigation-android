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
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_ICON
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_ICON_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_HEAD_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_SHAFT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID
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

@OptIn(MapboxExperimental::class)
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
        val mockImage = mockk<Image>(relaxed = true)

        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(ARROW_SHAFT_SOURCE_ID) } returns false
            every { styleSourceExists(ARROW_HEAD_SOURCE_ID) } returns false
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
            every { getStyleImage(ARROW_HEAD_ICON_CASING) } returns mockImage
            every { getStyleImage(ARROW_HEAD_ICON) } returns mockImage
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                ARROW_SHAFT_SOURCE_ID,
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
            "",
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]!!.contents
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(ARROW_HEAD_SOURCE_ID, capture(headSourceValueSlot))
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
            "",
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]!!
                .contents.toString()
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify { style.removeStyleImage(ARROW_HEAD_ICON_CASING) }
        verify { style.addImage(ARROW_HEAD_ICON_CASING, any<Bitmap>()) }

        verify { style.removeStyleImage(ARROW_HEAD_ICON) }
        verify { style.addImage(ARROW_HEAD_ICON, any<Bitmap>()) }

        verify { style.removeStyleLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(ARROW_HEAD_CASING_LAYER_ID) }
        verify { style.removeStyleLayer(ARROW_SHAFT_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(ARROW_HEAD_LAYER_ID) }

        verify {
            style.addPersistentStyleLayer(
                capture(addStyleLayerSlots),
                capture(addStyleLayerPositionSlots)
            )
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
            "mapbox-top-level-route-layer",
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
    fun initializeLayers_whenCustomAboveLayerConfigured() {
        val options = RouteArrowOptions.Builder(ctx).withAboveLayerId("foobar").build()
        val shaftSourceValueSlot = slot<Value>()
        val addStyleLayerSlots = mutableListOf<Value>()
        val addStyleLayerPositionSlots = mutableListOf<LayerPosition>()
        val mockImage = mockk<Image>(relaxed = true)

        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(ARROW_SHAFT_SOURCE_ID) } returns false
            every { styleSourceExists(ARROW_HEAD_SOURCE_ID) } returns false
            every {
                styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_HEAD_LAYER_ID) } returns true
            every { styleLayerExists(PRIMARY_ROUTE_TRAFFIC_LAYER_ID) } returns true
            every { styleLayerExists(RESTRICTED_ROAD_LAYER_ID) } returns true
            every { styleLayerExists(options.aboveLayerId) } returns true
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(ARROW_HEAD_ICON_CASING) } returns mockImage
            every { getStyleImage(ARROW_HEAD_ICON) } returns mockImage
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                ARROW_SHAFT_SOURCE_ID,
                capture(shaftSourceValueSlot)
            )
        }
        verify {
            style.addPersistentStyleLayer(
                capture(addStyleLayerSlots),
                capture(addStyleLayerPositionSlots)
            )
        }
        assertEquals(
            "foobar",
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
        val mockImage = mockk<Image>(relaxed = true)
        val options = RouteArrowOptions.Builder(ctx).build()
        val shaftSourceValueSlot = slot<Value>()
        val headSourceValueSlot = slot<Value>()
        val addStyleLayerSlots = mutableListOf<Value>()
        val addStyleLayerPositionSlots = mutableListOf<LayerPosition>()
        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(ARROW_SHAFT_SOURCE_ID) } returns false
            every { styleSourceExists(ARROW_HEAD_SOURCE_ID) } returns false
            every {
                styleLayerExists(ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(ARROW_HEAD_LAYER_ID) } returns true
            every { styleLayerExists(options.aboveLayerId) } returns false
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(ARROW_HEAD_ICON_CASING) } returns mockImage
            every { getStyleImage(ARROW_HEAD_ICON) } returns mockImage
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                ARROW_SHAFT_SOURCE_ID,
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
            "",
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]!!.contents
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (shaftSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify {
            style.addStyleSource(ARROW_HEAD_SOURCE_ID, capture(headSourceValueSlot))
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
            "",
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)["data"]!!.contents
        )
        assertEquals(
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (headSourceValueSlot.captured.contents as HashMap<String, Value>)
            ["tolerance"]!!.contents
        )

        verify { style.removeStyleImage(ARROW_HEAD_ICON_CASING) }
        verify { style.addImage(ARROW_HEAD_ICON_CASING, any<Bitmap>()) }

        verify { style.removeStyleImage(ARROW_HEAD_ICON) }
        verify { style.addImage(ARROW_HEAD_ICON, any<Bitmap>()) }

        verify { style.removeStyleLayer(ARROW_SHAFT_CASING_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(ARROW_HEAD_CASING_LAYER_ID) }
        verify { style.removeStyleLayer(ARROW_SHAFT_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(ARROW_HEAD_LAYER_ID) }

        verify {
            style.addPersistentStyleLayer(
                capture(addStyleLayerSlots),
                capture(addStyleLayerPositionSlots)
            )
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
}
