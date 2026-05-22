package com.mapbox.navigation.base.utils

import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteStep
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DecodeUtilsCacheLimitTest {

    @Before
    @After
    fun clearCache() {
        DecodeUtils.clearCacheInternal()
    }

    private fun encodedPolyline(pointCount: Int, latOffset: Double = 0.0): String {
        val points = (0 until pointCount).map { i ->
            Point.fromLngLat(i * 0.001, latOffset)
        }
        return PolylineUtils.encode(points, 6)
    }

    private fun routeWithPointCount(
        pointCount: Int,
        uuid: String = "uuid",
        routeIndex: String = "0",
        latOffset: Double = 0.0,
    ) = createDirectionsRoute(
        requestUuid = uuid,
        routeIndex = routeIndex,
        geometry = encodedPolyline(pointCount, latOffset),
        legs = listOf(
            createRouteLeg(
                steps = listOf(
                    createRouteStep().toBuilder()
                        .geometry(encodedPolyline(pointCount, latOffset))
                        .build(),
                ),
            ),
        ),
    )

    @Test
    fun `sanity - decoding actually populates cache`() {
        val pointCount = 100
        val route = routeWithPointCount(pointCount)
        with(DecodeUtils) {
            route.completeGeometryToPoints()
            assertEquals(
                "Cache should contain exactly $pointCount points after decoding",
                pointCount,
                completeGeometryCacheTotalPoints(),
            )
        }
    }

    @Test
    fun `complete geometry cache does not exceed point count limit`() {
        val pointsPerRoute = 60_000
        val route1 = routeWithPointCount(pointsPerRoute, uuid = "uuid1", latOffset = 0.0)
        val route2 = routeWithPointCount(pointsPerRoute, uuid = "uuid2", latOffset = 1.0)
        val route3 = routeWithPointCount(pointsPerRoute, uuid = "uuid3", latOffset = 2.0)

        with(DecodeUtils) {
            route1.completeGeometryToPoints()
            route2.completeGeometryToPoints()
            route3.completeGeometryToPoints()

            val totalCachedPoints = completeGeometryCacheTotalPoints()
            assertTrue(
                "Total cached points ($totalCachedPoints) exceeded limit " +
                    "($COMPLETE_GEOMETRY_CACHE_MAX_POINTS)",
                totalCachedPoints <= COMPLETE_GEOMETRY_CACHE_MAX_POINTS,
            )
        }
    }

    @Test
    fun `steps geometry cache does not exceed point count limit`() {
        val pointsPerRoute = 60_000
        val route1 = routeWithPointCount(pointsPerRoute, uuid = "uuid1", latOffset = 0.0)
        val route2 = routeWithPointCount(pointsPerRoute, uuid = "uuid2", latOffset = 1.0)
        val route3 = routeWithPointCount(pointsPerRoute, uuid = "uuid3", latOffset = 2.0)

        with(DecodeUtils) {
            route1.stepsGeometryToPoints()
            route2.stepsGeometryToPoints()
            route3.stepsGeometryToPoints()

            val totalCachedPoints = stepsGeometryCacheTotalPoints()
            assertTrue(
                "Total cached points ($totalCachedPoints) exceeded limit " +
                    "($STEPS_GEOMETRY_CACHE_MAX_POINTS)",
                totalCachedPoints <= STEPS_GEOMETRY_CACHE_MAX_POINTS,
            )
        }
    }
}
