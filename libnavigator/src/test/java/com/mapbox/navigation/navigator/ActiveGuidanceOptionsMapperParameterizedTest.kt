package com.mapbox.navigation.navigator

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.navigator.internal.ActiveGuidanceOptionsMapper
import com.mapbox.navigator.ActiveGuidanceGeometryEncoding
import com.mapbox.navigator.ActiveGuidanceMode
import com.mapbox.navigator.ActiveGuidanceOptions
import com.mapbox.navigator.Waypoint
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ActiveGuidanceOptionsMapperParameterizedTest(
    private val profile: String,
    private val geometries: String,
    private val waypointIndicesList: List<Int>?,
    private val coordinates: List<Point>,
    private val activeGuidanceMode: ActiveGuidanceMode,
    private val activeGuidanceGeometryEncoding: ActiveGuidanceGeometryEncoding,
    private val activeGuidanceWaypoints: List<Waypoint>
) {

    companion object {

        private val coordinatesList = mutableListOf<Point>().apply {
            add(Point.fromLngLat(1.234, 5.678))
            add(Point.fromLngLat(2.345, 6.789))
            add(Point.fromLngLat(3.456, 7.891))
            add(Point.fromLngLat(4.567, 8.912))
            add(Point.fromLngLat(5.678, 9.123))
        }

        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(
                DirectionsCriteria.PROFILE_DRIVING,
                DirectionsCriteria.GEOMETRY_POLYLINE,
                listOf(0, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE5,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_DRIVING,
                DirectionsCriteria.GEOMETRY_POLYLINE6,
                listOf(0, 1, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), false),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_DRIVING,
                "geojson",
                listOf(0, 2, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.GEO_JSON,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), false),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_DRIVING,
                "unrecognized",
                listOf(0, 3, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), false),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                "unrecognized",
                DirectionsCriteria.GEOMETRY_POLYLINE,
                listOf(0, 1, 2, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE5,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), false),
                    Waypoint(Point.fromLngLat(3.456, 7.891), false),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                "unrecognized",
                DirectionsCriteria.GEOMETRY_POLYLINE6,
                listOf(0, 1, 3, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), false),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), false),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                "unrecognized",
                "geojson",
                listOf(0, 2, 3, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.GEO_JSON,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), false),
                    Waypoint(Point.fromLngLat(4.567, 8.912), false),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                "unrecognized",
                "unrecognized",
                listOf(0, 1, 2, 3, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), false),
                    Waypoint(Point.fromLngLat(3.456, 7.891), false),
                    Waypoint(Point.fromLngLat(4.567, 8.912), false),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                DirectionsCriteria.GEOMETRY_POLYLINE,
                listOf(0, 1, 2, 3, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE5,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), false),
                    Waypoint(Point.fromLngLat(3.456, 7.891), false),
                    Waypoint(Point.fromLngLat(4.567, 8.912), false),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                DirectionsCriteria.GEOMETRY_POLYLINE6,
                listOf(0, 2, 3, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), false),
                    Waypoint(Point.fromLngLat(4.567, 8.912), false),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_DRIVING,
                "geojson",
                listOf(0, 1, 3, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.GEO_JSON,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), false),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), false),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_DRIVING,
                "unrecognized",
                listOf(0, 1, 2, 4),
                coordinatesList,
                ActiveGuidanceMode.DRIVING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), false),
                    Waypoint(Point.fromLngLat(3.456, 7.891), false),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_WALKING,
                DirectionsCriteria.GEOMETRY_POLYLINE,
                listOf(0, 3, 4),
                coordinatesList,
                ActiveGuidanceMode.WALKING,
                ActiveGuidanceGeometryEncoding.POLYLINE5,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), false),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_WALKING,
                DirectionsCriteria.GEOMETRY_POLYLINE6,
                listOf(0, 2, 4),
                coordinatesList,
                ActiveGuidanceMode.WALKING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), false),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_WALKING,
                "geojson",
                listOf(0, 1, 4),
                coordinatesList,
                ActiveGuidanceMode.WALKING,
                ActiveGuidanceGeometryEncoding.GEO_JSON,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), false),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_WALKING,
                "unrecognized",
                listOf(0, 4),
                coordinatesList,
                ActiveGuidanceMode.WALKING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_CYCLING,
                DirectionsCriteria.GEOMETRY_POLYLINE,
                listOf(0, 4),
                coordinatesList,
                ActiveGuidanceMode.CYCLING,
                ActiveGuidanceGeometryEncoding.POLYLINE5,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_CYCLING,
                DirectionsCriteria.GEOMETRY_POLYLINE6,
                listOf(0, 1, 4),
                coordinatesList,
                ActiveGuidanceMode.CYCLING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), false),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_CYCLING,
                "geojson",
                listOf(0, 2, 4),
                coordinatesList,
                ActiveGuidanceMode.CYCLING,
                ActiveGuidanceGeometryEncoding.GEO_JSON,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), false),
                    Waypoint(Point.fromLngLat(4.567, 8.912), true),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            ),
            arrayOf(
                DirectionsCriteria.PROFILE_CYCLING,
                "unrecognized",
                listOf(0, 3, 4),
                coordinatesList,
                ActiveGuidanceMode.CYCLING,
                ActiveGuidanceGeometryEncoding.POLYLINE6,
                listOf(
                    Waypoint(Point.fromLngLat(1.234, 5.678), false),
                    Waypoint(Point.fromLngLat(2.345, 6.789), true),
                    Waypoint(Point.fromLngLat(3.456, 7.891), true),
                    Waypoint(Point.fromLngLat(4.567, 8.912), false),
                    Waypoint(Point.fromLngLat(5.678, 9.123), false)
                )
            )
        )
    }

    @Test
    fun checksActiveGuidanceOptionMapperWithNonNullInputValues() {
        val routeOptions: RouteOptions = mockk()
        every { routeOptions.profile() } returns profile
        every { routeOptions.geometries() } returns geometries
        every { routeOptions.coordinatesList() } returns coordinates
        every { routeOptions.waypointIndicesList() } returns waypointIndicesList
        val directionsRoute: DirectionsRoute = mockk()
        every { directionsRoute.routeOptions() } returns routeOptions

        val drivingPolyline = ActiveGuidanceOptionsMapper.mapFrom(directionsRoute)

        assertEquals(
            ActiveGuidanceOptions(
                activeGuidanceMode,
                activeGuidanceGeometryEncoding,
                activeGuidanceWaypoints
            ),
            drivingPolyline
        )
    }
}
