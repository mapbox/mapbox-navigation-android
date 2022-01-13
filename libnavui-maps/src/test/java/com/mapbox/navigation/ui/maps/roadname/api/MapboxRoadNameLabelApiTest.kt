package com.mapbox.navigation.ui.maps.roadname.api

import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.road.model.RoadComponent
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
        val roadComponent = mockk<RoadComponent> {
            every { text } returns "Central Av"
            every { shield } returns null
            every { imageBaseUrl } returns null
        }
        val road = mockk<Road> {
            every { components } returns listOf(roadComponent)
        }
        val action = RoadNameAction.GetRoadNameLabel(road)
        every {
            RoadNameProcessor.process(action)
        } returns RoadNameResult.RoadNameLabel("Central Av", null, null)
        val roadNameApi = MapboxRoadNameLabelApi()

        val actual = roadNameApi.getRoadNameLabel(road)

        assertEquals("Central Av", actual.roadName)
        assertNull(actual.shield)
        assertNull(actual.shieldName)
    }
}
