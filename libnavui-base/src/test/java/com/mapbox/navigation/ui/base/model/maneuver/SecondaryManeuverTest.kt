package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class SecondaryManeuverTest : BuilderTest<SecondaryManeuver, SecondaryManeuver.Builder>() {

    override fun getImplementationClass(): KClass<SecondaryManeuver> =
        SecondaryManeuver::class

    override fun getFilledUpBuilder(): SecondaryManeuver.Builder {
        return SecondaryManeuver.Builder()
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
