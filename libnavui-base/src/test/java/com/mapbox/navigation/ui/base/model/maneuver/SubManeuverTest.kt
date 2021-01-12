package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class SubManeuverTest : BuilderTest<SubManeuver, SubManeuver.Builder>() {

    override fun getImplementationClass(): KClass<SubManeuver> =
        SubManeuver::class

    override fun getFilledUpBuilder(): SubManeuver.Builder {
        return SubManeuver.Builder()
            .degrees(11.0)
            .text("Street")
            .type("turn")
            .modifier(ManeuverModifier.SHARP_LEFT)
            .drivingSide("left")
            .componentList(
                listOf(
                    Component(
                        BannerComponents.DELIMITER,
                        DelimiterComponentNode.Builder()
                            .text("/")
                            .build()
                    )
                )
            )
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
