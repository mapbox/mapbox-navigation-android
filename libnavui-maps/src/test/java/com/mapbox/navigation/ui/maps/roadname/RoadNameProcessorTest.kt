package com.mapbox.navigation.ui.maps.roadname

import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.ui.maps.roadname.model.RoadLabel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoadNameProcessorTest {

    @Test
    fun `when road has data then return road result`() {
        val roadComponent = mockk<RoadComponent> {
            every { text } returns "Central Av"
            every { shield } returns null
            every { imageBaseUrl } returns null
        }
        val road = mockk<Road> {
            every { components } returns listOf(roadComponent)
        }
        val action = RoadNameAction.GetRoadNameLabel(road)
        val expected = RoadLabel("Central Av", null, null)

        val result = RoadNameProcessor.process(action) as RoadNameResult.RoadNameLabel

        assertEquals(expected.roadName, result.name)
        assertNull(expected.shield)
        assertEquals(expected.shieldName, result.shieldName)
    }
}
