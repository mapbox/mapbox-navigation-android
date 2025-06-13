package com.mapbox.navigation.base.options

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class HistoryRecorderOptionsTest :
    BuilderTest<HistoryRecorderOptions, HistoryRecorderOptions.Builder>() {
    override fun getImplementationClass() = HistoryRecorderOptions::class

    override fun getFilledUpBuilder() = HistoryRecorderOptions.Builder()
        .fileDirectory("/history/path")
        .shouldRecordRouteLineEvents(true)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
