package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class ManeuverViewOptionsTest : BuilderTest<ManeuverViewOptions, ManeuverViewOptions.Builder>() {

    override fun getImplementationClass(): KClass<ManeuverViewOptions> = ManeuverViewOptions::class

    override fun getFilledUpBuilder(): ManeuverViewOptions.Builder {
        return ManeuverViewOptions.Builder()
            .primaryManeuverOptions(getPrimaryManeuverOptions())
            .secondaryManeuverOptions(getSecondaryManeuverOptions())
            .subManeuverOptions(getSubManeuverOptions())
    }

    @Test
    override fun trigger() {
        // see comments
    }

    private fun getPrimaryManeuverOptions(): ManeuverPrimaryOptions {
        return ManeuverPrimaryOptions
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
                    .build()
            )
            .build()
    }

    private fun getSecondaryManeuverOptions(): ManeuverSecondaryOptions {
        return ManeuverSecondaryOptions
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
                    .build()
            )
            .build()
    }

    private fun getSubManeuverOptions(): ManeuverSubOptions {
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
                    .build()
            )
            .build()
    }
}
