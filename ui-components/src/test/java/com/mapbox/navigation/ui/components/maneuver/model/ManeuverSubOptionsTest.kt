package com.mapbox.navigation.ui.components.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class ManeuverSubOptionsTest :
    BuilderTest<ManeuverSubOptions, ManeuverSubOptions.Builder>() {

    override fun getImplementationClass(): KClass<ManeuverSubOptions> =
        ManeuverSubOptions::class

    override fun getFilledUpBuilder(): ManeuverSubOptions.Builder {
        return ManeuverSubOptions
            .Builder()
            .textAppearance(1)
            .exitOptions(
                ManeuverExitOptions
                    .Builder()
                    .textAppearance(1)
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
                    .build(),
            )
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
