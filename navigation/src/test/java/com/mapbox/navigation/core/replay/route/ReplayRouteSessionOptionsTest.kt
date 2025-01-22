package com.mapbox.navigation.core.replay.route

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test
import kotlin.reflect.KClass

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ReplayRouteSessionOptionsTest :
    BuilderTest<ReplayRouteSessionOptions, ReplayRouteSessionOptions.Builder>() {
    override fun getImplementationClass(): KClass<ReplayRouteSessionOptions> =
        ReplayRouteSessionOptions::class

    override fun getFilledUpBuilder(): ReplayRouteSessionOptions.Builder {
        return ReplayRouteSessionOptions.Builder()
            .replayRouteOptions(mockk(relaxed = true))
            .locationResetEnabled(false)
            .decodeMinDistance(1458.54)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
