package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import kotlin.reflect.KClass

class ManeuverOptionsTest : BuilderTest<ManeuverOptions, ManeuverOptions.Builder>() {

    override fun getImplementationClass(): KClass<ManeuverOptions> = ManeuverOptions::class

    override fun getFilledUpBuilder(): ManeuverOptions.Builder {
        return ManeuverOptions.Builder()
            .filterDuplicateManeuvers(false)
    }

    override fun trigger() {
        // see comments
    }
}
