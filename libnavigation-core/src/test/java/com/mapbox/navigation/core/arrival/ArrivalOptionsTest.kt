package com.mapbox.navigation.core.arrival

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

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

    @Test(expected = Throwable::class)
    fun `negative arrival time is not supported`() {
        ArrivalOptions.Builder()
            .arrivalInMeters(-1.0)
            .build()
    }

    @Test(expected = Throwable::class)
    fun `negative arrival distance is not supported`() {
        ArrivalOptions.Builder()
            .arrivalInMeters(-1.0)
            .build()
    }

    @Test(expected = Throwable::class)
    fun `time or distance is required`() {
        ArrivalOptions.Builder()
            .arrivalInSeconds(null)
            .arrivalInMeters(null)
            .build()
    }
}
