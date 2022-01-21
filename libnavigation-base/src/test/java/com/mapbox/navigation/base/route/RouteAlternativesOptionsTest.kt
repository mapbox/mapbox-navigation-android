package com.mapbox.navigation.base.route

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import java.util.concurrent.TimeUnit

class RouteAlternativesOptionsTest :
    BuilderTest<RouteAlternativesOptions, RouteAlternativesOptions.Builder>() {

    override fun getImplementationClass() = RouteAlternativesOptions::class

    override fun getFilledUpBuilder() = RouteAlternativesOptions.Builder()
        .intervalMillis(TimeUnit.SECONDS.toMillis(30))
        .avoidManeuverSeconds(5)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }
}
