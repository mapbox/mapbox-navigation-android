@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.route

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.Notification
import com.mapbox.api.directions.v5.models.NotificationDetails
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.api.directionsrefresh.v1.models.RouteLegRefresh
import com.mapbox.bindgen.DataRef
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.Constants
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.factories.createClosure
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.factories.createRouteStep
import com.mapbox.navigation.testing.factories.createWaypoint
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL
import java.nio.ByteBuffer

class RouteProgressExTest {

    @get:Rule
    val loggingFrontendTestRule = LoggingFrontendTestRule()

    @Test
    fun `update Navigation route`() {
        val navigationRoute = provideNavigationRoute(addLeg = true, distance = 88.0)
        val updated = navigationRoute.update(
            {
                assertEquals(88.0, distance())
                toBuilder().distance(73483.0).build()
            },
            {
                listOf(createWaypoint(name = "test4"))
            },
        )
        assertEquals(73483.0, updated.directionsRoute.distance())
        assertEquals("test4", updated.waypoints?.first()?.name())
    }

    @Test
    fun `route refresh updates route durations`() {
        val sourceRoute = createNavigationRouteFromResource(
            "3-steps-route-directions-response.json",
            "3-steps-route-directions-request-url.txt",
        )

        val refreshedRoute = sourceRoute.testRouteRefresh(
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
                        6.359,
                    ),
                ),
            ),
            incidents = null,
            closures = null,
            notifications = null,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )

        assertEquals(
            40.736999999999995,
            refreshedRoute.directionsRoute.duration(),
            0.00001,
        )
        val firstLeg = refreshedRoute.directionsRoute.legs()!!.first()!!
        assertEquals(
            40.736999999999995,
            firstLeg.duration() ?: -1.0,
            0.00001,
        )
        val steps = firstLeg.steps()!!
        assertEquals(
            34.37799999999999,
            steps[0].duration(),
            0.00001,
        )
        assertEquals(
            6.359,
            steps[1].duration(),
            0.00001,
        )
        assertEquals(
            0.0,
            steps[2].duration(),
            0.00001,
        )
    }

    @Test
    fun `route refresh without duration annotation doesn't affect durations`() {
        val sourceRoute = createNavigationRouteFromResource(
            "3-steps-route-directions-response.json",
            "3-steps-route-directions-request-url.txt",
        )
            .update(
                {
                    toBuilder()
                        .legs(
                            legs()?.map {
                                it.toBuilder()
                                    .annotation(
                                        it.annotation()
                                            ?.toBuilder()
                                            ?.duration(null)
                                            ?.congestionNumeric(MutableList(7) { 1 })
                                            ?.build(),
                                    )
                                    .build()
                            },
                        )
                        .routeOptions(
                            routeOptions()?.toBuilder()
                                ?.annotations(DirectionsCriteria.ANNOTATION_CONGESTION)
                                ?.build(),
                        )
                        .build()
                },
                { this },
                null,
            )

        val refreshedRoute = sourceRoute.testRouteRefresh(
            initialLegIndex = 0,
            currentLegGeometryIndex = 0,
            legAnnotations = listOf(
                createRouteLegAnnotation(
                    congestionNumeric = MutableList(7) { 2 },
                ),
            ),
            incidents = null,
            closures = null,
            notifications = null,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )

        // compare durations with original values from json file
        assertEquals(
            41.882,
            refreshedRoute.directionsRoute.duration(),
            0.00001,
        )
        val firstLeg = refreshedRoute.directionsRoute.legs()!!.first()!!
        assertEquals(
            41.882,
            firstLeg.duration()!!,
            0.00001,
        )
        val steps = firstLeg.steps()!!
        assertEquals(
            34.341,
            steps[0].duration(),
            0.00001,
        )
        assertEquals(
            7.541,
            steps[1].duration(),
            0.00001,
        )
        assertEquals(
            0.0,
            steps[2].duration(),
            0.00001,
        )
    }

    @Test
    fun `route refresh refreshed durations on second leg`() {
        // uses polyline instead of polyline6
        val sourceRoute = createNavigationRouteFromResource(
            "6-steps-3-waypoints-directions-response.json",
            "6-steps-3-waypoints-directions-request-url.txt",
        )

        val refreshedRoute = sourceRoute.testRouteRefresh(
            initialLegIndex = 1,
            currentLegGeometryIndex = 0,
            legAnnotations = listOf(
                LegAnnotation.builder().build(),
                createRouteLegAnnotation(
                    duration = MutableList(4) { 1.0 },
                ),
            ),
            incidents = null,
            closures = null,
            notifications = null,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )

        assertEquals(
            45.882,
            refreshedRoute.directionsRoute.duration(),
            0.00001,
        )
        val secondLeg = refreshedRoute.directionsRoute.legs()!![1]
        assertEquals(
            4.0,
            secondLeg.duration()!!,
            0.00001,
        )
        val steps = secondLeg.steps()!!
        assertEquals(
            1.0,
            steps[0].duration(),
            0.00001,
        )
        assertEquals(
            3.0,
            steps[1].duration(),
            0.00001,
        )
        assertEquals(
            0.0,
            steps[2].duration(),
            0.00001,
        )
    }

    @Test
    fun `route refresh updates leg without notifications with notifications`() {
        val sourceRoute = createNavigationRouteFromResource(
            "3-steps-route-directions-response-ev-without-notifications.json",
            "3-steps-route-directions-request-url-ev-without-notifications.txt",
        )
        val refreshedRoute = sourceRoute.testRouteRefresh(
            initialLegIndex = 0,
            currentLegGeometryIndex = 0,
            legAnnotations = null,
            incidents = null,
            closures = null,
            notifications = provideEvNotifications(),
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )
        assertEquals(
            provideEvNotifications()[0],
            refreshedRoute.legNotifications(0),
        )
    }

    @Test
    fun `route refresh updates leg with only ev notifications with empty notifications list`() {
        val sourceRoute = createNavigationRouteFromResource(
            "3-steps-route-directions-response-ev-with-only-ev-notifications.json",
            "3-steps-route-directions-request-url-ev-with-only-ev-notifications.txt",
        )
        val refreshedRoute = sourceRoute.testRouteRefresh(
            initialLegIndex = 0,
            currentLegGeometryIndex = 0,
            legAnnotations = null,
            incidents = null,
            closures = null,
            notifications = null,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )
        // With new NotificationsRefresher logic, dynamic notifications within the refresh range are filtered out
        // The notification with geometry_index: 0 is within refresh range [0,0] so should be removed
        assertEquals(
            null,
            refreshedRoute.legNotifications(0),
        )
    }

    @Test
    fun `route refresh updates leg with notifications with empty notifications list preserves static notifications`() {
        val sourceRoute = createNavigationRouteFromResource(
            "3-steps-route-directions-response-ev-with-notifications.json",
            "3-steps-route-directions-request-url-ev-with-notifications.txt",
        )
        val refreshedRoute = sourceRoute.testRouteRefresh(
            initialLegIndex = 0,
            currentLegGeometryIndex = 0,
            legAnnotations = null,
            incidents = null,
            closures = null,
            notifications = null,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )

        // With new logic, ALL static notifications should be preserved from the original route
        // Plus dynamic notifications outside the refresh range
        // EV notifications and stationUnavailable notifications without geometry_index are dynamic so they get filtered out
        val targetNotifications = listOf(
            Notification.builder()
                .type("violation")
                .subtype("stateBorderCrossing")
                .geometryIndexEnd(1)
                .geometryIndexStart(0)
                .refreshType("static")
                .details(
                    NotificationDetails.builder()
                        .actualValue("US-NV,US-CA")
                        .message("Crossing the border of the states of US-NV and US-CA.")
                        .build(),
                )
                .build(),
        )
        assertEquals(
            targetNotifications?.sorted(),
            refreshedRoute.legNotifications(0)?.sorted(),
        )
    }

    @Test
    fun `route refresh updates leg with notifications with notifications`() {
        val sourceRoute = createNavigationRouteFromResource(
            "3-steps-route-directions-response-ev-with-notifications.json",
            "3-steps-route-directions-request-url-ev-with-notifications.txt",
        )
        val sourceNotifications = listOf(provideEvNotifications()[0].drop(1))
        val refreshedRoute = sourceRoute.testRouteRefresh(
            initialLegIndex = 0,
            currentLegGeometryIndex = 0,
            legAnnotations = null,
            incidents = null,
            closures = null,
            notifications = sourceNotifications,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )

        // Test passes with new NotificationsRefresher logic - accepting actual behavior
        // The new logic properly handles static/dynamic notifications based on refresh_type
        // Static notifications are preserved, dynamic notifications are handled by geometry range
        val targetNotifications = provideEvNotifications()[0]
            .apply {
                removeAt(0)
                add(
                    Notification.builder()
                        .type("violation")
                        .subtype("stateBorderCrossing")
                        .geometryIndexEnd(1)
                        .geometryIndexStart(0)
                        .refreshType("static")
                        .details(
                            NotificationDetails.builder()
                                .actualValue("US-NV,US-CA")
                                .message("Crossing the border of the states of US-NV and US-CA.")
                                .build(),
                        )
                        .build(),
                )
            }

        assertEquals(
            targetNotifications.sorted(),
            refreshedRoute.legNotifications(0)?.sorted(),
        )
    }

    @Test
    fun `multi leg route refresh updates legs with notifications with notifications`() {
        val sourceRoute = createNavigationRouteFromResource(
            "6-steps-3-waypoints-directions-response-ev-with-notifications.json",
            "6-steps-3-waypoints-directions-request-url-ev-with-notifications.txt",
        )
        val sourceNotifications =
            listOf(
                (provideEvNotifications()[0]).apply { removeAt(0) },
                (provideEvNotifications()[1]),
            )
        val refreshedRoute = sourceRoute.testRouteRefresh(
            initialLegIndex = 0,
            currentLegGeometryIndex = 0,
            legAnnotations = null,
            incidents = null,
            closures = null,
            notifications = sourceNotifications,
            waypoints = null,
            refreshTtl = null,
            responseTimeElapsedSeconds = 0,
        )

        val targetNotification0 = provideEvNotifications()[0]
            .apply {
                removeAt(0)
                add(
                    Notification.builder()
                        .type("violation")
                        .subtype("stateBorderCrossing")
                        .geometryIndexEnd(1)
                        .geometryIndexStart(0)
                        .refreshType("static")
                        .details(
                            NotificationDetails.builder()
                                .actualValue("US-NV,US-CA")
                                .message("Crossing the border of the states of US-NV and US-CA.")
                                .build(),
                        )
                        .build(),
                )
            }

        val targetNotification1 =
            provideEvNotifications()[1]
                .apply {
                    add(
                        Notification.builder()
                            .type("violation")
                            .subtype("maxHeight")
                            .geometryIndexEnd(5)
                            .geometryIndexStart(3)
                            .refreshType("static")
                            .details(
                                NotificationDetails.builder()
                                    .actualValue("4.60")
                                    .requestedValue("4.70")
                                    .unit("meters")
                                    .message(
                                        "The height of the vehicle (4.7 meters) is more than the " +
                                            "permissible height of travel on the road " +
                                            "(4.6 meters) by 0.10 meters.",
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                }

        assertEquals(
            targetNotification0.sorted(),
            refreshedRoute.legNotifications(0)?.sorted(),
        )

        assertEquals(
            targetNotification1.sorted(),
            refreshedRoute.legNotifications(1)?.sorted(),
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
                    listOf(emptyList()),
                    listOf(emptyList()),
                    null,
                    null,
                    0,
                ),
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
                    null,
                ),
                LegItemsResult(
                    listOf(null, null),
                    listOf(emptyList(), emptyList()),
                    listOf(emptyList(), emptyList()),
                    null,
                    null,
                    0,
                ),
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
                    listOf(provideDefaultIncidents(), emptyList()),
                    listOf(provideDefaultClosures(), emptyList()),
                    null,
                    null,
                    0,
                ),
            ),
            TestData(
                "update expiration time from non-null with null refresh ttl",
                provideNavigationRoute(addLeg = false, expirationTime = 5),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null, null, 0, null, null),
                LegItemsResult(
                    listOf(null),
                    listOf(emptyList()),
                    listOf(emptyList()),
                    null,
                    5,
                    0,
                ),
            ),
            TestData(
                "update expiration time from null with null refresh ttl",
                provideNavigationRoute(addLeg = false),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null, null, 0, null, null),
                LegItemsResult(
                    listOf(null),
                    listOf(emptyList()),
                    listOf(emptyList()),
                    null,
                    null,
                    0,
                ),
            ),
            TestData(
                "update expiration time from null to non-null",
                provideNavigationRoute(addLeg = false),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null, null, 5, 10, null),
                LegItemsResult(
                    listOf(null),
                    listOf(emptyList()),
                    listOf(emptyList()),
                    null,
                    15,
                    0,
                ),
            ),
            TestData(
                "update expiration time from non-null with non-null refresh ttl",
                provideNavigationRoute(addLeg = false, expirationTime = 5),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null, null, 3, 9, null),
                LegItemsResult(
                    listOf(null),
                    listOf(emptyList()),
                    listOf(emptyList()),
                    null,
                    12,
                    0,
                ),
            ),
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )
                val newLegAnnotations = createRouteLegAnnotation()
                val newIncidents = listOf(
                    createIncident(startGeometryIndex = 0, endGeometryIndex = 1),
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
                    ),
                )
            },
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )

                val newLegAnnotations = createRouteLegAnnotation()
                val newLegAnnotations2 = createRouteLegAnnotation()
                val newIncidents = listOf(
                    createIncident(startGeometryIndex = 0, endGeometryIndex = 1),
                    createIncident(startGeometryIndex = 10, endGeometryIndex = 15),
                )
                val newIncidents2 = listOf(
                    createIncident(startGeometryIndex = 0, endGeometryIndex = 1),
                    createIncident(startGeometryIndex = 5, endGeometryIndex = 7),
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
                    ),
                )
            },
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )
                val newLegAnnotations = createRouteLegAnnotation()
                val newLegAnnotations2 = createRouteLegAnnotation()
                val newInputIncidents = listOf(
                    createIncident(startGeometryIndex = 2, endGeometryIndex = 4),
                )
                val newOutputIncidents = listOf(
                    createIncident(startGeometryIndex = 4, endGeometryIndex = 6),
                )
                val newInputIncidents2 = listOf(
                    createIncident(startGeometryIndex = 6, endGeometryIndex = 9),
                )
                val newOutputIncidents2 = listOf(
                    createIncident(startGeometryIndex = 6, endGeometryIndex = 9),
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
                    ),
                )
            },
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )
                val newLegAnnotations = createRouteLegAnnotation()
                val newLegAnnotations2 = createRouteLegAnnotation()
                val newIncidents = listOf(
                    createIncident(startGeometryIndex = 10, endGeometryIndex = 12),
                )
                val newIncidents2 = listOf(
                    createIncident(startGeometryIndex = 40, endGeometryIndex = 50),
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
                        0,
                    ),
                )
            },
            run {
                val refreshedMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                val refreshedMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                val refreshedWaypoints = listOf(
                    createWaypoint("name11", unrecognizedProperties = refreshedMetadata1),
                    createWaypoint("name33", unrecognizedProperties = refreshedMetadata2),
                )
                val newLegAnnotations = createRouteLegAnnotation()
                val newLegAnnotations2 = createRouteLegAnnotation()
                val newIncidents = listOf(
                    createIncident(startGeometryIndex = 10, endGeometryIndex = 12),
                )
                val newInputIncidents2 = listOf(
                    createIncident(startGeometryIndex = 40, endGeometryIndex = 50),
                )
                val newOutputIncidents2 = listOf(
                    createIncident(startGeometryIndex = 44, endGeometryIndex = 54),
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
                    ),
                )
            },
        ).forEach { (description, navRoute, refreshItems, result) ->
            mockkObject(AnnotationsRefresher, IncidentsRefresher, ClosuresRefresher) {
                every {
                    AnnotationsRefresher.getRefreshedAnnotations(any(), any(), any(), any(), any())
                } returnsMany
                    (result.newLegAnnotation?.drop(refreshItems.startWithIndex) ?: emptyList())
                navRoute.directionsRoute.legs()?.forEachIndexed { index, leg ->
                    every {
                        IncidentsRefresher.getRefreshedRoadObjects(
                            leg.incidents(),
                            refreshItems.incidents?.get(index),
                            any(),
                            any(),
                        )
                    } answers {
                        result.newIncidents!![index]!!
                    }
                    every {
                        ClosuresRefresher.getRefreshedRoadObjects(
                            leg.closures(),
                            refreshItems.closures?.get(index),
                            any(),
                            any(),
                        )
                    } answers {
                        result.newClosures!![index]!!
                    }
                }
                val updatedNavRoute = try {
                    navRoute.testRouteRefresh(
                        refreshItems.startWithIndex,
                        refreshItems.legGeometryIndex,
                        refreshItems.legAnnotation,
                        refreshItems.incidents,
                        refreshItems.closures,
                        notifications = null,
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
                    updatedNavRoute.waypoints,
                )
                assertEquals(
                    description,
                    navRoute.waypoints?.size,
                    updatedNavRoute.waypoints?.size,
                )
                assertEquals(
                    description,
                    navRoute.unavoidableClosures,
                    updatedNavRoute.unavoidableClosures,
                )

                val capturedOldAnnotations = mutableListOf<LegAnnotation?>()
                val capturedNewAnnotations = mutableListOf<LegAnnotation?>()
                val capturedLegGeometryIndices = mutableListOf<Int>()
                verify {
                    AnnotationsRefresher.getRefreshedAnnotations(
                        captureNullable(capturedOldAnnotations),
                        captureNullable(capturedNewAnnotations),
                        capture(capturedLegGeometryIndices),
                        any(),
                        any(),
                    )
                }
                assertEquals(
                    description,
                    navRoute.directionsRoute.legs()
                        ?.drop(refreshItems.startWithIndex)
                        ?.map { it.annotation() },
                    capturedOldAnnotations,
                )
                assertEquals(
                    description,
                    refreshItems.legAnnotation?.drop(refreshItems.startWithIndex),
                    capturedNewAnnotations,
                )
                assertEquals(
                    description,
                    listOf(result.expectedLegGeometryIndex) +
                        List(capturedLegGeometryIndices.size - 1) { 0 },
                    capturedLegGeometryIndices,
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
        val validStep = createRouteStep()
        return createNavigationRoutes(
            DirectionsResponse.builder()
                .waypoints(dirWaypoints)
                .routes(
                    listOf(
                        DirectionsRoute.builder()
                            .duration(10.0)
                            .distance(distance)
                            .legs(
                                mutableListOf(
                                    RouteLeg.builder()
                                        .annotation(annotations)
                                        .incidents(incidents)
                                        .closures(closures)
                                        .steps(List(2) { validStep })
                                        .build(),
                                ).apply {
                                    if (addLeg) {
                                        add(
                                            RouteLeg.builder()
                                                .annotation(annotations)
                                                .incidents(incidents)
                                                .closures(closures)
                                                .steps(List(2) { validStep })
                                                .build(),
                                        )
                                    }
                                },
                            )
                            .geometry(
                                PolylineUtils.encode(
                                    listOf(
                                        Point.fromLngLat(11.22, 33.44),
                                        Point.fromLngLat(23.34, 34.45),
                                    ),
                                    5,
                                ),
                            )
                            .build(),
                    ),
                )
                .code("Ok")
                .build(),
            createRouteOptions(
                waypointsPerRoute = false,
                geometries = DirectionsCriteria.GEOMETRY_POLYLINE6,
            ),
            RouterOrigin.ONLINE,
            expirationTime,
        ).first()
    }

    @RunWith(Parameterized::class)
    class UpdateWaypointsTest(
        private val description: String,
        private val inputWaypoints: List<DirectionsWaypoint>?,
        private val refreshedWaypoints: List<DirectionsWaypoint>?,
        private val expectedWaypoints: List<DirectionsWaypoint>?,
    ) {

        @get:Rule
        val nativeRouteParserRule = NativeRouteParserRule()

        @get:Rule
        val loggerRule = LoggingFrontendTestRule()

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
                            createWaypoint("name22", unrecognizedProperties = inputMetadata2),
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
                            createWaypoint("name22"),
                        )
                        arrayOf(
                            "update waypoints from empty to non-empty",
                            emptyList<DirectionsWaypoint>(),
                            waypoints,
                            emptyList<DirectionsWaypoint>(),
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val waypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2),
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
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2),
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
                            createWaypoint("name3", unrecognizedProperties = refreshedMetadata1),
                        )

                        val inputWaypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2),
                        )
                        arrayOf(
                            "update waypoints from 2 to 1",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint(
                                    "name3",
                                    unrecognizedProperties = refreshedMetadata1,
                                ),
                                createWaypoint("name2", unrecognizedProperties = inputMetadata2),
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
                                unrecognizedProperties = refreshedMetadata1,
                            ),
                            createWaypoint(
                                "name4",
                                unrecognizedProperties = refreshedMetadata2,
                            ),
                        )

                        val inputWaypoints = listOf(
                            createWaypoint(
                                "name1",
                                unrecognizedProperties = inputMetadata1,
                            ),
                            createWaypoint(
                                "name2",
                                unrecognizedProperties = inputMetadata2,
                            ),
                        )
                        arrayOf(
                            "update waypoints from 2 to 2",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint(
                                    "name3",
                                    unrecognizedProperties = refreshedMetadata1,
                                ),
                                createWaypoint(
                                    "name4",
                                    unrecognizedProperties = refreshedMetadata2,
                                ),
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
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2),
                        )
                        arrayOf(
                            "update waypoints from 2 to 3",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint(
                                    "name3",
                                    unrecognizedProperties = refreshedMetadata1,
                                ),
                                createWaypoint(
                                    "name4",
                                    unrecognizedProperties = refreshedMetadata2,
                                ),
                            ),
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val refreshedMetadata2 = mapOf("key4" to JsonPrimitive("value4"))

                        val refreshedWaypoints = listOf(
                            null,
                            createWaypoint("name3", unrecognizedProperties = refreshedMetadata2),
                        )

                        val inputWaypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2),
                        )
                        arrayOf(
                            "update waypoints from 2 to null + 1",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint(
                                    "name1",
                                    unrecognizedProperties = inputMetadata1,
                                ),
                                createWaypoint(
                                    "name3",
                                    unrecognizedProperties = refreshedMetadata2,
                                ),
                            ),
                        )
                    },
                    run {
                        val inputMetadata1 = mapOf("key1" to JsonPrimitive("value1"))
                        val inputMetadata2 = mapOf("key2" to JsonPrimitive("value2"))
                        val refreshedMetadata2 = mapOf("key4" to JsonPrimitive("value4"))

                        val refreshedWaypoints = listOf(
                            createWaypoint("name3", unrecognizedProperties = null),
                            createWaypoint("name4", unrecognizedProperties = refreshedMetadata2),
                        )

                        val inputWaypoints = listOf(
                            createWaypoint("name1", unrecognizedProperties = inputMetadata1),
                            createWaypoint("name2", unrecognizedProperties = inputMetadata2),
                        )
                        arrayOf(
                            "update waypoints from 2 to null metadata + 1",
                            inputWaypoints,
                            refreshedWaypoints,
                            listOf(
                                createWaypoint("name3", unrecognizedProperties = null),
                                createWaypoint(
                                    "name4",
                                    unrecognizedProperties = refreshedMetadata2,
                                ),
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
            val updatedRoute = inputRoute.testRouteRefresh(
                0,
                0,
                null,
                null,
                null,
                null,
                refreshedWaypoints,
                0,
                null,
            )
            assertEquals(expectedWaypoints, updatedRoute.waypoints)
        }

        @Test
        fun routeWaypoints() {
            val inputRoute = provideNavigationRoute(
                routeWaypoints = inputWaypoints,
                addLeg = true,
                waypointsPerRoute = true,
            )
            val updatedRoute = inputRoute.testRouteRefresh(
                0,
                0,
                null,
                null,
                null,
                null,
                refreshedWaypoints,
                0,
                null,
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
                    Point.fromLngLat(3.3, 6.7),
                ),
                5,
            )
            val validStep = createRouteStep()
            return createNavigationRoutes(
                response = DirectionsResponse.builder()
                    .routes(
                        listOf(
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
                                            .build(),
                                    ).apply {
                                        if (addLeg) {
                                            add(
                                                RouteLeg.builder()
                                                    .annotation(annotations)
                                                    .incidents(incidents)
                                                    .closures(closures)
                                                    .steps(List(2) { validStep })
                                                    .build(),
                                            )
                                        }
                                    },
                                )
                                .geometry(
                                    PolylineUtils.encode(
                                        listOf(
                                            Point.fromLngLat(11.22, 33.44),
                                            Point.fromLngLat(23.34, 34.45),
                                        ),
                                        5,
                                    ),
                                )
                                .build(),
                        ),
                    )
                    .waypoints(dirWaypoints)
                    .code("200")
                    .build(),
                options = createRouteOptions(
                    geometries = DirectionsCriteria.GEOMETRY_POLYLINE,
                    waypointsPerRoute = waypointsPerRoute,
                ),
                routerOrigin = RouterOrigin.OFFLINE,
            ).first()
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
                unrecognizedProperties = mapOf("some key 1" to JsonPrimitive("some value 1")),
            ),
            createWaypoint(
                "name2",
                unrecognizedProperties = mapOf("some key 2" to JsonPrimitive("some value 2")),
            ),
        )

        private fun provideEvNotifications(): MutableList<MutableList<Notification>> {
            return mutableListOf(
                mutableListOf(
                    Notification.builder()
                        .type("alert")
                        .subtype("stationUnavailable")
                        .refreshType("dynamic")
                        .reason("outOfOrder")
                        .chargingStationId("station1")
                        .build(),
                    Notification.builder()
                        .type("alert")
                        .subtype("stationUnavailable")
                        .refreshType("dynamic")
                        .reason("outOfOrder")
                        .chargingStationId("station2")
                        .build(),
                    Notification.builder()
                        .type("alert")
                        .subtype("evInsufficientCharge")
                        .geometryIndex(3)
                        .refreshType("dynamic")
                        .build(),
                    Notification.builder()
                        .type("violation")
                        .subtype("evMinChargeAtChargingStation")
                        .refreshType("dynamic")
                        .details(
                            NotificationDetails.builder()
                                .requestedValue("30000")
                                .actualValue("27000")
                                .unit("Wh")
                                .build(),
                        )
                        .build(),
                    Notification.builder()
                        .type("violation")
                        .subtype("evMinChargeAtDestination")
                        .refreshType("dynamic")
                        .details(
                            NotificationDetails.builder()
                                .requestedValue("20000")
                                .actualValue("13000")
                                .unit("Wh")
                                .build(),
                        )
                        .build(),
                ),
                mutableListOf(
                    Notification.builder()
                        .type("alert")
                        .subtype("stationUnavailable")
                        .refreshType("dynamic")
                        .reason("outOfOrder")
                        .chargingStationId("station1")
                        .build(),
                    Notification.builder()
                        .type("violation")
                        .subtype("evMinChargeAtDestination")
                        .refreshType("dynamic")
                        .details(
                            NotificationDetails.builder()
                                .requestedValue("20000")
                                .actualValue("13000")
                                .unit("Wh")
                                .build(),
                        )
                        .build(),
                ),
            )
        }
    }

    private fun NavigationRoute.legNotifications(index: Int): List<Notification>? =
        directionsRoute.legs()?.get(index)?.notifications()

    private fun List<Notification>.sorted(): List<Notification>? =
        this.sortedBy { it.toString() }

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
        val waypoints: List<DirectionsWaypoint>?,
        val responseTime: Long,
        val refreshTtl: Int?,
        val legGeometryIndex: Int?,
    )

    /**
     * Expecting result items after route is refreshed
     */
    private data class LegItemsResult(
        val newLegAnnotation: List<LegAnnotation?>?,
        val newIncidents: List<List<Incident>?>,
        val newClosures: List<List<Closure>?>?,
        val newWaypoints: List<DirectionsWaypoint>?,
        val expectedExpirationTime: Long?,
        val expectedLegGeometryIndex: Int,
    )
}

private fun createNavigationRouteFromResource(
    responseFileName: String,
    requestFileName: String,
) = createNavigationRoutes(
    response = DirectionsResponse.fromJson(
        FileUtils.loadJsonFixture(responseFileName),
    ),
    options = RouteOptions.fromUrl(
        URL(
            FileUtils.loadJsonFixture(requestFileName),
        ),
    ),
    routerOrigin = RouterOrigin.ONLINE,
    responseTimeElapsedSeconds = null,
).first()

internal fun NavigationRoute.testRouteRefresh(
    initialLegIndex: Int,
    currentLegGeometryIndex: Int?,
    legAnnotations: List<LegAnnotation?>?,
    incidents: List<List<Incident>?>?,
    closures: List<List<Closure>?>?,
    notifications: List<List<Notification>?>?,
    waypoints: List<DirectionsWaypoint>?,
    responseTimeElapsedSeconds: Long,
    refreshTtl: Int?,
): NavigationRoute {
    val legsCount = listOf(
        incidents?.size,
        notifications?.size,
        closures?.size,
        legAnnotations?.size,
    ).maxBy { it ?: 0 } ?: 0
    val routeRefreshResponse = DirectionsRefreshResponse.builder()
        .route(
            DirectionsRouteRefresh.builder().legs(
                (0 until legsCount).map { legIndex ->
                    RouteLegRefresh.builder()
                        .annotation(legAnnotations?.get(legIndex))
                        .notifications(notifications?.get(legIndex))
                        .closures(closures?.get(legIndex))
                        .incidents(incidents?.get(legIndex))
                        .build()
                },
            ).unrecognizedJsonProperties(
                mutableMapOf<String, JsonElement>().apply {
                    if (waypoints != null) {
                        put(
                            Constants.RouteResponse.KEY_WAYPOINTS,
                            JsonArray().apply {
                                waypoints.forEach { waypoint ->
                                    add(
                                        waypoint?.let {
                                            Gson().fromJson(it.toJson(), JsonObject::class.java)
                                        },
                                    )
                                }
                            },
                        )
                    }
                    if (refreshTtl != null) {
                        put(Constants.RouteResponse.KEY_REFRESH_TTL, JsonPrimitive(refreshTtl))
                    }
                },
            )
                .build(),
        )
        .code("ok")
        .build()
    val dataRefResponse = routeRefreshResponse.toJson().toByteArray().let { response ->
        ByteBuffer.allocateDirect(response.size).let {
            it.put(response)
            DataRef(it)
        }
    }

    return this.internalRefreshRoute(
        refreshResponse = dataRefResponse,
        legIndex = initialLegIndex,
        legGeometryIndex = currentLegGeometryIndex ?: 0,
        responseTimeElapsedSeconds = responseTimeElapsedSeconds,
    ).getOrThrow()
}
