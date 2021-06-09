package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class HistoryRecorderOptionsTest :
    BuilderTest<HistoryRecorderOptions, HistoryRecorderOptions.Builder>() {
    override fun getImplementationClass() = HistoryRecorderOptions::class

    override fun getFilledUpBuilder() = HistoryRecorderOptions.Builder()
        .enabled(true)
        .fileDirectory("/history/path")

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
