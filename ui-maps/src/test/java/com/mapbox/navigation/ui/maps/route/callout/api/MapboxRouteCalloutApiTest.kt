package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.ui.maps.internal.route.callout.api.MapboxRouteCalloutsApi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxRouteCalloutApiTest {

    @Test
    fun `generate a single callout when there is only one route`() {
        val routes = createMockRoutes(routeCount = 1)
        val result = MapboxRouteCalloutsApi().setNavigationRoutes(routes, emptyList())

        assertTrue(result.callouts.first().isPrimary)
        assertEquals(1, result.callouts.size)
    }

    @Test
    fun `generate eta callouts for each route`() {
        val routes = createMockRoutes(routeCount = 5)

        val result = MapboxRouteCalloutsApi().setNavigationRoutes(routes, emptyList())

        assertEquals(5, result.callouts.size)
    }

    @Test
    fun `only one eta callouts should be primary`() {
        val routes = createMockRoutes(routeCount = 5)

        val result = MapboxRouteCalloutsApi().setNavigationRoutes(routes, emptyList())

        assertEquals(1, result.callouts.count { it.isPrimary })
    }

    @Test
    fun `first route callout should have duration difference 0`() {
        val routes = createMockRoutes(routeCount = 5)

        val result = MapboxRouteCalloutsApi().setNavigationRoutes(routes, emptyList())

        assertEquals(0.seconds, result.callouts.first().durationDifferenceWithPrimary)
    }

    @Test
    fun `durationDifference callouts duration should be relative`() {
        val routes = createMockRoutes(
            routeCount = 3,
            durationList = listOf(120.0, 60.0, 180.0),
        )

        val callouts = MapboxRouteCalloutsApi().setNavigationRoutes(routes, emptyList()).callouts
        val actualDuration = callouts.map { it.durationDifferenceWithPrimary }

        assertEquals(listOf(0.seconds, 60.seconds, (-60).seconds), actualDuration)
    }

    private fun createMockRoutes(
        routeCount: Int = 2,
        durationList: List<Double> = listOf(1000.0, 1500.0),
        geometryList: List<String> = listOf(
            "_jajfAhauzgFqNoEh@}ChFyXr_@cpArAqIdAiKVoE}I_B",
            "_jajfAhauzgFlA^lWjIb@iBNcAjJqe@zYm`AvF{]r@uE{k@cK}I_B",
        ),
    ): List<NavigationRoute> {
        val routes = mutableListOf<DirectionsRoute>()

        repeat(routeCount) {
            val geometry = geometryList.getOrNull(it)
            val duration = durationList.getOrNull(it) ?: 1000.0
            routes.add(
                createDirectionsRoute(
                    geometry = geometry,
                    duration = duration,
                ),
            )
        }

        return createNavigationRoutes(
            createDirectionsResponse(
                routes = routes,
            ),
        )
    }
}
