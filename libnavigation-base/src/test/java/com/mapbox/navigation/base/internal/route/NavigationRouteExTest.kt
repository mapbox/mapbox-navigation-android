package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouterOrigin
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Test

class NavigationRouteExTest {

    @Test
    fun `update Navigation route`() {
        val navigationRoute = provideNavigationRoute(addLeg = true, distance = 88.0)
        val updated = navigationRoute.updateDirectionsRouteOnly {
            assertEquals(88.0, distance())
            toBuilder().distance(73483.0).build()
        }
        assertEquals(73483.0, updated.directionsRoute.distance())
    }

    @Test
    fun `extension NavigationRoute refreshRoute`() {
        listOf(
            TestData(
                "update to null items",
                provideNavigationRoute(addLeg = false),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null), null),
                LegItemsResult(
                    listOf(null),
                    listOf(null),
                    0
                )
            ),
            TestData(
                "update to null items multi-leg route",
                provideNavigationRoute(addLeg = true),
                RefreshLegItemsWrapper(0, listOf(null, null), listOf(null, null), null),
                LegItemsResult(
                    listOf(null, null),
                    listOf(null, null),
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
                    null
                ),
                LegItemsResult(
                    listOf(provideDefaultLegAnnotation(), null),
                    listOf(provideDefaultIncidents(), null),
                    0
                ),
            ),

            run {
                val newLegAnnotations = mockk<LegAnnotation>()
                val newIncidents = mockk<List<Incident>>()
                return@run TestData(
                    "update items route",
                    provideNavigationRoute(addLeg = false),
                    RefreshLegItemsWrapper(
                        0,
                        listOf(newLegAnnotations),
                        listOf(newIncidents),
                        null
                    ),
                    LegItemsResult(
                        listOf(newLegAnnotations),
                        listOf(newIncidents),
                        0
                    )
                )
            },
            run {
                val newLegAnnotations = mockk<LegAnnotation>()
                val newLegAnnotations2 = mockk<LegAnnotation>()
                val newIncidents = mockk<List<Incident>>()
                val newIncidents2 = mockk<List<Incident>>()
                TestData(
                    "update items multi-leg route",
                    provideNavigationRoute(addLeg = true),
                    RefreshLegItemsWrapper(
                        0,
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2),
                        null
                    ),
                    LegItemsResult(
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2),
                        0
                    )
                )
            },
            run {
                val newLegAnnotations = mockk<LegAnnotation>()
                val newLegAnnotations2 = mockk<LegAnnotation>()
                val newIncidents = mockk<List<Incident>>()
                val newIncidents2 = mockk<List<Incident>>()
                TestData(
                    "update items multi-leg route, geometryIndex is 2",
                    provideNavigationRoute(addLeg = true),
                    RefreshLegItemsWrapper(
                        0,
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2),
                        2
                    ),
                    LegItemsResult(
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2),
                        2
                    )
                )
            },
            run {
                val newLegAnnotations = mockk<LegAnnotation>()
                val newLegAnnotations2 = mockk<LegAnnotation>()
                val newIncidents = mockk<List<Incident>>()
                val newIncidents2 = mockk<List<Incident>>()
                TestData(
                    "update items multi-leg route starting with second leg",
                    provideNavigationRoute(addLeg = true),
                    RefreshLegItemsWrapper(
                        1,
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2),
                        null
                    ),

                    LegItemsResult(
                        listOf(provideDefaultLegAnnotation(), newLegAnnotations2),
                        listOf(provideDefaultIncidents(), newIncidents2),
                        0
                    )
                )
            },
            run {
                val newLegAnnotations = mockk<LegAnnotation>()
                val newLegAnnotations2 = mockk<LegAnnotation>()
                val newIncidents = mockk<List<Incident>>()
                val newIncidents2 = mockk<List<Incident>>()
                TestData(
                    "update items multi-leg route starting with second leg, geometryIndex = 4",
                    provideNavigationRoute(addLeg = true),
                    RefreshLegItemsWrapper(
                        1,
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2),
                        4
                    ),

                    LegItemsResult(
                        listOf(provideDefaultLegAnnotation(), newLegAnnotations2),
                        listOf(provideDefaultIncidents(), newIncidents2),
                        4
                    )
                )
            },
        ).forEach { (description, navRoute, refreshItems, result) ->
            mockkObject(AnnotationsRefresher) {
                every {
                    AnnotationsRefresher.getRefreshedAnnotations(any(), any(), any())
                } returnsMany
                    (result.newLegAnnotation?.drop(refreshItems.startWithIndex) ?: emptyList())
                val updatedNavRoute = navRoute.refreshRoute(
                    refreshItems.startWithIndex,
                    refreshItems.legGeometryIndex,
                    refreshItems.legAnnotation,
                    refreshItems.incidents
                )

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
                    listOf(result.expectedLegGeometryIndex)
                        + List(capturedLegGeometryIndices.size - 1) { 0 },
                    capturedLegGeometryIndices
                )
            }
        }
    }

    private fun provideNavigationRoute(
        annotations: LegAnnotation? = provideDefaultLegAnnotation(),
        incidents: List<Incident>? = provideDefaultIncidents(),
        addLeg: Boolean,
        distance: Double = 10.0
    ): NavigationRoute {
        val twoPointGeometry = PolylineUtils.encode(
            listOf(
                Point.fromLngLat(1.2, 3.4),
                Point.fromLngLat(3.3, 6.7)
            ),
            5
        )
        val validStep = mockk<LegStep>(relaxed = true) {
            every { geometry() } returns twoPointGeometry
        }
        return NavigationRoute(
            DirectionsResponse.builder()
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
                                        .steps(List(2) { validStep })
                                        .build()
                                ).apply {
                                    if (addLeg) {
                                        add(
                                            RouteLeg.builder()
                                                .annotation(annotations)
                                                .incidents(incidents)
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
                            .build()
                    )
                )
                .code("Ok")
                .build(),
            0,
            mockk {
                every { geometries() } returns DirectionsCriteria.GEOMETRY_POLYLINE
            },
            mockk {
                every { routeInfo } returns mockk(relaxed = true)
                every { routeId } returns ""
                every { routerOrigin } returns RouterOrigin.ONLINE
            }
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
        val legGeometryIndex: Int?,
    )

    /**
     * Expecting result items after route is refreshed
     */
    private data class LegItemsResult(
        val newLegAnnotation: List<LegAnnotation?>?,
        val newIncidents: List<List<Incident>?>?,
        val expectedLegGeometryIndex: Int,
    )
}
