package com.mapbox.navigation.base.internal

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class CopilotOptionsTest : BuilderTest<CopilotOptions, CopilotOptions.Builder>() {
    override fun getImplementationClass() = CopilotOptions::class

    override fun getFilledUpBuilder() = CopilotOptions.Builder()
        .shouldSendHistoryOnlyWithFeedback(true)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
