package com.mapbox.navigation.core.trip.model.roadobject

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectGeometry
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadObjectUtilsTest {

    @Test
    fun `verify alert geometry linestring`() {
        val directionsRoute = getDirectionsRoutesFromJsonResponse(
            "mock_response_multi_leg_route_alerts_restricted_tunnel.json"
        )
        val objectGeometry = RoadObjectGeometry.Builder(
            length = 27.962033570785458,
            shape = LineString.fromLngLats(
                listOf(
                    Point.fromLngLat(17.043697, 51.117523),
                    Point.fromLngLat(17.043299, 51.117504)
                )
            ),
            startGeometryIndex = 0,
            endGeometryIndex = 2
        ).build()
        val expectedLineString = LineString.fromLngLats(
            listOf(
                Point.fromLngLat(17.043697, 51.117523),
                Point.fromLngLat(17.043516, 51.117515),
                Point.fromLngLat(17.043298, 51.117504)
            )
        )

        val actualLineString = objectGeometry.toLineString(directionsRoute, 6)

        assertEquals(expectedLineString, actualLineString)
    }

    @Test
    fun `verify alert geometry linestring second leg`() {
        val directionsRoute = getDirectionsRoutesFromJsonResponse(
            "mock_response_multi_leg_route_alerts_restricted_tunnel.json"
        )
        val objectGeometry = RoadObjectGeometry.Builder(
            length = 43.13547754234237,
            shape = LineString.fromLngLats(
                listOf(
                    Point.fromLngLat(17.043299, 51.117504),
                    Point.fromLngLat(17.043203, 51.117191)
                )
            ),
            startGeometryIndex = 2,
            endGeometryIndex = 7
        ).build()
        val expectedLineString = LineString.fromLngLats(
            listOf(
                Point.fromLngLat(17.043298, 51.117504),
                Point.fromLngLat(17.043174, 51.1175),
                Point.fromLngLat(17.043186, 51.117378),
                Point.fromLngLat(17.043197, 51.117252),
                Point.fromLngLat(17.043199, 51.117237),
                Point.fromLngLat(17.043203, 51.117191)
            )
        )

        val actualLineString = objectGeometry.toLineString(directionsRoute, 6)

        assertEquals(expectedLineString, actualLineString)
    }

    private fun getDirectionsRoutesFromJsonResponse(
        fileName: String,
        routeIndex: Int = 0
    ): DirectionsRoute {
        val responseJson = javaClass.classLoader?.getResourceAsStream(fileName)
            ?.bufferedReader()
            ?.use { it.readText() }!!
        return DirectionsResponse.fromJson(responseJson).routes()[routeIndex]
    }
}
