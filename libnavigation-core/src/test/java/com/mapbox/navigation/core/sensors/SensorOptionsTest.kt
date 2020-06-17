package com.mapbox.navigation.core.sensors

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

class SensorOptionsTest : BuilderTest<SensorOptions, SensorOptions.Builder>() {
    override fun getImplementationClass(): KClass<SensorOptions> = SensorOptions::class

    override fun getFilledUpBuilder(): SensorOptions.Builder {
        return SensorOptions.Builder()
            .enableSensorTypes(mockk(relaxed = true))
            .signalsPerSecond(123)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
