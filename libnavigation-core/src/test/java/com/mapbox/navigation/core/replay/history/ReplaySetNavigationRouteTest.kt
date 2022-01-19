package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.testing.BuilderTest
import io.mockk.mockk
import org.junit.Test

class ReplaySetNavigationRouteTest :
    BuilderTest<ReplaySetNavigationRoute, ReplaySetNavigationRoute.Builder>() {
    override fun getImplementationClass() = ReplaySetNavigationRoute::class

    override fun getFilledUpBuilder() = ReplaySetNavigationRoute.Builder(123.0)
        .route(mockk())

    @Test
    override fun trigger() {
        // see comments
    }
}
