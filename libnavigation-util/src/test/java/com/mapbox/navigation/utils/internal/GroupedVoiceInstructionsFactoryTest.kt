package com.mapbox.navigation.utils.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import junit.framework.TestCase.assertTrue
import org.junit.Test

class GroupedVoiceInstructionsFactoryTest {

    @Test
    fun isInRangeTest() {
        val route = getRoute()

        val result = GroupedVoiceInstructionsFactory.getGroupedAnnouncementRanges(
            route,
            100.0,
        )

        assertTrue(result.isInRange(135))
        assertTrue(result.isInRange(700))
        assertTrue(result.isInRange(1600))
    }

    private fun getRoute(): DirectionsRoute {
        val routeJson = loadJsonFixture("route-with-close-voice-announcements.json")
        return DirectionsRoute.fromJson(routeJson)
    }

    private fun loadJsonFixture(fileName: String): String {
        return javaClass.classLoader?.getResourceAsStream(fileName)
            ?.bufferedReader()
            ?.use { it.readText() }!!
    }
}
