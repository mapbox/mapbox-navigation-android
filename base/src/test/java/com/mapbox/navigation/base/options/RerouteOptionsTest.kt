package com.mapbox.navigation.base.options

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.reflect.KClass

class RerouteOptionsTest : BuilderTest<RerouteOptions, RerouteOptions.Builder>() {

    override fun getImplementationClass(): KClass<RerouteOptions> =
        RerouteOptions::class

    @OptIn(ExperimentalMapboxNavigationAPI::class, ExperimentalPreviewMapboxNavigationAPI::class)
    override fun getFilledUpBuilder(): RerouteOptions.Builder =
        RerouteOptions.Builder()
            .avoidManeuverSeconds(10)
            .rerouteStrategyForMapMatchedRoutes(NavigateToFinalDestination)
            .repeatRerouteAfterOffRouteDelaySeconds(5)

    @Test
    override fun trigger() {
        // trigger, see KDoc
    }

    @Test
    fun test_default_value_is_8() {
        val rerouteOptions = RerouteOptions.Builder().build()
        assertEquals(8, rerouteOptions.avoidManeuverSeconds)
    }

    @Test
    fun test_reroute_delay_is_turned_off_by_default() {
        val rerouteOptions = RerouteOptions.Builder().build()
        assertEquals(-1, rerouteOptions.repeatRerouteAfterOffRouteDelaySeconds)
    }

    @Test(expected = IllegalStateException::class)
    fun set_negative_maneuver_seconds_trow_exception() {
        RerouteOptions.Builder().avoidManeuverSeconds(-1).build()
    }

    @Test(expected = IllegalStateException::class)
    fun set_incorrect_reroute_delay_seconds_trow_exception() {
        RerouteOptions.Builder().repeatRerouteAfterOffRouteDelaySeconds(-2).build()
    }
}
