package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

class PrimaryManeuverTest : BuilderTest<PrimaryManeuver, PrimaryManeuver.Builder>() {

    override fun getImplementationClass(): KClass<PrimaryManeuver> =
        PrimaryManeuver::class

    override fun getFilledUpBuilder(): PrimaryManeuver.Builder {
        return PrimaryManeuver.Builder()
            .id("1234abcd")
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
