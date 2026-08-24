package com.mapbox.navigation.core.reroute

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createRouteOptions
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.net.URL

class RerouteContextReasonOptionsAdapterTest {

    @get:Rule
    val loggingFrontendTestRule = LoggingFrontendTestRule()

    @Test
    fun `adapter adds context and reason on reroute`() {
        val adapter = createAdapter()
        val testOptions = createRouteOptions(
            unrecognizedProperties = mapOf(
                "testKey" to JsonPrimitive("testValue"),
            ),
        )

        val result = adapter.onRouteOptions(
            testOptions,
            deviationParams(),
        )

        val resultUrl = result.toHttpUrl()
        assertEquals(
            "reroute",
            resultUrl.queryParameter("context"),
        )
        assertEquals(
            "deviation",
            resultUrl.queryParameter("reason"),
        )
        assertEquals(
            "testValue",
            resultUrl.queryParameter("testKey"),
        )
    }

    @Test
    fun `adapter keeps context and reason after second reroute with the same reason`() {
        val testOptions = createRouteOptions()

        val params = deviationParams()
        val adapter = createAdapter()
        val result = adapter.onRouteOptions(testOptions, params).let { firstRerouteResult ->
            adapter.onRouteOptions(firstRerouteResult, params)
        }

        val resultUrl = result.toHttpUrl()
        assertEquals(
            "reroute",
            resultUrl.queryParameter("context"),
        )
        assertEquals(
            "deviation",
            resultUrl.queryParameter("reason"),
        )
    }

    @Test
    fun `adapter changes context and reason after second reroute with different reason`() {
        val testOptions = createRouteOptions()

        val adapter = createAdapter()
        val result =
            adapter.onRouteOptions(testOptions, deviationParams()).let { firstRerouteResult ->
                adapter.onRouteOptions(firstRerouteResult, appTriggeredParams())
            }

        val resultUrl = result.toHttpUrl()
        assertEquals(
            "reroute",
            resultUrl.queryParameter("context"),
        )
        assertEquals(
            "parameters_change",
            resultUrl.queryParameter("reason"),
        )
    }

    @Test
    fun `adapter changes context and reason after reroute from CA`() {
        val caRouteOptionsGeneratedByNN = RouteOptions.fromUrl(
            URL(
                "https://api.mapbox.com/directions/v5/driverapp-here/driving-traffic/" +
                    "-118.244087488%2C33.7568809126;-118.39359%2C33.80324" +
                    "?access_token=****diFg&alternatives=true" +
                    "&annotations=distance%2Cduration%2Cspeed%2Cmaxspeed%2Ccongestion_numeric" +
                    "%2Cclosure%2Cfreeflow_speed&avoid_maneuver_radius=212.002" +
                    "&banner_instructions=true&bearings=250.715%2C45%3B" +
                    "&context=continuous_alternative&continue_straight=true&enable_refresh=true" +
                    "&geometries=polyline6&language=en-US&layers=0%3B&overview=full" +
                    "&reason=periodic&roundabout_exits=true" +
                    "&routes_history=J7YQ63ivGpyJ1pK7HZLCGRXl5VH2lZH37zkn0mGhuGxjNY7hzQ6N3w%3D" +
                    "%3D_us-west-2%2C0%2C198%3BlxFKuZXTAieCHK6gw4g59_wpf2aPzVlsvx_OmDRDJiv9I60b6" +
                    "-41TQ%3D%3D_us-west-2%2C0%2C0" +
                    "&snapping_include_closures=true%3B&snapping_include_static_closures=true" +
                    "%3B&steps=true&suppress_voice_instruction_local_names=true" +
                    "&voice_instructions=true&voice_units=imperial" +
                    "&waypoint_names=%3BRosita%20Place&waypoints=0%3B1&waypoints_per_route=true",
            ),
        )
        val testAdapter = createAdapter()

        val result = testAdapter.onRouteOptions(caRouteOptionsGeneratedByNN, appTriggeredParams())

        val resultUrl = result.toHttpUrl()
        assertEquals(
            "reroute",
            resultUrl.queryParameter("context"),
        )
        assertEquals(
            "parameters_change",
            resultUrl.queryParameter("reason"),
        )
    }
}

private fun createAdapter() = RerouteContextReasonOptionsAdapter()

private fun deviationParams() = createRouteOptionsAdapterParams(
    signature = deviationSignature,
)

private fun appTriggeredParams() = createRouteOptionsAdapterParams(
    signature = appTriggeredRerouteSignature,
)

private fun RouteOptions.toHttpUrl() = toUrl("***").toString().toHttpUrl()
