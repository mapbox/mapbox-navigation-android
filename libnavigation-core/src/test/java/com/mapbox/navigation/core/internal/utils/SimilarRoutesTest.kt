package com.mapbox.navigation.core.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class SimilarRoutesTest {

    @Test
    fun `the same routes`() {
        val route = createNavigationRoutes(
            DirectionsResponse.fromJson(resourceAsString("a.json")),
            RouteOptions.fromUrl(URL("https://api.mapbox.com/directions/v5/mapbox/driving/-73.978054%2C40.754434%3B-73.973023%2C40.761265?alternatives=true&geometries=polyline6&language=en&overview=full&steps=true")),
        ).first()
        val similarity = calculateRoutesSimilarity(route, route)
        assertEquals(1.0, similarity, 0.00001)
    }

    @Test
    fun `different routes`() {
        val a = createNavigationRoutes(
            DirectionsResponse.fromJson(resourceAsString("a.json")),
            RouteOptions.fromUrl(URL("https://api.mapbox.com/directions/v5/mapbox/driving/-73.978054%2C40.754434%3B-73.973023%2C40.761265?alternatives=true&geometries=polyline6&language=en&overview=full&steps=true")),
        ).first()
        val b = createNavigationRoutes(
            DirectionsResponse.fromJson(resourceAsString("b.json")),
            RouteOptions.fromUrl(URL("https://api.mapbox.com/directions/v5/mapbox/driving/-73.973758%2C40.757206%3B-73.970704%2C40.761369?alternatives=true&geometries=polyline6&language=en&overview=full&steps=true")),
        ).first()
        val similarity = calculateRoutesSimilarity(a, b)
        assertEquals(0.0, similarity, 0.00001)
    }

    @Test
    fun `half the same routes`() {
        val a = createNavigationRoutes(
            DirectionsResponse.fromJson(resourceAsString("a.json")),
            RouteOptions.fromUrl(URL("https://api.mapbox.com/directions/v5/mapbox/driving/-73.978054%2C40.754434%3B-73.973023%2C40.761265?alternatives=true&geometries=polyline6&language=en&overview=full&steps=true")),
        ).first()
        val halfA = createNavigationRoutes(
            DirectionsResponse.fromJson(resourceAsString("b.json")),
            RouteOptions.fromUrl(URL("https://api.mapbox.com/directions/v5/mapbox/driving/-73.97406112344191%2C40.758298547604284%3B-73.973023%2C40.761265?alternatives=true&geometries=polyline6&language=en&overview=full&steps=true")),
        ).first()
        val similarity = calculateRoutesSimilarity(a, halfA)
        assertEquals(0.5, similarity, 0.1)
    }

    private fun resourceAsString(
        name: String,
        packageName: String = "com.mapbox.navigation.core.internal.utils.similarroutes"
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        return IOUtils.toString(inputStream, "UTF-8")
    }
}