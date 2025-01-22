package com.mapbox.navigation.tripdata.maneuver.api

import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.tripdata.R
import com.mapbox.navigation.tripdata.maneuver.model.TurnIconResources
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxTurnIconsApiTest {

    @Test
    fun turnIcon() {
        val turnIconResources = TurnIconResources.Builder().build()
        val type: String? = null
        val degrees: Float? = null
        val modifier: String = ManeuverModifier.LEFT
        val drivingSide: String? = null
        val expected = ManeuverTurnIcon(
            degrees,
            drivingSide,
            false,
            R.drawable.mapbox_ic_turn_left,
        )

        val result = MapboxTurnIconsApi(turnIconResources).generateTurnIcon(
            type,
            degrees,
            modifier,
            drivingSide,
        ).value

        assertEquals(expected, result)
    }

    @Test
    fun updateResources() {
        val turnIconResources = TurnIconResources.Builder().build()
        val api = MapboxTurnIconsApi(turnIconResources)
        val type: String? = null
        val degrees: Float? = null
        val modifier: String = ManeuverModifier.LEFT
        val drivingSide: String? = null
        val defaultExpected = ManeuverTurnIcon(
            degrees,
            drivingSide,
            false,
            R.drawable.mapbox_ic_turn_left,
        )
        val updatedExpected = ManeuverTurnIcon(
            degrees,
            drivingSide,
            false,
            R.drawable.mapbox_ic_turn_right,
        )
        val updatedResources =
            turnIconResources.toBuilder().turnIconTurnLeft(R.drawable.mapbox_ic_turn_right).build()
        val defaultResult = api.generateTurnIcon(
            type,
            degrees,
            modifier,
            drivingSide,
        ).value

        api.updateResources(updatedResources)

        val updatedResult = api.generateTurnIcon(
            type,
            degrees,
            modifier,
            drivingSide,
        ).value

        assertEquals(defaultExpected, defaultResult)
        assertEquals(updatedExpected, updatedResult)
    }
}
