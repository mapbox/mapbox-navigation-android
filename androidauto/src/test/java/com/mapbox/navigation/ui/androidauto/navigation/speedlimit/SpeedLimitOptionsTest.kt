package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class SpeedLimitOptionsTest : BuilderTest<SpeedLimitOptions, SpeedLimitOptions.Builder>() {

    override fun getImplementationClass() = SpeedLimitOptions::class

    override fun getFilledUpBuilder(): SpeedLimitOptions.Builder {
        return SpeedLimitOptions.Builder()
            .forcedSignFormat(SpeedLimitSign.VIENNA)
            .warningThreshold(10)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
