package com.mapbox.navigation.route.internal.util

import java.util.Scanner

class TestRouteFixtures {
    fun loadTwoLegRoute() = loadJsonFixture("directions_response_two_leg_route.json")

    fun loadTwoLegRouteWithRefreshTtl() = loadJsonFixture(
        "directions_response_two_leg_route_with_refresh_ttl.json"
    )

    fun loadTwoRoutes() = loadJsonFixture("directions_response_two_routes.json")

    fun loadEmptyRoutesResponse() = loadJsonFixture("directions_response_empty_routes.json")

    /**
     * This route contains a cleared out congestion and speed annotations
     */
    fun loadMultiLegRouteForRefresh() = loadJsonFixture("multi_leg_route_for_refresh.json")

    /**
     * Refresh response for the [loadMultiLegRouteForRefresh].
     */
    fun loadRefreshForMultiLegRoute() =
        loadJsonFixture("multi_leg_refresh_response.json")

    fun loadRefreshForMultiLegRouteWithRefreshTtl() =
        loadJsonFixture("multi_leg_refresh_response_with_refresh_ttl.json")

    /**
     * Refreshed [loadMultiLegRouteForRefresh] by [loadRefreshForMultiLegRoute].
     */
    fun loadRefreshedMultiLegRoute() =
        loadJsonFixture("multi_leg_route_refreshed.json")

    /**
     * Refresh response for the [loadMultiLegRouteForRefresh] starting from second leg.
     */
    fun loadRefreshForMultiLegRouteSecondLeg() =
        loadJsonFixture("multi_leg_refresh_response_second_leg.json")

    /**
     * Refreshed [loadMultiLegRouteForRefresh] by [loadRefreshForMultiLegRoute] starting from second leg.
     */
    fun loadRefreshedMultiLegRouteSecondLeg() =
        loadJsonFixture("multi_leg_route_refreshed_second_leg.json")

    private fun loadJsonFixture(filename: String): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream(filename)
        return inputStream?.let {
            val scanner = Scanner(it, "UTF-8").useDelimiter("\\A")
            if (scanner.hasNext()) scanner.next() else ""
        } ?: ""
    }
}
