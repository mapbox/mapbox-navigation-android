package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.KClass

class RerouteOptionsTest : BuilderTest<RerouteOptions, RerouteOptions.Builder>() {

    override fun getImplementationClass(): KClass<RerouteOptions> =
        RerouteOptions::class

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    override fun getFilledUpBuilder(): RerouteOptions.Builder =
        RerouteOptions.Builder()
            .avoidManeuverSeconds(10)
            .rerouteStrategyForMapMatchedRoutes(NavigateToFinalDestination)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }

    @Test
    fun test_default_value_is_8() {
        val rerouteOptions = RerouteOptions.Builder().build()

        assertEquals(8, rerouteOptions.avoidManeuverSeconds)
    }

    @Test(expected = IllegalStateException::class)
    fun set_negative_maneuver_seconds_trow_exception() {
        RerouteOptions.Builder().avoidManeuverSeconds(-1).build()
    }
}
