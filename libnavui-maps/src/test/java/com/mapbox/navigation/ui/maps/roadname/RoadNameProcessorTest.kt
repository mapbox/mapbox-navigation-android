package com.mapbox.navigation.ui.maps.roadname

import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.ui.maps.roadname.model.RoadLabel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoadNameProcessorTest {

    @Test
    fun `when road has data then return road result`() {
        val road = mockk<Road>() {
            every { name } returns "Central Avenue"
            every { shieldUrl } returns ""
            every { shieldName } returns "101 South"
        }
        val action = RoadNameAction.GetRoadNameLabel(road)
        val expected = RoadLabel(road.name, null, road.shieldName)

        val result = RoadNameProcessor.process(action) as RoadNameResult.RoadNameLabel

        assertEquals(expected.roadName, result.name)
        assertNull(expected.shield)
        assertEquals(expected.shieldName, result.shieldName)
    }
}
