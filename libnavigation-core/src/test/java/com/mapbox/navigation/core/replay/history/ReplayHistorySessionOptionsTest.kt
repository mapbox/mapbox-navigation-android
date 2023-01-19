package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ReplayHistorySessionOptionsTest :
    BuilderTest<ReplayHistorySessionOptions, ReplayHistorySessionOptions.Builder>() {
    override fun getImplementationClass(): KClass<ReplayHistorySessionOptions> =
        ReplayHistorySessionOptions::class

    override fun getFilledUpBuilder(): ReplayHistorySessionOptions.Builder {
        return ReplayHistorySessionOptions.Builder()
            .filePath("test_path")
            .replayHistoryMapper(mockk(relaxed = true))
            .enableSetRoute(false)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
