package com.mapbox.navigation.core.arrival

import com.mapbox.navigation.testing.BuilderTest
import kotlin.reflect.KClass
import org.junit.Test

class ArrivalOptionsTest : BuilderTest<ArrivalOptions, ArrivalOptions.Builder>() {
    override fun getImplementationClass(): KClass<ArrivalOptions> = ArrivalOptions::class

    override fun getFilledUpBuilder(): ArrivalOptions.Builder {
        return ArrivalOptions.Builder()
            .arrivalInMeters(123.0)
            .arrivalInSeconds(345.0)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
