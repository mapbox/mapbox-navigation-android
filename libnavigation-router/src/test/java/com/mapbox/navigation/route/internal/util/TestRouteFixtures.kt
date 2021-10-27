package com.mapbox.navigation.route.internal.util

import java.util.Scanner

class TestRouteFixtures {
    fun loadTwoLegRoute() = loadJsonFixture(RESPONSE_MULTI_LEG_ROUTE_FIXTURE)
    fun loadRefreshedRoute() = loadJsonFixture(REFRESHED_ROUTE_FIXTURE)
    fun loadEmptyRoutesResponse() = loadJsonFixture(RESPONSE_EMPTY_ROUTES_FIXTURE)

    private fun loadJsonFixture(filename: String): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream(filename)
        return inputStream?.let {
            val scanner = Scanner(it, "UTF-8").useDelimiter("\\A")
            if (scanner.hasNext()) scanner.next() else ""
        } ?: ""
    }

    private companion object {
        const val RESPONSE_MULTI_LEG_ROUTE_FIXTURE = "directions_response_two_leg_route.json"
        const val RESPONSE_EMPTY_ROUTES_FIXTURE = "directions_response_empty_routes.json"
        const val REFRESHED_ROUTE_FIXTURE = "refreshed_route.json"
    }
}
