package com.mapbox.navigation.core.reroute

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.testing.factories.createRouteOptions
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URL

class CleanupCARelatedParamsAdapterTest {
    @Test
    fun `current_alternatives param is removed by adapter`() {
        val urlGeneratedByNNOnCARequest = "https://api.mapbox.com/" +
            "directions/v5/mapbox/driving-traffic/" +
            "11.5358898418%2C48.1676436069;11.5801189669%2C48.1254832547" +
            "?access_token=****edOA&alternatives=true" +
            "&annotations=distance%2Cduration&bearings=138.453%2C45%3B" +
            "&context=continuous_alternative" +
            "&continue_straight=true" +
            "&current_alternatives=clvz2limf039l19oi03jafen7_us-east-1%2C1%2C4%3" +
            "Bclvz2limf039l19oi03jafen7_us-east-1%2C2%2C4" +
            "&enable_refresh=true&geometries=polyline6" +
            "&layers=0%3B&overview=full&reason=periodic" +
            "&routes_history=clvz2limf039l19oi03jafen7_us-east-1%2C0%2C4" +
            "&snapping_include_closures=true%3B&snapping_include_static_closures=true%3B" +
            "&steps=true&waypoints=0%3B1&optimize_alternatives=true"
        val initialRouteOptions = RouteOptions.fromUrl(URL(urlGeneratedByNNOnCARequest))

        val result = createCleanupCARelatedParamsAdapter().onRouteOptions(
            initialRouteOptions,
            defaultRouteOptionsAdapterParams,
        )

        val url = result.toHttpUrl()
        assertNull(url.queryParameter("current_alternatives"))
        assertNull(url.queryParameter("optimize_alternatives"))
    }

    @Test
    fun `parameters not related to CA stay`() {
        val initialRouteOptions = createRouteOptions(
            unrecognizedProperties = mapOf("testKey" to JsonPrimitive("testValue")),
        )

        val result = createCleanupCARelatedParamsAdapter().onRouteOptions(
            initialRouteOptions,
            defaultRouteOptionsAdapterParams,
        )

        val url = result.toHttpUrl()
        assertEquals(
            "testValue",
            url.queryParameter("testKey"),
        )
    }

    @Test
    fun `adapter doesn't change route options if there are no unrecognized options`() {
        val initialRouteOptions = createRouteOptions()

        val result = createCleanupCARelatedParamsAdapter().onRouteOptions(
            initialRouteOptions,
            defaultRouteOptionsAdapterParams,
        )

        assertEquals(
            initialRouteOptions,
            result,
        )
    }
}

private fun createCleanupCARelatedParamsAdapter() = CleanupCARelatedParamsAdapter()

private fun RouteOptions.toHttpUrl() = toUrl("***").toString().toHttpUrl()
