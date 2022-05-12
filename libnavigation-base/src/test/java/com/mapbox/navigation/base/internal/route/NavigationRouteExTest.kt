package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.RouterOrigin
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationRouteExTest {

    @Test
    fun `extension NavigationRoute refreshRoute`() {
        listOf(
            TestData(
                "update to null items",
                provideNavigationRoute(addLeg = false),
                RefreshLegItemsWrapper(0, listOf(null), listOf(null)),
                LegItemsResult(
                    listOf(null),
                    listOf(null)
                )
            ),
            TestData(
                "update to null items multi-leg route",
                provideNavigationRoute(addLeg = true),
                RefreshLegItemsWrapper(0, listOf(null, null), listOf(null, null)),
                LegItemsResult(
                    listOf(null, null),
                    listOf(null, null)
                )
            ),
            TestData(
                "update to null items multi-leg route starting with second leg",
                provideNavigationRoute(addLeg = true),
                RefreshLegItemsWrapper(
                    1,
                    listOf(null, null),
                    listOf(null, null)
                ),
                LegItemsResult(
                    listOf(provideDefaultLegAnnotation(), null),
                    listOf(provideDefaultIncidents(), null)
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
                        listOf(newIncidents)
                    ),
                    LegItemsResult(
                        listOf(newLegAnnotations),
                        listOf(newIncidents),
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
                        listOf(newIncidents, newIncidents2)
                    ),
                    LegItemsResult(
                        listOf(newLegAnnotations, newLegAnnotations2),
                        listOf(newIncidents, newIncidents2)
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
                        listOf(newIncidents, newIncidents2)
                    ),

                    LegItemsResult(
                        listOf(provideDefaultLegAnnotation(), newLegAnnotations2),
                        listOf(provideDefaultIncidents(), newIncidents2)
                    )
                )
            }
        ).forEach { (description, navRoute, refreshItems, result) ->
            val updatedNavRoute = navRoute.refreshRoute(
                refreshItems.startWithIndex, refreshItems.legAnnotation, refreshItems.incidents
            )

            assertEquals(
                description,
                result.newLegAnnotation,
                updatedNavRoute.directionsRoute
                    .legs()
                    ?.map { it.annotation() }
            )
            assertEquals(
                description,
                result.newIncidents,
                updatedNavRoute.directionsRoute
                    .legs()
                    ?.map { it.incidents() }
            )
        }
    }

    private fun provideNavigationRoute(
        annotations: LegAnnotation? = provideDefaultLegAnnotation(),
        incidents: List<Incident>? = provideDefaultIncidents(),
        addLeg: Boolean
    ): NavigationRoute {
        return NavigationRoute(
            DirectionsResponse.builder()
                .routes(
                    listOf(
                        DirectionsRoute.builder()
                            .duration(10.0)
                            .distance(10.0)
                            .legs(
                                mutableListOf(
                                    RouteLeg.builder()
                                        .annotation(annotations)
                                        .incidents(incidents)
                                        .build()
                                ).apply {
                                    if (addLeg) {
                                        add(
                                            RouteLeg.builder()
                                                .annotation(annotations)
                                                .incidents(incidents)
                                                .build()
                                        )
                                    }
                                }
                            )
                            .build()
                    )
                )
                .code("Ok")
                .build(),
            0,
            mockk(),
            mockk {
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
    )

    /**
     * Expecting result items after route is refreshed
     */
    private data class LegItemsResult(
        val newLegAnnotation: List<LegAnnotation?>?,
        val newIncidents: List<List<Incident>?>?,
    )
}
