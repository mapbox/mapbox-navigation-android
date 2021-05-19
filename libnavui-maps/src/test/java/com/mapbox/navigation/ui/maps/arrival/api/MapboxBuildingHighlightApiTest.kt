package com.mapbox.navigation.ui.maps.arrival.api

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.None
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.QueryFeaturesCallback
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.navigation.testing.FileUtils
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Test

class MapboxBuildingHighlightApiTest {

    /** Mock map styles **/
    private val expected: Expected<String, None> = mockk {
        every { error } returns null
    }
    private val style: Style = mockk {
        every { styleLayerExists(any()) } returns false
        every { addStyleLayer(any(), any()) } returns expected
        every { removeStyleLayer(any()) } returns mockk()
    }

    /** Mock querying features **/
    private val queriedFeaturesExpected: Expected<String, List<QueriedFeature>> = mockk {
        every { value } returns emptyList()
        every { error } returns null
    }
    private val onStyleLoadedSlot = CapturingSlot<Style.OnStyleLoaded>()
    private val mapboxMap: MapboxMap = mockk {
        every { queryRenderedFeatures(any<ScreenCoordinate>(), any(), any()) } answers {
            thirdArg<QueryFeaturesCallback>().run(queriedFeaturesExpected)
        }
        every { getStyle(capture(onStyleLoadedSlot)) } answers {
            // No answer
        }
    }

    private val buildingHighlightApi = MapboxBuildingHighlightApi(mapboxMap)

    @Test
    fun `highlight null point is a no-op`() {
        buildingHighlightApi.highlightBuilding(null)
    }

    @Test
    fun `should add style layer when building is selected`() {
        every { mapboxMap.pixelForCoordinate(any()) } returns mockk {
            every { x } returns 134.0
            every { y } returns 160.0
        }
        every { mapboxMap.queryRenderedFeatures(any<ScreenCoordinate>(), any(), any()) } answers {
            thirdArg<QueryFeaturesCallback>().run(mockSuccessQueriedFeature())
        }

        val testPoint = Point.fromLngLat(-122.431969, 37.777663)
        buildingHighlightApi.highlightBuilding(testPoint)
        onStyleLoadedSlot.captured.onStyleLoaded(style)

        val propertySlot = CapturingSlot<Value>()
        verify { style.addStyleLayer(capture(propertySlot), any()) }
        assertTrue(propertySlot.isCaptured)
    }

    private fun mockSuccessQueriedFeature() = mockk<Expected<String, List<QueriedFeature>>> {
        mockk {
            every { value } returns listOf(
                mockk {
                    every { source } returns "composite"
                    every { sourceLayer } returns "building"
                    every { feature } returns loadFeature()
                }
            )
            every { error } returns null
        }
    }

    private fun loadFeature() = Feature.fromJson(
        FileUtils.loadJsonFixture("arrival-highlight-building-feature.json")
    )
}
