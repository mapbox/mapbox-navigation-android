package com.mapbox.navigation.ui.maps.internal

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.StyleLoadedCallback
import com.mapbox.maps.StylePropertyValue
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.internal.MapHdRoadsVisibilityController.Companion.BASEMAP_IMPORT_ID
import com.mapbox.navigation.ui.maps.internal.MapHdRoadsVisibilityController.Companion.SHOW_HD_ROADS_CONFIG_KEY
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapHdRoadsVisibilityControllerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val styleLoadedCallbackSlot = slot<StyleLoadedCallback>()

    @Before
    fun setUp() {
        setCoordinationEnabled(false)
    }

    @After
    fun tearDown() {
        setCoordinationEnabled(false)
    }

    @Test
    fun `subscribes to the map style loaded events on creation`() {
        val mapboxMap = mockk<MapboxMap>(relaxed = true)

        MapHdRoadsVisibilityController(mapboxMap)

        verify(exactly = 1) { mapboxMap.subscribeStyleLoaded(any()) }
    }

    @Test
    fun `hides HD roads on style loaded when coordination is not used`() {
        val mapboxMap = givenHdCapableMap(currentlyVisible = true)

        MapHdRoadsVisibilityController(mapboxMap)
        styleLoadedCallbackSlot.captured.run(mockk(relaxed = true))

        verify(exactly = 1) {
            mapboxMap.setStyleImportConfigProperty(
                BASEMAP_IMPORT_ID,
                SHOW_HD_ROADS_CONFIG_KEY,
                Value.valueOf(false),
            )
        }
    }

    @Test
    fun `does not touch the style on style loaded when coordination is used`() {
        setCoordinationEnabled(true)
        val mapboxMap = givenHdCapableMap(currentlyVisible = true)

        MapHdRoadsVisibilityController(mapboxMap)
        styleLoadedCallbackSlot.captured.run(mockk(relaxed = true))

        verify(exactly = 0) { mapboxMap.getStyleImportConfigProperty(any(), any()) }
        verify(exactly = 0) { mapboxMap.setStyleImportConfigProperty(any(), any(), any()) }
    }

    @Test
    fun `does nothing when the style has no showHdRoads import config key`() {
        val mapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
            every {
                getStyleImportConfigProperty(BASEMAP_IMPORT_ID, SHOW_HD_ROADS_CONFIG_KEY)
            } returns ExpectedFactory.createError("import config property not found")
            every {
                subscribeStyleLoaded(capture(styleLoadedCallbackSlot))
            } returns mockk(relaxed = true)
        }

        MapHdRoadsVisibilityController(mapboxMap)
        styleLoadedCallbackSlot.captured.run(mockk(relaxed = true))

        verify(exactly = 0) { mapboxMap.setStyleImportConfigProperty(any(), any(), any()) }
    }

    @Test
    fun `does nothing when the map is destroyed`() {
        val mapboxMap = givenHdCapableMap(currentlyVisible = true)
        every { mapboxMap.isValid() } returns false

        MapHdRoadsVisibilityController(mapboxMap)
        styleLoadedCallbackSlot.captured.run(mockk(relaxed = true))

        verify(exactly = 0) { mapboxMap.setStyleImportConfigProperty(any(), any(), any()) }
    }

    @Test
    fun `cancels the style subscription on destroy`() {
        val cancelable = mockk<Cancelable>(relaxUnitFun = true)
        val mapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns cancelable
        }
        val controller = MapHdRoadsVisibilityController(mapboxMap)

        controller.onDestroy()

        verify(exactly = 1) { cancelable.cancel() }
    }

    @Test
    fun `does nothing when the HD roads are hidden already`() {
        val mapboxMap = givenHdCapableMap(currentlyVisible = false)

        MapHdRoadsVisibilityController(mapboxMap)
        styleLoadedCallbackSlot.captured.run(mockk(relaxed = true))

        verify(exactly = 0) { mapboxMap.setStyleImportConfigProperty(any(), any(), any()) }
    }

    private fun givenHdCapableMap(currentlyVisible: Boolean): MapboxMap {
        val propertyValue = mockk<StylePropertyValue>()
        every { propertyValue.value } returns Value.valueOf(currentlyVisible)
        return mockk<MapboxMap>(relaxed = true) {
            every { isValid() } returns true
            every {
                getStyleImportConfigProperty(BASEMAP_IMPORT_ID, SHOW_HD_ROADS_CONFIG_KEY)
            } returns ExpectedFactory.createValue(propertyValue)
            every {
                setStyleImportConfigProperty(any(), any(), any())
            } returns ExpectedFactory.createNone()
            every {
                subscribeStyleLoaded(capture(styleLoadedCallbackSlot))
            } returns mockk(relaxed = true)
        }
    }
}
