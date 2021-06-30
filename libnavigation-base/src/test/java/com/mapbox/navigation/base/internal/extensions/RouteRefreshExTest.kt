package com.mapbox.navigation.base.internal.extensions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.supportsRouteRefresh
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRefreshExTest {

    private val defaultOptionsBuilder = RouteOptions.builder()
        .accessToken("test_access_token")
        .coordinatesList(
            listOf(
                Point.fromLngLat(-121.470162, 38.563121),
                Point.fromLngLat(-121.483304, 38.583313)
            )
        )
        .applyDefaultNavigationOptions()

    @Test
    fun `should enable route refresh with all the criteria`() {
        val routeOptions = defaultOptionsBuilder
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .annotationsList(listOf(DirectionsCriteria.ANNOTATION_MAXSPEED))
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .build()

        assertTrue(routeOptions.supportsRouteRefresh())
    }

    @Test
    fun `should enable route refresh with annotation congestion`() {
        val routeOptions = defaultOptionsBuilder
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .annotationsList(listOf(DirectionsCriteria.ANNOTATION_CONGESTION))
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .build()

        assertTrue(routeOptions.supportsRouteRefresh())
    }

    @Test
    fun `should disable route refresh when overview is not full`() {
        val routeOptions = defaultOptionsBuilder
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .annotationsList(listOf(DirectionsCriteria.ANNOTATION_MAXSPEED))
            .overview(DirectionsCriteria.OVERVIEW_SIMPLIFIED)
            .build()

        assertFalse(routeOptions.supportsRouteRefresh())
    }

    @Test
    fun `should disable route refresh when profile is not driving-traffic`() {
        val routeOptions = defaultOptionsBuilder
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .annotationsList(
                listOf(DirectionsCriteria.ANNOTATION_SPEED, DirectionsCriteria.ANNOTATION_DISTANCE)
            )
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .build()

        assertFalse(routeOptions.supportsRouteRefresh())
    }
}
