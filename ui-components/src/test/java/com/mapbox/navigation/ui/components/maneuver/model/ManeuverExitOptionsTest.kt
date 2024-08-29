package com.mapbox.navigation.ui.components.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class ManeuverExitOptionsTest : BuilderTest<ManeuverExitOptions, ManeuverExitOptions.Builder>() {

    override fun getImplementationClass(): KClass<ManeuverExitOptions> = ManeuverExitOptions::class

    override fun getFilledUpBuilder(): ManeuverExitOptions.Builder {
        return ManeuverExitOptions.Builder()
            .textAppearance(2)
            .mutcdExitProperties(
                MapboxExitProperties.PropertiesMutcd(
                    shouldFallbackWithDrawable = false,
                    shouldFallbackWithText = true,
                    exitBackground = 0,
                    fallbackDrawable = 1,
                    exitLeftDrawable = 2,
                    exitRightDrawable = 3,
                ),
            )
            .viennaExitProperties(
                MapboxExitProperties.PropertiesVienna(
                    shouldFallbackWithDrawable = false,
                    shouldFallbackWithText = true,
                    exitBackground = 0,
                    fallbackDrawable = 1,
                    exitLeftDrawable = 2,
                    exitRightDrawable = 3,
                ),
            )
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
