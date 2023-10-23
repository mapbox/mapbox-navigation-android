package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.factories.TestSDKRouteParser
import com.mapbox.navigation.testing.factories.createClosure
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createManeuver
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.factories.createWaypoint
import com.mapbox.navigator.RouterOrigin
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL

class RouteProgressExTest {

    @Test
    fun `update Navigation route`() {
        val navigationRoute = provideNavigationRoute(addLeg = true, distance = 88.0)
        val updated = navigationRoute.update(
            {
                assertEquals(88.0, distance())
                toBuilder().distance(73483.0).build()
            },
            {
                message("new message")
            }
        )
        assertEquals(73483.0, updated.directionsRoute.distance())
        assertEquals("new message", updated.directionsResponse.message())
    }

    @Test
    fun `route refresh updates route durations`() {
        val sourceRoute = createNavigationRouteFromResource(
            "3-steps-route-directions-response.json",
            "3-steps-route-directions-request-url.txt"
        )

        val refreshedRoute = sourceRoute.refreshRoute(
            initialLegIndex = 0,
            currentLegGeometryIndex = 0,
            legAnnotations = listOf(
                createRouteLegAnnotation(
                    duration = listOf(
                        4.548,
                        4.555,
                        4.512,
                        3.841,
                        15.415,
                        1.507,
                        6.359
                    )
                )
            ),
            incidents = null,
            closures = null,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )

        assertEquals(
            40.736999999999995,
            refreshedRoute.directionsRoute.duration(),
            0.00001
        )
        val firstLeg = refreshedRoute.directionsRoute.legs()!!.first()!!
        assertEquals(
            40.736999999999995,
            firstLeg.duration() ?: -1.0,
            0.00001
        )
        val steps = firstLeg.steps()!!
        assertEquals(
            34.37799999999999,
            steps[0].duration(),
            0.00001
        )
        assertEquals(
            6.359,
            steps[1].duration(),
            0.00001
        )
        assertEquals(
            0.0,
            steps[2].duration(),
            0.00001
        )
    }

    @Test
    fun `route refresh without duration annotation doesn't affect durations`() {
        val sourceRoute = createNavigationRouteFromResource(
            "3-steps-route-directions-response.json",
            "3-steps-route-directions-request-url.txt"
        )
            .update({
                toBuilder()
                    .legs(
                        legs()?.map {
                            it.toBuilder()
                                .annotation(
                                    it.annotation()
                                        ?.toBuilder()
                                        ?.duration(null)
                                        ?.congestionNumeric(MutableList(7) { 1 })
                                        ?.build()
                                )
                                .build()
                        }
                    )
                    .routeOptions(
                        routeOptions()?.toBuilder()
                            ?.annotations(DirectionsCriteria.ANNOTATION_CONGESTION)
                            ?.build()
                    )
                    .build()
            }, { this }, null)

        val refreshedRoute = sourceRoute.refreshRoute(
            initialLegIndex = 0,
            currentLegGeometryIndex = 0,
            legAnnotations = listOf(
                createRouteLegAnnotation(
                    congestionNumeric = MutableList(7) { 2 }
                )
            ),
            incidents = null,
            closures = null,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )

        // compare durations with original values from json file
        assertEquals(
            41.882,
            refreshedRoute.directionsRoute.duration(),
            0.00001
        )
        val firstLeg = refreshedRoute.directionsRoute.legs()!!.first()!!
        assertEquals(
            41.882,
            firstLeg.duration()!!,
            0.00001
        )
        val steps = firstLeg.steps()!!
        assertEquals(
            34.341,
            steps[0].duration(),
            0.00001
        )
        assertEquals(
            7.541,
            steps[1].duration(),
            0.00001
        )
        assertEquals(
            0.0,
            steps[2].duration(),
            0.00001
        )
    }

    @Test
    fun `route refresh refreshed durations on second leg`() {
        // uses polyline instead of polyline6
        val sourceRoute = createNavigationRouteFromResource(
            "6-steps-3-waypoints-directions-response.json",
            "6-steps-3-waypoints-directions-request-url.txt"
        )

        val refreshedRoute = sourceRoute.refreshRoute(
            initialLegIndex = 1,
            currentLegGeometryIndex = 0,
            legAnnotations = listOf(
                LegAnnotation.builder().build(),
                createRouteLegAnnotation(
                    duration = MutableList(4) { 1.0 }
                )
            ),
            incidents = null,
            closures = null,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )

        assertEquals(
            45.882,
            refreshedRoute.directionsRoute.duration(),
            0.00001
        )
        val secondLeg = refreshedRoute.directionsRoute.legs()!![1]
        assertEquals(
            4.0,
            secondLeg.duration()!!,
            0.00001
        )
        val steps = secondLeg.steps()!!
        assertEquals(
            1.0,
            steps[0].duration(),
            0.00001
        )
        assertEquals(
            3.0,
            steps[1].duration(),
            0.00001
        )
        assertEquals(
            0.0,
            steps[2].duration(),
            0.00001
        )
    }

    @Test
    fun `extension NavigationRoute refreshRoute`() {
        listOf(
            TestData(
                "update to null items",
                provideNavigationRoute(addLeg = false),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null, null, 0, null, null),
                LegItemsResult(
                    listOf(null),
                    listOf(null),
                    listOf(null),
                    null,
                    null,
                    0,
                )
            ),
            TestData(
                "update to null items multi-leg route",
                provideNavigationRoute(addLeg = true),
                RefreshLegItemsWrapper(
                    0,
                    listOf(null, null),
                    listOf(null, null),
                    null,
                    null,
                    0,
                    null,
                    null
                ),
                LegItemsResult(
                    listOf(null, null),
                    listOf(null, null),
                    listOf(null, null),
                    null,
                    null,
                    0
                )
            ),
            TestData(
                "update to null items multi-leg route starting with second leg",
                provideNavigationRoute(addLeg = true),
                RefreshLegItemsWrapper(
                    1,
                    listOf(null, null),
                    listOf(null, null),
                    null,
                    null,
                    0,
                    null,
                    null,
                ),
                LegItemsResult(
                    listOf(provideDefaultLegAnnotation(), null),
                    listOf(provideDefaultIncidents(), null),
                    listOf(provideDefaultClosures(), null),
                    null,
                    null,
                    0
                ),
            ),
            TestData(
                "update expiration time from non-null with null refresh ttl",
                provideNavigationRoute(addLeg = false, expirationTime = 5),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null, null, 0, null, null),
                LegItemsResult(
                    listOf(null),
                    listOf(null),
                    listOf(null),
                    null,
                    5,
                    0,
                )
            ),
            TestData(
                "update expiration time from null with null refresh ttl",
                provideNavigationRoute(addLeg = false),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null, null, 0, null, null),
                LegItemsResult(
                    listOf(null),
                    listOf(null),
                    listOf(null),
                    null,
                    null,
                    0,
                )
            ),
            TestData(
                "update expiration time from null to non-null",
                provideNavigationRoute(addLeg = false),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null, null, 5, 10, null),
                LegItemsResult(
                    listOf(null),
                    listOf(null),
                    listOf(null),
                    null,
                    15,
                    0,
                )
            ),
            TestData(
                "update expiration time from non-null with non-null refresh ttl",
                provideNavigationRoute(addLeg = false, expirationTime = 5),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null, null, 3, 9, null),
                LegItemsResult(
                    listOf(null),
                    listOf(null),
                    listOf(null),
                    null,
                    12,
                    0,
                )
            ),
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2)
                )
                val newLegAnnotations = createRouteLegAnnotation()
                val newIncidents = listOf(
                    createIncident(startGeometryIndex = 0, endGeometryIndex = 1)
                )
                val newClosures = listOf(createClosure(10, 15))
                val expectedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )
                return@run TestData(
                    "update items route",
                    provideNavigationRoute(addLeg = false, dirWaypoints = provideWaypoints()),
                    RefreshLegItemsWrapper(
                        0,
                        listOf(newLegAnnotations),
                        listOf(newIncidents),
                        listOf(newClosures),
                        refreshedWaypoints,
                        0,
                        null,
                        legGeometryIndex = null,
                    ),
                    LegItemsResult(
                        listOf(newLegAnnotations),
                        listOf(newIncidents),
                        listOf(newClosures),
                        expectedWaypoints,
                        null,
                        0,
                    )
                )
            },
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2)
                )

                val newLegAnnotations = createRouteLegAnnotation()
                val newLegAnnotations2 = createRouteLegAnnotation()
                val newIncidents = listOf(
                    createIncident(startGeometryIndex = 0, endGeometryIndex = 1),
                    createIncident(startGeometryIndex = 10, endGeometryIndex = 15)
                )
                val newIncidents2 = listOf(
                    createIncident(startGeometryIndex = 0, endGeometryIndex = 1),
                    createIncident(startGeometryIndex = 5, endGeometryIndex = 7)
                )
                val newClosures = listOf(createClosure(0, 3), createClosure(6, 7))
                val newClosures2 = listOf(createClosure(4, 7), createClosure(14, 17))
                val expectedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )
                TestData(
                    "update items multi-leg route",
                    provideNavigationRoute(addLeg = true, dirWaypoints = provideWaypoints()),
                    RefreshLegItemsWrapper(
                        0,
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2),
                        listOf(newClosures, newClosures2),
                        refreshedWaypoints,
                        0,
                        null,
                        null,
                    ),
                    LegItemsResult(
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2),
                        listOf(newClosures, newClosures2),
                        expectedWaypoints,
                        null,
                        0,
                    )
                )
            },
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2)
                )
                val newLegAnnotations = createRouteLegAnnotation()
                val newLegAnnotations2 = createRouteLegAnnotation()
                val newInputIncidents = listOf(
                    createIncident(startGeometryIndex = 2, endGeometryIndex = 4)
                )
                val newOutputIncidents = listOf(
                    createIncident(startGeometryIndex = 4, endGeometryIndex = 6)
                )
                val newInputIncidents2 = listOf(
                    createIncident(startGeometryIndex = 6, endGeometryIndex = 9)
                )
                val newOutputIncidents2 = listOf(
                    createIncident(startGeometryIndex = 6, endGeometryIndex = 9)
                )
                val newInputClosures = listOf(createClosure(3, 4))
                val newOutputClosures = listOf(createClosure(5, 6))
                val newInputClosures2 = listOf(createClosure(1, 2))
                val newOutputClosures2 = listOf(createClosure(1, 2))
                val expectedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )
                TestData(
                    "update items multi-leg route, geometryIndex is 2",
                    provideNavigationRoute(addLeg = true, dirWaypoints = provideWaypoints()),
                    RefreshLegItemsWrapper(
                        0,
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newInputIncidents, newInputIncidents2),
                        listOf(newInputClosures, newInputClosures2),
                        refreshedWaypoints,
                        0,
                        null,
                        2,
                    ),
                    LegItemsResult(
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newOutputIncidents, newOutputIncidents2),
                        listOf(newOutputClosures, newOutputClosures2),
                        expectedWaypoints,
                        null,
                        2,
                    )
                )
            },
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2)
                )
                val newLegAnnotations = createRouteLegAnnotation()
                val newLegAnnotations2 = createRouteLegAnnotation()
                val newIncidents = listOf(
                    createIncident(startGeometryIndex = 10, endGeometryIndex = 12)
                )
                val newIncidents2 = listOf(
                    createIncident(startGeometryIndex = 40, endGeometryIndex = 50)
                )
                val newClosures = listOf(createClosure(13, 17))
                val newClosures2 = listOf(createClosure(2, 6))
                val expectedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )
                TestData(
                    "update items multi-leg route starting with second leg",
                    provideNavigationRoute(addLeg = true, dirWaypoints = provideWaypoints()),
                    RefreshLegItemsWrapper(
                        1,
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2),
                        listOf(newClosures, newClosures2),
                        refreshedWaypoints,
                        0,
                        null,
                        null,
                    ),
                    LegItemsResult(
                        listOf(provideDefaultLegAnnotation(), newLegAnnotations2),
                        listOf(provideDefaultIncidents(), newIncidents2),
                        listOf(provideDefaultClosures(), newClosures2),
                        expectedWaypoints,
                        null,
                        0
                    )
                )
            },
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2)
                )
                val newLegAnnotations = createRouteLegAnnotation()
                val newLegAnnotations2 = createRouteLegAnnotation()
                val newIncidents = listOf(
                    createIncident(startGeometryIndex = 10, endGeometryIndex = 12)
                )
                val newInputIncidents2 = listOf(
                    createIncident(startGeometryIndex = 40, endGeometryIndex = 50)
                )
                val newOutputIncidents2 = listOf(
                    createIncident(startGeometryIndex = 44, endGeometryIndex = 54)
                )
                val newClosures = listOf(createClosure(13, 17))
                val newInputClosures2 = listOf(createClosure(2, 6))
                val newOutputClosures2 = listOf(createClosure(6, 10))
                val expectedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )
                TestData(
                    "update items multi-leg route starting with second leg, " +
                        "geometryIndex = 4",
                    provideNavigationRoute(addLeg = true, dirWaypoints = provideWaypoints()),
                    RefreshLegItemsWrapper(
                        1,
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newInputIncidents2),
                        listOf(newClosures, newInputClosures2),
                        refreshedWaypoints,
                        0,
                        null,
                        4,
                    ),
                    LegItemsResult(
                        listOf(provideDefaultLegAnnotation(), newLegAnnotations2),
                        listOf(provideDefaultIncidents(), newOutputIncidents2),
                        listOf(provideDefaultClosures(), newOutputClosures2),
                        expectedWaypoints,
                        null,
                        4,
                    )
                )
            },
        ).forEach { (description, navRoute, refreshItems, result) ->
            mockkObject(AnnotationsRefresher) {
                every {
                    AnnotationsRefresher.getRefreshedAnnotations(any(), any(), any())
                } returnsMany
                    (result.newLegAnnotation?.drop(refreshItems.startWithIndex) ?: emptyList())
                val updatedNavRoute = try {
                    navRoute.refreshRoute(
                        refreshItems.startWithIndex,
                        refreshItems.legGeometryIndex,
                        refreshItems.legAnnotation,
                        refreshItems.incidents,
                        refreshItems.closures,
                        refreshItems.waypoints,
                        refreshItems.responseTime,
                        refreshItems.refreshTtl,
                    )
                } catch (t: Throwable) {
                    throw Throwable("unhandled exception in $description", t)
                }

                assertEquals(
                    description,
                    result.newLegAnnotation,
                    updatedNavRoute.directionsRoute
                        .legs()
                        ?.map { it.annotation() },
                )
                assertEquals(
                    description,
                    result.newIncidents,
                    updatedNavRoute.directionsRoute
                        .legs()
                        ?.map { it.incidents() },
                )
                assertEquals(
                    description,
                    result.newClosures,
                    updatedNavRoute.directionsRoute
                        .legs()
                        ?.map { it.closures() },
                )
                assertEquals(
                    description,
                    result.newWaypoints,
                    updatedNavRoute.directionsResponse.waypoints()
                )
                assertEquals(
                    description,
                    navRoute.directionsResponse.waypoints()?.size,
                    updatedNavRoute.directionsResponse.waypoints()?.size
                )
                assertEquals(
                    description,
                    navRoute.unavoidableClosures,
                    updatedNavRoute.unavoidableClosures
                )

                val capturedOldAnnotations = mutableListOf<LegAnnotation?>()
                val capturedNewAnnotations = mutableListOf<LegAnnotation?>()
                val capturedLegGeometryIndices = mutableListOf<Int>()
                verify {
                    AnnotationsRefresher.getRefreshedAnnotations(
                        captureNullable(capturedOldAnnotations),
                        captureNullable(capturedNewAnnotations),
                        capture(capturedLegGeometryIndices)
                    )
                }
                assertEquals(
                    description,
                    navRoute.directionsRoute.legs()
                        ?.drop(refreshItems.startWithIndex)
                        ?.map { it.annotation() },
                    capturedOldAnnotations
                )
                assertEquals(
                    description,
                    refreshItems.legAnnotation?.drop(refreshItems.startWithIndex),
                    capturedNewAnnotations
                )
                assertEquals(
                    description,
                    listOf(result.expectedLegGeometryIndex) +
                        List(capturedLegGeometryIndices.size - 1) { 0 },
                    capturedLegGeometryIndices
                )
            }
        }
    }

    private fun provideNavigationRoute(
        annotations: LegAnnotation? = provideDefaultLegAnnotation(),
        incidents: List<Incident>? = provideDefaultIncidents(),
        closures: List<Closure>? = provideDefaultClosures(),
        dirWaypoints: List<DirectionsWaypoint>? = null,
        addLeg: Boolean,
        distance: Double = 10.0,
        expirationTime: Long? = null,
    ): NavigationRoute {
        val twoPointGeometry = PolylineUtils.encode(
            listOf(
                Point.fromLngLat(1.2, 3.4),
                Point.fromLngLat(3.3, 6.7)
            ),
            5
        )
        val validStep = LegStep.builder()
            .geometry(twoPointGeometry)
            .distance(1.0)
            .duration(2.0)
            .weight(3.0)
            .mode("mode")
            .maneuver(createManeuver())
            .build()
        return com.mapbox.navigation.testing.factories.createNavigationRoutes(
            DirectionsResponse.builder()
                .waypoints(dirWaypoints)
                .routes(listOf(DirectionsRoute.builder()
                    .duration(10.0)
                    .distance(distance)
                    .legs(
                        mutableListOf(
                            RouteLeg.builder()
                                .annotation(annotations)
                                .incidents(incidents)
                                .closures(closures)
                                .steps(List(2) { validStep })
                                .build()
                        ).apply {
                            if (addLeg) {
                                add(
                                    RouteLeg.builder()
                                        .annotation(annotations)
                                        .incidents(incidents)
                                        .closures(closures)
                                        .steps(List(2) { validStep })
                                        .build()
                                )
                            }
                        }
                    )
                    .geometry(
                        PolylineUtils.encode(
                            listOf(
                                Point.fromLngLat(11.22, 33.44),
                                Point.fromLngLat(23.34, 34.45)
                            ),
                            5
                        )
                    )
                    .build()))
                .code("Ok")
                .build(),
            createRouteOptions(
                waypointsPerRoute = false,
                geometries = DirectionsCriteria.GEOMETRY_POLYLINE6
            ),
            com.mapbox.navigation.base.route.RouterOrigin.Offboard,
            expirationTime,
        ).first()
    }

    @RunWith(Parameterized::class)
    class UpdateWaypointsTest(
        private val description: String,
        private val inputWaypoints: List<DirectionsWaypoint>?,
        private val refreshedWaypoints: List<DirectionsWaypoint>?,
        private val expectedWaypoints: List<DirectionsWaypoint>?
    ) {

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data(): Collection<Array<Any?>> {
                return listOf(
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val waypoints = listOf(
                            createWaypoint("name11", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name22", unrecognizedProperties = inputMetadata2)
                        )
                        arrayOf(
                            "update waypoints from null to non-null",
                            null,
                            waypoints,
                            null,
                        )
                    },
                    run {
                        val waypoints = listOf(
                            createWaypoint("name11"),
                            createWaypoint("name22")
                        )
                        arrayOf(
                            "update waypoints from empty to non-empty",
                            emptyList<DirectionsWaypoint>(),
                            waypoints,
                            emptyList<DirectionsWaypoint>()
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val waypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2)
                        )
                        arrayOf(
                            "update waypoints from non-empty to null",
                            waypoints,
                            null,
                            waypoints,
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val waypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2)
                        )
                        arrayOf(
                            "update waypoints from non-empty to empty",
                            waypoints,
                            emptyList<DirectionsWaypoint>(),
                            waypoints,
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val refreshedMetadata1 = mapOf("key3" to JsonPrimitive("value3"))
                        val refreshedWaypoints = listOf(
                            createWaypoint("name3", unrecognizedProperties = refreshedMetadata1)
                        )

                        val inputWaypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2)
                        )
                        arrayOf(
                            "update waypoints from 2 to 1",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint(
                                    "name3",
                                    unrecognizedProperties = refreshedMetadata1
                                ),
                                createWaypoint("name2", unrecognizedProperties = inputMetadata2)
                            ),
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val refreshedMetadata1 = mapOf("key3" to JsonPrimitive("value3"))
                        val refreshedMetadata2 = mapOf("key4" to JsonPrimitive("value4"))

                        val refreshedWaypoints = listOf(
                            createWaypoint(
                                "name3",
                                unrecognizedProperties = refreshedMetadata1
                            ),
                            createWaypoint(
                                "name4",
                                unrecognizedProperties = refreshedMetadata2
                            )
                        )

                        val inputWaypoints = listOf(
                            createWaypoint(
                                "name1",
                                unrecognizedProperties = inputMetadata1
                            ),
                            createWaypoint(
                                "name2",
                                unrecognizedProperties = inputMetadata2
                            )
                        )
                        arrayOf(
                            "update waypoints from 2 to 2",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint(
                                    "name3",
                                    unrecognizedProperties = refreshedMetadata1
                                ),
                                createWaypoint(
                                    "name4",
                                    unrecognizedProperties = refreshedMetadata2
                                )
                            ),
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val refreshedMetadata1 = mapOf("key3" to JsonPrimitive("value3"))
                        val refreshedMetadata2 = mapOf("key4" to JsonPrimitive("value4"))
                        val refreshedMetadata3 = mapOf("key5" to JsonPrimitive("value5"))

                        val refreshedWaypoints = listOf(
                            createWaypoint("name3", unrecognizedProperties = refreshedMetadata1),
                            createWaypoint("name4", unrecognizedProperties = refreshedMetadata2),
                            createWaypoint("name5", unrecognizedProperties = refreshedMetadata3),
                        )

                        val inputWaypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2)
                        )
                        arrayOf(
                            "update waypoints from 2 to 3",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint(
                                    "name3",
                                    unrecognizedProperties = refreshedMetadata1
                                ),
                                createWaypoint(
                                    "name4",
                                    unrecognizedProperties = refreshedMetadata2
                                )
                            ),
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val refreshedMetadata2 = mapOf("key4" to JsonPrimitive("value4"))

                        val refreshedWaypoints = listOf(
                            null,
                            createWaypoint("name3", unrecognizedProperties = refreshedMetadata2)
                        )

                        val inputWaypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2)
                        )
                        arrayOf(
                            "update waypoints from 2 to null + 1",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                                createWaypoint("name3", unrecognizedProperties = refreshedMetadata2)
                            ),
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val refreshedMetadata2 = mapOf("key4" to JsonPrimitive("value4"))

                        val refreshedWaypoints = listOf(
                            createWaypoint("name3", unrecognizedProperties = null),
                            createWaypoint("name4", unrecognizedProperties = refreshedMetadata2)
                        )

                        val inputWaypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2)
                        )
                        arrayOf(
                            "update waypoints from 2 to null metadata + 1",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint("name3", unrecognizedProperties = null),
                                createWaypoint("name4", unrecognizedProperties = refreshedMetadata2)
                            ),
                        )
                    },
                )
            }
        }

        @Test
        fun refreshResponseWaypoints() {
            val inputRoute = provideNavigationRoute(
                dirWaypoints = inputWaypoints,
                addLeg = true,
                waypointsPerRoute = false,
            )
            val updatedRoute = inputRoute.refreshRoute(
                0,
                0,
                null,
                null,
                null,
                refreshedWaypoints,
                0,
                null
            )
            assertEquals(expectedWaypoints, updatedRoute.directionsResponse.waypoints())
        }

        @Test
        fun routeWaypoints() {
            val inputRoute = provideNavigationRoute(
                routeWaypoints = inputWaypoints,
                addLeg = true,
                waypointsPerRoute = true,
            )
            val updatedRoute = inputRoute.refreshRoute(
                0,
                0,
                null,
                null,
                null,
                refreshedWaypoints,
                0,
                null
            )
            assertEquals(expectedWaypoints, updatedRoute.directionsRoute.waypoints())
        }
    }

    companion object {
        private fun provideNavigationRoute(
            annotations: LegAnnotation? = provideDefaultLegAnnotation(),
            incidents: List<Incident>? = provideDefaultIncidents(),
            closures: List<Closure>? = provideDefaultClosures(),
            routeWaypoints: List<DirectionsWaypoint>? = null,
            dirWaypoints: List<DirectionsWaypoint>? = null,
            addLeg: Boolean,
            distance: Double = 10.0,
            waypointsPerRoute: Boolean = false,
        ): NavigationRoute {
            val twoPointGeometry = PolylineUtils.encode(
                listOf(
                    Point.fromLngLat(1.2, 3.4),
                    Point.fromLngLat(3.3, 6.7)
                ),
                5
            )
            val validStep = LegStep.builder()
                .geometry(twoPointGeometry)
                .distance(1.0)
                .duration(2.0)
                .weight(3.0)
                .mode("mode")
                .maneuver(mockk())
                .build()
            return NavigationRoute(
                DirectionsResponse.builder()
                    .routes(emptyList())
                    .waypoints(dirWaypoints)
                    .code("Ok")
                    .build(),
                0,
                mockk {
                    every { geometries() } returns DirectionsCriteria.GEOMETRY_POLYLINE
                    every { waypointsPerRoute() } returns waypointsPerRoute
                },
                DirectionsRoute.builder()
                    .duration(10.0)
                    .distance(distance)
                    .waypoints(routeWaypoints)
                    .legs(
                        mutableListOf(
                            RouteLeg.builder()
                                .annotation(annotations)
                                .incidents(incidents)
                                .closures(closures)
                                .steps(List(2) { validStep })
                                .build()
                        ).apply {
                            if (addLeg) {
                                add(
                                    RouteLeg.builder()
                                        .annotation(annotations)
                                        .incidents(incidents)
                                        .closures(closures)
                                        .steps(List(2) { validStep })
                                        .build()
                                )
                            }
                        }
                    )
                    .geometry(
                        PolylineUtils.encode(
                            listOf(
                                Point.fromLngLat(11.22, 33.44),
                                Point.fromLngLat(23.34, 34.45)
                            ),
                            5
                        )
                    )
                    .build(),
                mockk {
                    every { routeInfo } returns mockk(relaxed = true)
                    every { routeId } returns ""
                    every { routerOrigin } returns RouterOrigin.ONLINE
                    every { waypoints } returns emptyList()
                },
                expirationTimeElapsedSeconds = null
            )
        }

        private fun provideDefaultLegAnnotation(): LegAnnotation = LegAnnotation.builder()
            .congestion(listOf("congestion1, congestion2"))
            .distance(listOf(0.0, 0.1))
            .build()

        private fun provideDefaultIncidents(): List<Incident> = listOf(
            Incident.builder()
                .id("0")
                .description("description1")
                .build(),
            Incident.builder()
                .id("2")
                .description("description2")
                .build(),
        )

        private fun provideDefaultClosures(): List<Closure> = listOf(
            Closure.builder()
                .geometryIndexStart(0)
                .geometryIndexEnd(5)
                .build(),
            Closure.builder()
                .geometryIndexStart(10)
                .geometryIndexEnd(12)
                .build(),
        )

        private fun provideWaypoints(): List<DirectionsWaypoint> = listOf(
            createWaypoint(
                "name1",
                unrecognizedProperties = mapOf("some key 1" to JsonPrimitive("some value 1"))
            ),
            createWaypoint(
                "name2",
                unrecognizedProperties = mapOf("some key 2" to JsonPrimitive("some value 2"))
            )
        )
    }

    /**
     * Wrapper of test case
     *
     * @param testDescription short description of what is tested
     * @param navigationRoute initial navigation route that will be refreshed
     * @param refreshLegItemsWrapper refreshed data, which should be apply to the route
     * @param legResult result items of refreshed route
     */
    private data class TestData(
        val testDescription: String,
        val navigationRoute: NavigationRoute,
        val refreshLegItemsWrapper: RefreshLegItemsWrapper,
        val legResult: LegItemsResult,
    )

    /**
     * Items wrapper that needs to refresh route
     */
    private data class RefreshLegItemsWrapper(
        val startWithIndex: Int,
        val legAnnotation: List<LegAnnotation?>?,
        val incidents: List<List<Incident>?>?,
        val closures: List<List<Closure>?>?,
        val waypoints: List<DirectionsWaypoint?>?,
        val responseTime: Long,
        val refreshTtl: Int?,
        val legGeometryIndex: Int?,
    )

    /**
     * Expecting result items after route is refreshed
     */
    private data class LegItemsResult(
        val newLegAnnotation: List<LegAnnotation?>?,
        val newIncidents: List<List<Incident>?>?,
        val newClosures: List<List<Closure>?>?,
        val newWaypoints: List<DirectionsWaypoint>?,
        val expectedExpirationTime: Long?,
        val expectedLegGeometryIndex: Int,
    )
}

private fun createNavigationRouteFromResource(
    responseFileName: String,
    requestFileName: String
) = createNavigationRoutes(
    DirectionsResponse.fromJson(
        FileUtils.loadJsonFixture(responseFileName)
    ),
    RouteOptions.fromUrl(
        URL(
            FileUtils.loadJsonFixture(requestFileName)
        )
    ),
    TestSDKRouteParser(),
    com.mapbox.navigation.base.route.RouterOrigin.Offboard,
    null
).first()
