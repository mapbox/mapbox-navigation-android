package com.mapbox.navigation.instrumentation_tests.core

import android.content.Context
import android.location.Location
import androidx.annotation.IntegerRes
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.ui.maneuver.api.MapboxLaneIconsApi
import com.mapbox.navigation.ui.maneuver.model.LaneIconResources
import com.mapbox.navigation.ui.maneuver.model.LaneIndicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanesTest : BaseCoreNoCleanUpTest() {

    @Test
    fun testUnknownIndicationTypes() {
        val route = getRoute(context, R.raw.route_with_unknown_lane_types)
        val lanes = route.legs()?.first()?.steps()?.first()?.intersections()?.first()?.lanes()

        assertNotNull(lanes)
        requireNotNull(lanes)

        assertTrue(lanes.isNotEmpty())
        assertEquals(listOf("left", "straight", "unknown"), lanes.first().indications())
        assertEquals(listOf("right", "unknown"), lanes.last().indications())
        assertEquals("unknown", lanes.last().validIndication())
    }

    @Test
    fun testUnknownLaneTypesRendering() {
        val laneIconRes = LaneIconResources.Builder().build()
        val laneApi = MapboxLaneIconsApi(laneIconRes)

        val inactiveIndicator = LaneIndicator
            .Builder()
            .drivingSide("right")
            .isActive(false)
            .directions(listOf("unknown"))
            .activeDirection(null)
            .build()

        val inactiveTurnLane = laneApi.getTurnLane(inactiveIndicator)
        assertEquals(laneIconRes.laneStraight, inactiveTurnLane.drawableResId)

        val activeIndicator = LaneIndicator
            .Builder()
            .drivingSide("right")
            .isActive(true)
            .directions(listOf("unknown", "right"))
            .activeDirection("unknown")
            .build()

        val activeTurnLane = laneApi.getTurnLane(activeIndicator)
        assertEquals(laneIconRes.laneStraight, activeTurnLane.drawableResId)
    }

    private fun getRoute(
        context: Context,
        @IntegerRes routeFileResource: Int
    ): DirectionsRoute {
        val routeAsString = readRawFileText(context, routeFileResource)
        return DirectionsRoute.fromJson(routeAsString)
    }

    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            // no op
        }
    }
}
