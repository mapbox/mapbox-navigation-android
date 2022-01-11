package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class ManeuverOptionsTest : BuilderTest<ManeuverOptions, ManeuverOptions.Builder>() {

    override fun getImplementationClass(): KClass<ManeuverOptions> = ManeuverOptions::class

    override fun getFilledUpBuilder(): ManeuverOptions.Builder {
        return ManeuverOptions.Builder()
            .filterDuplicateManeuvers(false)
            .mutcdExitProperties(
                MapboxExitProperties.PropertiesMutcd(
                    shouldFallbackWithDrawable = false,
                    shouldFallbackWithText = true,
                    exitBackground = 0,
                    fallbackDrawable = 1,
                    exitLeftDrawable = 2,
                    exitRightDrawable = 3
                )
            )
            .viennaExitProperties(
                MapboxExitProperties.PropertiesVienna(
                    shouldFallbackWithDrawable = false,
                    shouldFallbackWithText = true,
                    exitBackground = 0,
                    fallbackDrawable = 1,
                    exitLeftDrawable = 2,
                    exitRightDrawable = 3
                )
            )
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
