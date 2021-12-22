package com.mapbox.navigation.ui.maps.roadname.api

import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.road.model.RoadComponent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxRoadNameLabelApiTest {

    @Test
    fun `when function invoked return a road label`() {
        val roadComponent = mockk<RoadComponent> {
            every { text } returns "Central Av"
            every { shield } returns null
            every { imageBaseUrl } returns null
        }
        val road = mockk<Road> {
            every { components } returns listOf(roadComponent)
            every { name } returns "Central Av"
            every { shieldName } returns null
            every { shieldUrl } returns null
        }
        val roadNameApi = MapboxRoadNameLabelApi()

        val actual = roadNameApi.getRoadNameLabel(road)

        assertEquals("Central Av", actual.roadName)
    }
}
