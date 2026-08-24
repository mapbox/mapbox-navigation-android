package com.mapbox.navigation.core.routeoptions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.WaypointFactory

object RouteOptionsUpdaterTestUtils {

    const val DEFAULT_REROUTE_BEARING_ANGLE = 11.0
    const val DEFAULT_REROUTE_BEARING_TOLERANCE = 90.0
    const val DEFAULT_Z_LEVEL = 3

    val COORDINATE_1: Point = Point.fromLngLat(1.0, 1.0)
    val COORDINATE_2: Point = Point.fromLngLat(2.0, 2.0)
    val COORDINATE_3: Point = Point.fromLngLat(3.0, 3.0)
    val COORDINATE_4: Point = Point.fromLngLat(4.0, 4.0)

    fun provideRouteOptionsWithCoordinates() =
        provideDefaultRouteOptionsBuilder()
            .coordinatesList(listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3, COORDINATE_4))
            .build()

    fun provideRouteOptionsWithCoordinatesAndBearings() =
        provideRouteOptionsWithCoordinates()
            .toBuilder()
            .bearingsList(
                listOf(
                    Bearing.builder().angle(10.0).degrees(10.0).build(),
                    Bearing.builder().angle(20.0).degrees(20.0).build(),
                    Bearing.builder().angle(30.0).degrees(30.0).build(),
                    Bearing.builder().angle(40.0).degrees(40.0).build(),
                ),
            )
            .build()

    fun provideRouteOptionsWithCoordinatesAndArriveByDepartAt() =
        provideRouteOptionsWithCoordinates()
            .toBuilder()
            .arriveBy("2021-01-01'T'01:01")
            .departAt("2021-02-02'T'02:02")
            .build()

    fun provideRouteOptionsWithCoordinatesAndLayers() =
        provideRouteOptionsWithCoordinates()
            .toBuilder()
            .layersList(listOf(0, 1, 2, 4))
            .build()

    private fun provideDefaultRouteOptionsBuilder() =
        RouteOptions.builder()
            .baseUrl(Constants.BASE_API_URL)
            .user(Constants.MAPBOX_USER)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .annotationsList(
                listOf(
                    DirectionsCriteria.ANNOTATION_SPEED,
                    DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
                ),
            )
            .coordinatesList(emptyList())
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .alternatives(true)
            .steps(true)
            .bannerInstructions(true)
            .continueStraight(true)
            .exclude(DirectionsCriteria.EXCLUDE_TOLL)
            .language("en")
            .roundaboutExits(true)
            .voiceInstructions(true)

    fun provideDefaultWaypointsList(): List<Waypoint> =
        listOf(
            WaypointFactory.provideWaypoint(
                Point.fromLngLat(1.0, 1.0),
                "",
                null,
                Waypoint.REGULAR,
                emptyMap(),
            ),
            WaypointFactory.provideWaypoint(
                Point.fromLngLat(2.0, 2.0),
                "",
                null,
                Waypoint.REGULAR,
                emptyMap(),
            ),
            WaypointFactory.provideWaypoint(
                Point.fromLngLat(3.0, 3.0),
                "",
                null,
                Waypoint.REGULAR,
                emptyMap(),
            ),
            WaypointFactory.provideWaypoint(
                Point.fromLngLat(4.0, 4.0),
                "",
                null,
                Waypoint.REGULAR,
                emptyMap(),
            ),
        )
}
