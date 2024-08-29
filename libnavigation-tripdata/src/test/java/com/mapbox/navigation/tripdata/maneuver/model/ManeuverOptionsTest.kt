package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class ManeuverOptionsTest : BuilderTest<ManeuverOptions, ManeuverOptions.Builder>() {

    override fun getImplementationClass(): KClass<ManeuverOptions> = ManeuverOptions::class

    override fun getFilledUpBuilder(): ManeuverOptions.Builder {
        return ManeuverOptions.Builder()
            .filterDuplicateManeuvers(false)
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
