package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.testing.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteExclusionsTest {

    @Test
    fun `RouteOptions Builder exclude parsing`() {
        val origin = Point.fromLngLat(14.75513115258181, 55.19464648744247)
        val destination = Point.fromLngLat(12.54071010365584, 55.68521471271404)
        val routeOptionsBuilder = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(listOf(origin, destination))
            .accessToken("pk.123")

        val routeOptionsWithExclusions = routeOptionsBuilder.exclude(
            DirectionsCriteria.EXCLUDE_TOLL,
            DirectionsCriteria.EXCLUDE_FERRY
        ).build()

        assertEquals("toll,ferry", routeOptionsWithExclusions.exclude())
    }

    @Test
    fun `empty exclusion violations if no exclude RouteOptions added`() {
        val origin = Point.fromLngLat(14.75513115258181, 55.19464648744247)
        val destination = Point.fromLngLat(12.54071010365584, 55.68521471271404)
        val routeOptionsWithoutExclusions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(listOf(origin, destination))
            .accessToken("pk.123")
            .build()
        val directionsRoute = DirectionsRoute.builder()
            .routeOptions(routeOptionsWithoutExclusions)
            .distance(183888.609)
            .duration(10697.573)
            .build()

        val exclusionViolations = directionsRoute.exclusionViolations()

        assertEquals(0, exclusionViolations.size)
    }

    @Test
    fun `toll and ferry exclusion violations - size`() {
        val directionsRoute = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("toll_and_ferry_directions_route.json")
        )

        val exclusionViolations = directionsRoute.exclusionViolations()

        assertEquals(77, exclusionViolations.size)
    }

    @Test
    fun `toll and ferry exclusion violations - type`() {
        val directionsRoute = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("toll_and_ferry_directions_route.json")
        )

        val tollAndFerryExclusionViolations = directionsRoute.exclusionViolations()
            .groupBy { it.type }

        assertEquals(2, tollAndFerryExclusionViolations.size)
        assertTrue(tollAndFerryExclusionViolations.containsKey("toll"))
        assertTrue(tollAndFerryExclusionViolations.containsKey("ferry"))
    }
}
