package com.mapbox.navigation.base.internal.factory

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigator.NavigationStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class RoadFactoryTest {

    @Test
    fun `road object provides name`() {
        val status = createNavigationStatus()

        val road = RoadFactory.buildRoadObject(status)

        Assert.assertEquals("roadName1", road.components[0].text)
        Assert.assertEquals("roadName2", road.components[1].text)
    }

    @Test
    fun `road object provides legacy shield url`() {
        val status = createNavigationStatus()

        val road = RoadFactory.buildRoadObject(status)

        Assert.assertEquals("legacyUrl1", road.components[0].imageBaseUrl)
        Assert.assertEquals("designUrl", road.components[0].shield?.baseUrl())
    }

    @Test
    fun `road object provides shield name`() {
        val status = createNavigationStatus()

        val road = RoadFactory.buildRoadObject(status)

        Assert.assertEquals("shieldName", road.components[0].shield?.name())
    }

    private fun createNavigationStatus(): NavigationStatus = mockk {
        every { roads } returns listOf(
            com.mapbox.navigator.RoadName(
                "roadName1",
                "en",
                "legacyUrl1",
                com.mapbox.navigator.Shield(
                    "designUrl",
                    "displayRef",
                    "shieldName",
                    "color",
                ),
            ),
            com.mapbox.navigator.RoadName(
                "roadName2",
                "en",
                null,
                null,
            ),
        )
    }
}
