package com.mapbox.navigation.base.route

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import java.util.concurrent.TimeUnit

class RouteRefreshOptionsTest : BuilderTest<RouteRefreshOptions, RouteRefreshOptions.Builder>() {

    override fun getImplementationClass() = RouteRefreshOptions::class

    override fun getFilledUpBuilder() = RouteRefreshOptions.Builder()
        .intervalMillis(TimeUnit.SECONDS.toMillis(30))

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
