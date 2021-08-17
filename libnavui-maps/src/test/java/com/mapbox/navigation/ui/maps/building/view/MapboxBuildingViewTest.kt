package com.mapbox.navigation.ui.maps.building.view

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.None
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.Style
import com.mapbox.navigation.testing.FileUtils
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MapboxBuildingViewTest {

    @Test
    fun `should add style layer when building is selected`() {
        val buildings = listOf<QueriedFeature>(
            mockk {
                every { source } returns "composite"
                every { sourceLayer } returns "building"
                every { feature } returns loadFeature()
            }
        )
        val expected: Expected<String, None> = mockk {
            every { error } returns null
        }
        val style: Style = mockk {
            every { styleLayerExists(any()) } returns false
            every { addStyleLayer(any(), any()) } returns expected
            every { removeStyleLayer(any()) } returns mockk()
        }
        val propertySlot = CapturingSlot<Value>()
        val buildingView = MapboxBuildingView()

        buildingView.highlightBuilding(style, buildings)

        verify { style.addStyleLayer(capture(propertySlot), any()) }
    }

    private fun loadFeature() = Feature.fromJson(
        FileUtils.loadJsonFixture("arrival-highlight-building-feature.json")
    )
}
