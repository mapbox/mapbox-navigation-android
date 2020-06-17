package com.mapbox.navigation.base.internal.extensions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRefreshExTest {

    private val defaultOptionsBuilder = RouteOptions.builder()
        .accessToken("test_access_token")
        .coordinates(
            listOf(
                Point.fromLngLat(-121.470162, 38.563121),
                Point.fromLngLat(-121.483304, 38.583313)
            )
        )
        .applyDefaultParams()

    @Test
    fun `should enable route refresh with all the criteria`() {
        val routeOptions = defaultOptionsBuilder
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .annotationsList(listOf(DirectionsCriteria.ANNOTATION_MAXSPEED))
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .build()

        assertTrue(routeOptions.supportsRefresh())
    }

    @Test
    fun `should enable route refresh with annotation congestion`() {
        val routeOptions = defaultOptionsBuilder
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .annotationsList(listOf(DirectionsCriteria.ANNOTATION_CONGESTION))
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .build()

        assertTrue(routeOptions.supportsRefresh())
    }

    @Test
    fun `should disable route refresh when overview is not full`() {
        val routeOptions = defaultOptionsBuilder
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .annotationsList(listOf(DirectionsCriteria.ANNOTATION_MAXSPEED))
            .overview(DirectionsCriteria.OVERVIEW_SIMPLIFIED)
            .build()

        assertFalse(routeOptions.supportsRefresh())
    }

    @Test
    fun `should disable route refresh when annotations does not have maxspeed`() {
        val routeOptions = defaultOptionsBuilder
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .annotationsList(
                listOf(DirectionsCriteria.ANNOTATION_SPEED, DirectionsCriteria.ANNOTATION_DISTANCE)
            )
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .build()

        assertFalse(routeOptions.supportsRefresh())
    }

    @Test
    fun `should disable route refresh when profile is not driving-traffic`() {
        val routeOptions = defaultOptionsBuilder
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .annotationsList(
                listOf(DirectionsCriteria.ANNOTATION_SPEED, DirectionsCriteria.ANNOTATION_DISTANCE)
            )
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .build()

        assertFalse(routeOptions.supportsRefresh())
    }
}
