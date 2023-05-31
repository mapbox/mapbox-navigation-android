package com.mapbox.navigation.base.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.CopilotOptions
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CopilotOptionsTest : BuilderTest<CopilotOptions, CopilotOptions.Builder>() {
    override fun getImplementationClass() = CopilotOptions::class

    override fun getFilledUpBuilder() = CopilotOptions.Builder()
        .shouldSendHistoryOnlyWithFeedback(true)
        .maxHistoryFileLengthMillis(180000)
        .maxHistoryFilesPerSession(2)
        .maxTotalHistoryFilesSizePerSession(250000)
        .shouldRecordFreeDriveHistories(false)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
