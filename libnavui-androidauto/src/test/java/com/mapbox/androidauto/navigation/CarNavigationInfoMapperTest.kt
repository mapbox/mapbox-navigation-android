package com.mapbox.androidauto.navigation

import android.content.Context
import android.util.Log
import com.mapbox.androidauto.navigation.lanes.CarLanesImageRenderer
import com.mapbox.androidauto.navigation.maneuver.CarManeuverIconRenderer
import com.mapbox.androidauto.navigation.maneuver.CarManeuverInstructionRenderer
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.shield.model.RouteShield
import io.mockk.every
import io.mockk.mockk

import org.junit.After
import org.junit.Before
import org.junit.Test

class CarNavigationInfoMapperTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun mapNavigationInfo() {
        // dependencies
        val context = mockk<Context>(relaxed = true)
        val carManeuverInstructionRenderer = mockk<CarManeuverInstructionRenderer>()
        val carManeuverIconRenderer = mockk<CarManeuverIconRenderer>()
        val carLanesImageGenerator = mockk<CarLanesImageRenderer>()

        // create instance of class
        val mapper = CarNavigationInfoMapper(
            context,
            carManeuverInstructionRenderer,
            carManeuverIconRenderer,
            carLanesImageGenerator,
        )

        // input
        val expectedManeuvers = mockk<Expected<ManeuverError, List<Maneuver>>>()
        val routeShields = mockk<List<RouteShield>>()
        val routeProgress = mockk<RouteProgress>()

        // mock some input behavior

        // output
        val output = mapper.mapNavigationInfo(expectedManeuvers, routeShields, routeProgress)

        // assert some behavior
    }
}
