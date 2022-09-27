package com.mapbox.navigation.core.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.updateDirectionsRouteOnly
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class SimilarRoutesTest {

    @Test
    fun `the same routes`() {
        val route = loadNavigationRoute("a")
        val similarity = calculateSimilarity(route, route)
        assertEquals(1.0, similarity, 0.00001)
    }


    @Test
    fun `the same routes but different ids`() {
        val original = loadNavigationRoute("a")
        val newId = original.updateDirectionsRouteOnly {
            toBuilder().requestUuid("different-id").build()
        }
        val similarity = calculateSimilarity(original, newId)
        assertEquals(1.0, similarity, 0.00001)
    }

    @Test
    fun `different routes`() {
        val a = loadNavigationRoute("a")
        val b = loadNavigationRoute("not_a")
        val similarity = calculateSimilarity(a, b)
        assertEquals(0.0, similarity, 0.00001)
    }

    @Test
    fun `compare route with its half`() {
        val a = loadNavigationRoute("a")
        val halfA = loadNavigationRoute("half_a")
        val similarity = calculateSimilarity(a, halfA)

        assertEquals(1.0, similarity, 0.001)
    }

    @Test
    fun `half of a route matches the route`() {
        val a = loadNavigationRoute("a")
        val halfA = loadNavigationRoute("half_a")
        val similarity = calculateSimilarity(halfA, a)
        assertEquals(1.0, similarity, 0.001)
    }

    private fun loadNavigationRoute(name: String) = createNavigationRoutes(
        DirectionsResponse.fromJson(resourceAsString("${name}_response.json")),
        RouteOptions.fromUrl(URL(resourceAsString("${name}_request.txt"))),
    ).first()

    private fun resourceAsString(
        name: String,
        packageName: String = "com.mapbox.navigation.core.internal.utils.similarroutes"
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        return IOUtils.toString(inputStream, "UTF-8")
    }
}