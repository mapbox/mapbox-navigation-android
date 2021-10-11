package com.mapbox.navigation.ui.maps.roadname.api

import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.ui.maps.roadname.RoadNameAction
import com.mapbox.navigation.ui.maps.roadname.RoadNameProcessor
import com.mapbox.navigation.ui.maps.roadname.RoadNameResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MapboxRoadNameLabelApiTest {

    @Before
    fun setUp() {
        mockkObject(RoadNameProcessor)
    }

    @After
    fun tearDown() {
        unmockkObject(RoadNameProcessor)
    }

    @Test
    fun `when function invoked return a road label`() {
        val road = mockk<Road> {
            every { name } returns "Central Avenue"
            every { shieldUrl } returns ""
            every { shieldName } returns "101 South"
        }
        val action = RoadNameAction.GetRoadNameLabel(road)
        every {
            RoadNameProcessor.process(action)
        } returns RoadNameResult.RoadNameLabel(road.name, null, road.shieldName)
        val roadNameApi = MapboxRoadNameLabelApi()

        val expected = roadNameApi.getRoadNameLabel(road)

        assertEquals(expected.roadName, road.name)
        assertNull(expected.shield)
        assertEquals(expected.shieldName, road.shieldName)
    }
}
