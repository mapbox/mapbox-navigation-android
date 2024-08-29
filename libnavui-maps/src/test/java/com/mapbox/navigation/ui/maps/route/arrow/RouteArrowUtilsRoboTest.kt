package com.mapbox.navigation.ui.maps.route.arrow

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.maps.Image
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.Style
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RouteArrowUtilsRoboTest {

    private lateinit var ctx: Context

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun initializeLayers() {
        val options = RouteArrowOptions.Builder(ctx).build()
        val shaftSourceValueSlots = mutableListOf<Value>()
        val headSourceValueSlots = mutableListOf<Value>()
        val addStyleLayerSlots = mutableListOf<Value>()
        val addStyleLayerPositionSlots = mutableListOf<LayerPosition>()
        val mockImage = mockk<Image>(relaxed = true)

        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(RouteLayerConstants.ARROW_SHAFT_SOURCE_ID) } returns false
            every { styleSourceExists(RouteLayerConstants.ARROW_HEAD_SOURCE_ID) } returns false
            every {
                styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID) } returns true
            every { styleLayerExists(options.aboveLayerId) } returns true
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteLayerConstants.ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteLayerConstants.ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING) } returns mockImage
            every { getStyleImage(RouteLayerConstants.ARROW_HEAD_ICON) } returns mockImage
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                RouteLayerConstants.ARROW_SHAFT_SOURCE_ID,
                capture(shaftSourceValueSlots),
            )
        }
        assertEquals(
            "geojson",
            (shaftSourceValueSlots.last().contents as HashMap<String, Value>)["type"]!!.contents,
        )
        assertEquals(
            16L,
            (shaftSourceValueSlots.last().contents as HashMap<String, Value>)
            ["maxzoom"]!!.contents,
        )
        assertEquals(
            "",
            (shaftSourceValueSlots.last().contents as HashMap<String, Value>)["data"]!!.contents,
        )
        assertEquals(
            RouteLayerConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (shaftSourceValueSlots.last().contents as HashMap<String, Value>)
            ["tolerance"]!!.contents,
        )

        verify {
            style.addStyleSource(
                RouteLayerConstants.ARROW_HEAD_SOURCE_ID,
                capture(headSourceValueSlots),
            )
        }
        assertEquals(
            "geojson",
            (headSourceValueSlots.last().contents as HashMap<String, Value>)["type"]!!.contents,
        )
        assertEquals(
            16L,
            (headSourceValueSlots.last().contents as HashMap<String, Value>)["maxzoom"]!!.contents,
        )
        assertEquals(
            "",
            (headSourceValueSlots.last().contents as HashMap<String, Value>)["data"]!!
                .contents.toString(),
        )
        assertEquals(
            RouteLayerConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (headSourceValueSlots.last().contents as HashMap<String, Value>)
            ["tolerance"]!!.contents,
        )

        verify { style.removeStyleImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING) }
        verify { style.addImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING, any<Bitmap>()) }

        verify { style.removeStyleImage(RouteLayerConstants.ARROW_HEAD_ICON) }
        verify { style.addImage(RouteLayerConstants.ARROW_HEAD_ICON, any<Bitmap>()) }

        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_LAYER_ID) }

        verify {
            style.addPersistentStyleLayer(
                capture(addStyleLayerSlots),
                capture(addStyleLayerPositionSlots),
            )
        }
        assertEquals(
            "mapbox-navigation-arrow-shaft-casing-layer",
            (addStyleLayerSlots[0].contents as HashMap<String, Value>)["id"]!!.contents,
        )
        assertEquals(
            "mapbox-navigation-arrow-head-casing-layer",
            (addStyleLayerSlots[1].contents as HashMap<String, Value>)["id"]!!.contents,
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-layer",
            (addStyleLayerSlots[2].contents as HashMap<String, Value>)["id"]!!.contents,
        )
        assertEquals(
            "mapbox-navigation-arrow-head-layer",
            (addStyleLayerSlots[3].contents as HashMap<String, Value>)["id"]!!.contents,
        )
        assertEquals(
            "mapbox-top-level-route-layer",
            addStyleLayerPositionSlots[0].above,
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-casing-layer",
            addStyleLayerPositionSlots[1].above,
        )
        assertEquals(
            "mapbox-navigation-arrow-head-casing-layer",
            addStyleLayerPositionSlots[2].above,
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-layer",
            addStyleLayerPositionSlots[3].above,
        )
    }

    @Test
    fun initializeLayers_whenCustomAboveLayerConfigured() {
        val options = RouteArrowOptions.Builder(ctx).withAboveLayerId("foobar").build()
        val shaftSourceValueSlots = mutableListOf<Value>()
        val addStyleLayerSlots = mutableListOf<Value>()
        val addStyleLayerPositionSlots = mutableListOf<LayerPosition>()
        val mockImage = mockk<Image>(relaxed = true)

        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(RouteLayerConstants.ARROW_SHAFT_SOURCE_ID) } returns false
            every { styleSourceExists(RouteLayerConstants.ARROW_HEAD_SOURCE_ID) } returns false
            every {
                styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID) } returns true
            every {
                styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAFFIC)
            } returns true
            every { styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_RESTRICTED) } returns true
            every { styleLayerExists(options.aboveLayerId) } returns true
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteLayerConstants.ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteLayerConstants.ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING) } returns mockImage
            every { getStyleImage(RouteLayerConstants.ARROW_HEAD_ICON) } returns mockImage
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                RouteLayerConstants.ARROW_SHAFT_SOURCE_ID,
                capture(shaftSourceValueSlots),
            )
        }
        verify {
            style.addPersistentStyleLayer(
                capture(addStyleLayerSlots),
                capture(addStyleLayerPositionSlots),
            )
        }
        assertEquals(
            "foobar",
            addStyleLayerPositionSlots[0].above,
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-casing-layer",
            addStyleLayerPositionSlots[1].above,
        )
        assertEquals(
            "mapbox-navigation-arrow-head-casing-layer",
            addStyleLayerPositionSlots[2].above,
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-layer",
            addStyleLayerPositionSlots[3].above,
        )
    }

    @Test
    fun initializeLayers_whenAboveLayerNotExists() {
        val mockImage = mockk<Image>(relaxed = true)
        val options = RouteArrowOptions.Builder(ctx).build()
        val shaftSourceValueSlots = mutableListOf<Value>()
        val headSourceValueSlots = mutableListOf<Value>()
        val addStyleLayerSlots = mutableListOf<Value>()
        val addStyleLayerPositionSlots = mutableListOf<LayerPosition>()
        val style = mockk<Style>(relaxed = true) {
            every { styleLayers } returns listOf()
            every { styleSourceExists(RouteLayerConstants.ARROW_SHAFT_SOURCE_ID) } returns false
            every { styleSourceExists(RouteLayerConstants.ARROW_HEAD_SOURCE_ID) } returns false
            every {
                styleLayerExists(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID)
            } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) } returns true
            every { styleLayerExists(RouteLayerConstants.ARROW_HEAD_LAYER_ID) } returns true
            every { styleLayerExists(options.aboveLayerId) } returns false
            every { addPersistentStyleLayer(any(), any()) } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteLayerConstants.ARROW_SHAFT_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every {
                addStyleSource(RouteLayerConstants.ARROW_HEAD_SOURCE_ID, any())
            } returns ExpectedFactory.createNone()
            every { getStyleImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING) } returns mockImage
            every { getStyleImage(RouteLayerConstants.ARROW_HEAD_ICON) } returns mockImage
        }

        RouteArrowUtils.initializeLayers(style, options)

        verify {
            style.addStyleSource(
                RouteLayerConstants.ARROW_SHAFT_SOURCE_ID,
                capture(shaftSourceValueSlots),
            )
        }
        assertEquals(
            "geojson",
            (shaftSourceValueSlots.last().contents as HashMap<String, Value>)["type"]!!.contents,
        )
        assertEquals(
            16L,
            (shaftSourceValueSlots.last().contents as HashMap<String, Value>)["maxzoom"]!!.contents,
        )
        assertEquals(
            "",
            (shaftSourceValueSlots.last().contents as HashMap<String, Value>)["data"]!!.contents,
        )
        assertEquals(
            RouteLayerConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (shaftSourceValueSlots.last().contents as HashMap<String, Value>)
            ["tolerance"]!!.contents,
        )

        verify {
            style.addStyleSource(
                RouteLayerConstants.ARROW_HEAD_SOURCE_ID,
                capture(headSourceValueSlots),
            )
        }
        assertEquals(
            "geojson",
            (headSourceValueSlots.last().contents as HashMap<String, Value>)["type"]!!.contents,
        )
        assertEquals(
            16L,
            (headSourceValueSlots.last().contents as HashMap<String, Value>)["maxzoom"]!!.contents,
        )
        assertEquals(
            "",
            (headSourceValueSlots.last().contents as HashMap<String, Value>)["data"]!!.contents,
        )
        assertEquals(
            RouteLayerConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            (headSourceValueSlots.last().contents as HashMap<String, Value>)
            ["tolerance"]!!.contents,
        )

        verify { style.removeStyleImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING) }
        verify { style.addImage(RouteLayerConstants.ARROW_HEAD_ICON_CASING, any<Bitmap>()) }

        verify { style.removeStyleImage(RouteLayerConstants.ARROW_HEAD_ICON) }
        verify { style.addImage(RouteLayerConstants.ARROW_HEAD_ICON, any<Bitmap>()) }

        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID) }
        verify { style.removeStyleLayer(RouteLayerConstants.ARROW_HEAD_LAYER_ID) }

        verify {
            style.addPersistentStyleLayer(
                capture(addStyleLayerSlots),
                capture(addStyleLayerPositionSlots),
            )
        }
        assertEquals(
            "mapbox-navigation-arrow-shaft-casing-layer",
            (addStyleLayerSlots[0].contents as HashMap<String, Value>)["id"]!!.contents,
        )
        assertEquals(
            "mapbox-navigation-arrow-head-casing-layer",
            (addStyleLayerSlots[1].contents as HashMap<String, Value>)["id"]!!.contents,
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-layer",
            (addStyleLayerSlots[2].contents as HashMap<String, Value>)["id"]!!.contents,
        )
        assertEquals(
            "mapbox-navigation-arrow-head-layer",
            (addStyleLayerSlots[3].contents as HashMap<String, Value>)["id"]!!.contents,
        )
        assertEquals(
            null,
            addStyleLayerPositionSlots[0].above,
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-casing-layer",
            addStyleLayerPositionSlots[1].above,
        )
        assertEquals(
            "mapbox-navigation-arrow-head-casing-layer",
            addStyleLayerPositionSlots[2].above,
        )
        assertEquals(
            "mapbox-navigation-arrow-shaft-layer",
            addStyleLayerPositionSlots[3].above,
        )
    }
}
