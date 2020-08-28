package com.mapbox.navigation.ui.map

import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.navigation.ui.map.building.BuildingExtrusionHighlightLayer
import com.mapbox.navigation.ui.map.building.BuildingFootprintHighlightLayer
import com.mapbox.navigation.ui.map.building.BuildingLayerSupport
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * This file has basic tests that use [BuildingExtrusionHighlightLayer],
 * [BuildingFootprintHighlightLayer], and [BuildingLayerSupport].
 */
@RunWith(RobolectricTestRunner::class)
class BuildingHighlightTest {

    @Test
    fun buildingFootprintHighlightLayerNotNull() {
        val mapboxMap = mockk<MapboxMap>()
        val buildingFootprintHighlightLayer = BuildingFootprintHighlightLayer(mapboxMap)
        assertNotNull(buildingFootprintHighlightLayer)
    }

    @Test
    fun buildingExtrusionHighlightLayerNotNull() {
        val mapboxMap = mockk<MapboxMap>()
        val buildingExtrusionHighlightLayer = BuildingExtrusionHighlightLayer(mapboxMap)
        assertNotNull(buildingExtrusionHighlightLayer)
    }

    @Test
    fun buildingFootprintHighlightLayerLatLngNull() {
        val mapboxMap = mockk<MapboxMap>()
        val buildingFootprintHighlightLayer = BuildingFootprintHighlightLayer(mapboxMap)
        assertNull(buildingFootprintHighlightLayer.queryLatLng)
    }

    @Test
    fun buildingExtrusionHighlightLayerLatLngNull() {
        val mapboxMap = mockk<MapboxMap>()
        val buildingExtrusionHighlightLayer = BuildingExtrusionHighlightLayer(mapboxMap)
        assertNull(buildingExtrusionHighlightLayer.queryLatLng)
    }

    @Test
    fun buildingFootprintHighlightLayerDefaultColor() {
        val mapboxMap = mockk<MapboxMap>()
        val buildingFootprintHighlightLayer = BuildingFootprintHighlightLayer(mapboxMap)
        assertEquals(
            BuildingLayerSupport.DEFAULT_HIGHLIGHT_COLOR,
            buildingFootprintHighlightLayer.color
        )
    }

    @Test
    fun buildingExtrusionHighlightLayerDefaultColor() {
        val mapboxMap = mockk<MapboxMap>()
        val buildingExtrusionHighlightLayer = BuildingExtrusionHighlightLayer(mapboxMap)
        assertEquals(
            BuildingLayerSupport.DEFAULT_HIGHLIGHT_COLOR,
            buildingExtrusionHighlightLayer.color
        )
    }

    @Test
    fun buildingFootprintHighlightLayerDefaultOpacity() {
        val mapboxMap = mockk<MapboxMap>()
        val buildingFootprintHighlightLayer = BuildingFootprintHighlightLayer(mapboxMap)
        assertEquals(
            BuildingLayerSupport.DEFAULT_HIGHLIGHT_OPACITY,
            buildingFootprintHighlightLayer.opacity
        )
    }

    @Test
    fun buildingExtrusionHighlightLayerDefaultOpacity() {
        val mapboxMap = mockk<MapboxMap>()
        val buildingExtrusionHighlightLayer = BuildingExtrusionHighlightLayer(mapboxMap)
        assertEquals(
            BuildingLayerSupport.DEFAULT_HIGHLIGHT_OPACITY,
            buildingExtrusionHighlightLayer.opacity
        )
    }
}
