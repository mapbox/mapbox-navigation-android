package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

class SubManeuverTest : BuilderTest<SubManeuver, SubManeuver.Builder>() {

    override fun getImplementationClass(): KClass<SubManeuver> =
        SubManeuver::class

    override fun getFilledUpBuilder(): SubManeuver.Builder {
        return SubManeuver.Builder()
            .id("1a2s3d4f5g")
            .degrees(11.0)
            .text("Street")
            .type("turn")
            .modifier(ManeuverModifier.SHARP_LEFT)
            .drivingSide("left")
            .componentList(listOf(mockk()))
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
