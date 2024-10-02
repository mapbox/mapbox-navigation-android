package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.ui.maps.internal.route.callout.model.DurationDifferenceType
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCallout
import com.mapbox.navigation.ui.maps.route.callout.model.MapboxRouteCalloutApiOptions
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxRouteCalloutApiTest {

    @Test
    fun `generate no callouts when there is only one route`() {
        val routes = createMockRoutes(routeCount = 1)
        val result = MapboxRouteCalloutApi().setNavigationRoutes(routes)

        assertTrue(result.callouts.isEmpty())
    }

    @Test
    fun `generate eta callouts for each route`() {
        val routes = createMockRoutes(routeCount = 5)

        val result = MapboxRouteCalloutApi().setNavigationRoutes(routes)

        assertTrue(result.callouts.all { it is RouteCallout.Eta })
        assertEquals(5, result.callouts.size)
    }

    @Test
    fun `only one eta callouts should be primary`() {
        val routes = createMockRoutes(routeCount = 5)

        val result = MapboxRouteCalloutApi().setNavigationRoutes(routes)

        assertEquals(1, result.callouts.count { it is RouteCallout.Eta && it.isPrimary })
    }

    @Test
    fun `eta callout primary geometry should remain the same while alt one should be different`() {
        val primaryGeometry = "_jajfAhauzgFqNoEh@}ChFyXr_@cpArAqIdAiKVoE}I_B"
        val routes = createMockRoutes(
            routeCount = 2,
            geometryList = listOf(
                "_jajfAhauzgFqNoEh@}ChFyXr_@cpArAqIdAiKVoE}I_B",
                "_jajfAhauzgFlA^lWjIb@iBNcAjJqe@zYm`AvF{]r@uE{k@cK}I_B",
            ),
        )

        val etaCallouts = MapboxRouteCalloutApi().setNavigationRoutes(routes).callouts
            .filterIsInstance<RouteCallout.Eta>()

        assertEquals(primaryGeometry, etaCallouts[0].geometry.toPolyline(6))
        assertEquals(
            "qgajfAhbuzgFlWjIb@iBNcAjJqe@zYm`AvF{]r@uE",
            etaCallouts[1].geometry.toPolyline(6),
        )
    }

    @Test
    fun `generate DurationDifference callouts for alternatives when type is RelativeDurations`() {
        val routes = createMockRoutes(routeCount = 5)
        val options = MapboxRouteCalloutApiOptions.Builder()
            .routeCalloutType(RouteCalloutType.RelativeDurationsOnAlternative)
            .build()

        val result = MapboxRouteCalloutApi(options).setNavigationRoutes(routes)

        assertTrue(result.callouts.all { it is RouteCallout.DurationDifference })
        assertEquals(4, result.callouts.size)
    }

    @Test
    fun `durationDifference callouts duration should be relative`() {
        val routes = createMockRoutes(
            routeCount = 3,
            durationList = listOf(120.0, 60.0, 180.0),
        )
        val options = MapboxRouteCalloutApiOptions.Builder()
            .routeCalloutType(RouteCalloutType.RelativeDurationsOnAlternative)
            .similarDurationDelta(0.seconds)
            .build()

        val relativeCallouts = MapboxRouteCalloutApi(options).setNavigationRoutes(routes).callouts
            .filterIsInstance<RouteCallout.DurationDifference>()
        val actualDuration = relativeCallouts.map { it.duration }
        val actualDurationDifferenceType = relativeCallouts.map { it.type }

        assertEquals(listOf(60.seconds, 60.seconds), actualDuration)
        assertEquals(
            listOf(DurationDifferenceType.Faster, DurationDifferenceType.Slower),
            actualDurationDifferenceType,
        )
    }

    @Test
    fun `durationDifference callouts type should be same if difference not greater the delta`() {
        val routes = createMockRoutes(
            routeCount = 3,
            durationList = listOf(120.0, 60.0, 180.0),
        )
        val options = MapboxRouteCalloutApiOptions.Builder()
            .routeCalloutType(RouteCalloutType.RelativeDurationsOnAlternative)
            .similarDurationDelta(61.seconds)
            .build()

        val relativeCallouts = MapboxRouteCalloutApi(options).setNavigationRoutes(routes).callouts
            .filterIsInstance<RouteCallout.DurationDifference>()
        val actualDuration = relativeCallouts.map { it.duration }
        val actualDurationDifferenceType = relativeCallouts.map { it.type }

        assertEquals(listOf(60.seconds, 60.seconds), actualDuration)
        assertEquals(
            listOf(DurationDifferenceType.Same, DurationDifferenceType.Same),
            actualDurationDifferenceType,
        )
    }

    @Test
    fun `update routeCalloutType should regenerate callouts for routes`() {
        val routes = createMockRoutes(routeCount = 5)

        val api = MapboxRouteCalloutApi()
        val result = api.setNavigationRoutes(routes)
        assertTrue(result.callouts.all { it is RouteCallout.Eta })

        val options = MapboxRouteCalloutApiOptions.Builder()
            .routeCalloutType(RouteCalloutType.RelativeDurationsOnAlternative)
            .build()
        val updatedResult = api.updateOptions(options)

        assertTrue(updatedResult.callouts.all { it is RouteCallout.DurationDifference })
        assertEquals(4, updatedResult.callouts.size)
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
