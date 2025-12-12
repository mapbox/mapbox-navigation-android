@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.time.parseISO8601DateToLocalTimeOrNull
import com.mapbox.navigation.core.internal.RouteProgressData
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createIncident
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteLeg
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class ExpiringDataRemoverTest {

    private val localDateProvider = mockk<() -> Date>(relaxed = true)
    private val sut = ExpiringDataRemover(localDateProvider)

    @Test
    fun removeExpiringDataFromRoutes() {
        every {
            localDateProvider()
        } returns parseISO8601DateToLocalTimeOrNull("2022-06-30T20:00:00Z")!!
        val route1 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("moderate", "moderate"))
                            .congestionNumeric(listOf(80, 80))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-30T21:59:00Z"),
                            createIncident(endTime = "2022-06-31T21:59:00Z"),
                        ),
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("heavy", "heavy"))
                            .congestionNumeric(listOf(90, 90))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-30T20:59:00Z"),
                            createIncident(endTime = "bad time"),
                            createIncident(endTime = "2022-06-30T19:59:00Z"),
                        ),
                    ),
                ),
            ),
        )
        val expectedNewRoute1 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("moderate", "moderate"))
                            .congestionNumeric(listOf(80, 80))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-30T21:59:00Z"),
                            createIncident(endTime = "2022-06-31T21:59:00Z"),
                        ),
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("unknown", "unknown"))
                            .congestionNumeric(listOf(null, null))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-30T20:59:00Z"),
                            createIncident(endTime = "bad time"),
                        ),
                    ),
                ),
            ),
        )
        val route2 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("moderate", "heavy"))
                            .congestionNumeric(listOf(80, 90))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T10:59:00Z"),
                            createIncident(endTime = "2022-06-21T10:59:00Z"),
                        ),
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("heavy", "moderate"))
                            .congestionNumeric(listOf(90, 80))
                            .build(),
                        incidents = null,
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("severe", "low"))
                            .congestionNumeric(listOf(120, 20))
                            .build(),
                        incidents = null,
                    ),
                    createRouteLeg(
                        annotation = null,
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T22:59:00Z"),
                        ),
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder().build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T22:50:00Z"),
                        ),
                    ),
                ),
            ),
        )
        val expectedNewRoute2 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                legs = listOf(
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("moderate", "heavy"))
                            .congestionNumeric(listOf(80, 90))
                            .build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T10:59:00Z"),
                            createIncident(endTime = "2022-06-21T10:59:00Z"),
                        ),
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("heavy", "moderate"))
                            .congestionNumeric(listOf(90, 80))
                            .build(),
                        incidents = null,
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder()
                            .congestion(listOf("unknown", "unknown"))
                            .congestionNumeric(listOf(null, null))
                            .build(),
                        incidents = null,
                    ),
                    createRouteLeg(
                        annotation = null,
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T22:59:00Z"),
                        ),
                    ),
                    createRouteLeg(
                        annotation = LegAnnotation.builder().build(),
                        incidents = listOf(
                            createIncident(endTime = "2022-06-31T22:50:00Z"),
                        ),
                    ),
                ),
            ),
        )
        val route3 = createNavigationRoute(directionsRoute = createDirectionsRoute(legs = null))
        val expectedNewRoute3 = createNavigationRoute(
            directionsRoute = createDirectionsRoute(legs = null),
        )
        val route1RouteProgressData = RouteProgressData(1, 2, 3)
        val route2RouteProgressData = RouteProgressData(2, 5, 6)
        val route3RouteProgressData = RouteProgressData(0, 5, 7)
        val input = RoutesRefresherResult(
            RouteRefresherResult(
                route1,
                route1RouteProgressData,
                RouteRefresherStatus.Failure,
            ),
            listOf(
                RouteRefresherResult(
                    route2,
                    route2RouteProgressData,
                    RouteRefresherStatus.Failure,
                ),
                RouteRefresherResult(
                    route3,
                    route3RouteProgressData,
                    RouteRefresherStatus.Invalidated,
                ),
            ),
        )
        val expected = RoutesRefresherResult(
            RouteRefresherResult(
                expectedNewRoute1,
                route1RouteProgressData,
                RouteRefresherStatus.Failure,
                wasRouteUpdated = true,
            ),
            listOf(
                RouteRefresherResult(
                    expectedNewRoute2,
                    route2RouteProgressData,
                    RouteRefresherStatus.Failure,
                    wasRouteUpdated = true,
                ),
                RouteRefresherResult(
                    expectedNewRoute3,
                    route3RouteProgressData,
                    RouteRefresherStatus.Invalidated,
                    wasRouteUpdated = true,
                ),
            ),
        )

        val actual = sut.removeExpiringDataFromRoutesProgressData(input)

        assertEquals(expected, actual)
        assertEquals(
            false,
            actual.primaryRouteRefresherResult.route.routeRefreshMetadata?.isUpToDate,
        )
        assertEquals(
            listOf(false, false),
            actual.alternativesRouteRefresherResults.map {
                it.route.routeRefreshMetadata?.isUpToDate
            },
        )
    }
}
