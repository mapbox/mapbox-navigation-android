package com.mapbox.navigation.core.fasterroute

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.updateDirectionsRouteOnly
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class RoutesSimilarityTest {

    @Test
    fun `the same routes`() {
        val route = loadNavigationRoute("a")
        val similarity = calculateGeometrySimilarity(route, route)
        assertEquals(1.0, similarity, 0.00001)
    }

    @Test
    fun `the same routes but different ids`() {
        val original = loadNavigationRoute("a")
        val newId = original.updateDirectionsRouteOnly {
            toBuilder().requestUuid("different-id").build()
        }

        val geometrySimilarity = calculateGeometrySimilarity(original, newId)
        val streetsSimilarity = calculateStreetsSimilarity(original, newId)

        assertEquals(1.0, geometrySimilarity, 0.00001)
        assertEquals(1.0, streetsSimilarity, 0.00001)
    }

    @Test
    fun `different routes  similarity`() {
        val a = loadNavigationRoute("a")
        val b = loadNavigationRoute("not_a")

        val similarity = calculateGeometrySimilarity(a, b)
        val summarySimilarity = calculateSummarySimilarity(a, b)
        val streetsSimilarity = calculateStreetsSimilarity(a, b)

        assertEquals(0.0, similarity, 0.00001)
        assertEquals(0.0, summarySimilarity, 0.00001)
        assertEquals(0.0, streetsSimilarity, 0.00001)
    }

    @Test
    fun `compare route with its half`() {
        val a = loadNavigationRoute("a")
        val halfA = loadNavigationRoute("half_a")

        val geometrySimilarity = calculateGeometrySimilarity(a, halfA)
        val summarySimilarity = calculateSummarySimilarity(a, halfA)
        val streetsSimilarity = calculateStreetsSimilarity(a, halfA)

        assertEquals(1.0, geometrySimilarity, 0.001)
        assertEquals(1.0, summarySimilarity, 0.001)
        assertEquals(1.0, streetsSimilarity, 0.001)
    }

    @Test
    fun `half of a route matches the route`() {
        val a = loadNavigationRoute("a")
        val halfA = loadNavigationRoute("half_a")

        val similarity = calculateGeometrySimilarity(halfA, a)
        val descriptionSimilarity = calculateSummarySimilarity(halfA, a)
        val streetsSimilarity = calculateStreetsSimilarity(halfA, a)


        assertEquals(1.0, similarity, 0.001)
        assertEquals(1.0, descriptionSimilarity, 0.001)
        assertEquals(1.0, streetsSimilarity, 0.001)
    }

    @Test
    fun `compare partially matched routes`() {
        val a = loadNavigationRoute("a")
        val endsLikeA = loadNavigationRoute("ends_like_a")

        val geometrySimilarity = calculateGeometrySimilarity(endsLikeA, a)
        val summarySimilarity = calculateSummarySimilarity(endsLikeA, a)
        val streetsSimilarity = calculateSummarySimilarity(endsLikeA, a)

        assertEquals(0.8, geometrySimilarity, 0.05)
        assertEquals(0.5, summarySimilarity, 0.001)
        assertEquals(0.5, streetsSimilarity, 0.001)
    }

    private fun loadNavigationRoute(name: String) = createNavigationRoutes(
        DirectionsResponse.fromJson(resourceAsString("${name}_response.json")),
        RouteOptions.fromUrl(URL(resourceAsString("${name}_request.txt"))),
    ).first()

    private fun resourceAsString(
        name: String,
        packageName: String = "com.mapbox.navigation.core.internal.fasterroute.similarity"
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        return IOUtils.toString(inputStream, "UTF-8")
    }
}
