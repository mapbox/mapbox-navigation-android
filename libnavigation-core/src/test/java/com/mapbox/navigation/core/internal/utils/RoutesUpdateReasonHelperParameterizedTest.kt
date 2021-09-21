package com.mapbox.navigation.core.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.TripSession
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RoutesUpdateReasonHelperParameterizedTest(
    val testName: String,
    val legacyRoutes: List<DirectionsRoute>,
    val newRoutes: List<DirectionsRoute>,
    val isOffRoute: Boolean,
    @RoutesExtra.RoutesUpdateReason val expectedReason: String,
) {

    private lateinit var routesUpdateReasonHelper: RoutesUpdateReasonHelper
    private val offRouteObserverSlot = CapturingSlot<OffRouteObserver>()

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = listOf(
            arrayOf(
                "legacyRoute is not empty, newRoutes is empty, " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP}]",
                listOf(provideDirectionRouteBase().build()),
                emptyList<DirectionsRoute>(),
                false,
                RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            ),
            arrayOf(
                "legacyRoute is empty, newRoutes is empty, " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP}]",
                emptyList<DirectionsRoute>(),
                emptyList<DirectionsRoute>(),
                false,
                RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP,
            ),
            arrayOf(
                "legacyRoute is empty, newRoutes is not empty, " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_NEW}]",
                emptyList<DirectionsRoute>(),
                listOf(provideDirectionRouteBase().build()),
                false,
                RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            ),
            arrayOf(
                "offRoute event, same RouteOptions Coordinates(except origin), " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_REROUTE}]",
                listOf(
                    provideDirectionRouteBase(
                        uuid = "1",
                        geometry = "aaa_bbb",
                        coordinates = listOf(
                            Point.fromLngLat(99.1, 99.2),
                            Point.fromLngLat(11.1, 11.2),
                            Point.fromLngLat(12.1, 12.2),
                        )
                    ).build()
                ),
                listOf(
                    provideDirectionRouteBase(
                        uuid = "2",
                        geometry = "ccc_ddd",
                        coordinates = listOf(
                            Point.fromLngLat(50.1, 51.2),
                            Point.fromLngLat(12.1, 12.2),
                        )
                    ).build()
                ),
                true,
                RoutesExtra.ROUTES_UPDATE_REASON_REROUTE,
            ),
            arrayOf(
                "offRoute event, 3 coordinates in original route; " +
                    "2 coordinates in new route and same destination, " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_REROUTE}]",
                listOf(
                    provideDirectionRouteBase(
                        coordinates = listOf(
                            Point.fromLngLat(99.1, 99.2),
                            Point.fromLngLat(11.1, 11.2),
                            Point.fromLngLat(12.1, 12.2),
                        )
                    ).build()
                ),
                listOf(
                    provideDirectionRouteBase(
                        coordinates = listOf(
                            Point.fromLngLat(10.1, 10.2),
                            Point.fromLngLat(11.1, 11.2),
                            Point.fromLngLat(12.1, 12.2),
                        )
                    ).build()
                ),
                true,
                RoutesExtra.ROUTES_UPDATE_REASON_REROUTE,
            ),
            arrayOf(
                "same Route and same UUID, " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_REFRESH}]",
                listOf(provideDirectionRouteBase().build()),
                listOf(provideDirectionRouteBase().build()),
                false,
                RoutesExtra.ROUTES_UPDATE_REASON_REFRESH,
            ),
            arrayOf(
                "same RouteOptions coordinates, different UUIDs, same geometries, " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE}]",
                listOf(
                    provideDirectionRouteBase(
                        uuid = "1",
                        coordinates = listOf(
                            Point.fromLngLat(99.1, 99.2),
                            Point.fromLngLat(11.1, 11.2),
                            Point.fromLngLat(12.1, 12.2),
                        )
                    ).build()
                ),
                listOf(
                    provideDirectionRouteBase(
                        uuid = "2",
                        coordinates = listOf(
                            Point.fromLngLat(10.1, 10.2),
                            Point.fromLngLat(11.1, 11.2),
                            Point.fromLngLat(12.1, 12.2),
                        )
                    ).build()
                ),
                false,
                RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE,
            ),
            arrayOf(
                "same RouteOptions coordinates, different geometries, same UUID, " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE}]",
                listOf(
                    provideDirectionRouteBase(
                        geometry = "aaa_bbb",
                        coordinates = listOf(
                            Point.fromLngLat(99.1, 99.2),
                            Point.fromLngLat(11.1, 11.2),
                            Point.fromLngLat(12.1, 12.2),
                        )
                    ).build()
                ),
                listOf(
                    provideDirectionRouteBase(
                        geometry = "ccc_ddd",
                        coordinates = listOf(
                            Point.fromLngLat(10.1, 10.2),
                            Point.fromLngLat(11.1, 11.2),
                            Point.fromLngLat(12.1, 12.2),
                        )
                    ).build()
                ),
                false,
                RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE,
            ),
            // `else` (other) cases:
            arrayOf(
                "offRoute event, different RouteOptions coordinates, different UUID, " +
                    "different geometries, " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_NEW}]",
                listOf(
                    provideDirectionRouteBase(
                        uuid = "1",
                        geometry = "aaa_bbb",
                        coordinates = listOf(
                            Point.fromLngLat(99.1, 99.2),
                            Point.fromLngLat(11.1, 11.2),
                            Point.fromLngLat(12.1, 12.2),
                        )
                    ).build()
                ),
                listOf(
                    provideDirectionRouteBase(
                        uuid = "2",
                        geometry = "ccc_ddd",
                        coordinates = listOf(
                            Point.fromLngLat(10.1, 10.2),
                            Point.fromLngLat(11.1, 11.2),
                            Point.fromLngLat(51.1, 52.2),
                        )
                    ).build()
                ),
                true,
                RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            ),
            arrayOf(
                "different RouteOptions coordinates, different geometries, different UUID " +
                    "reason is [${RoutesExtra.ROUTES_UPDATE_REASON_NEW}]",
                listOf(
                    provideDirectionRouteBase(
                        uuid = "1",
                        geometry = "aaa_bbb",
                        coordinates = listOf(
                            Point.fromLngLat(1.1, 2.2),
                            Point.fromLngLat(2.1, 2.2),
                            Point.fromLngLat(3.1, 3.2),
                        )
                    ).build()
                ),
                listOf(
                    provideDirectionRouteBase(
                        uuid = "2",
                        geometry = "eee_fff",
                        coordinates = listOf(
                            Point.fromLngLat(4.1, 4.2),
                            Point.fromLngLat(5.1, 5.2),
                            Point.fromLngLat(6.1, 6.2),
                        )
                    ).build()
                ),
                false,
                RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            ),
            arrayOf(
                "offRoute event, different RouteOptions coordinates, different geometries, " +
                    "different UUID, reason is [${RoutesExtra.ROUTES_UPDATE_REASON_NEW}]",
                listOf(
                    provideDirectionRouteBase(
                        uuid = "1",
                        geometry = "aaa_bbb",
                        coordinates = listOf(
                            Point.fromLngLat(1.1, 2.2),
                            Point.fromLngLat(2.1, 2.2),
                            Point.fromLngLat(3.1, 3.2),
                        )
                    ).build()
                ),
                listOf(
                    provideDirectionRouteBase(
                        uuid = "2",
                        geometry = "eee_fff",
                        coordinates = listOf(
                            Point.fromLngLat(4.1, 4.2),
                            Point.fromLngLat(5.1, 5.2),
                            Point.fromLngLat(6.1, 6.2),
                        )
                    ).build()
                ),
                true,
                RoutesExtra.ROUTES_UPDATE_REASON_NEW,
            ),
        )

        private fun provideDirectionRouteBase(
            uuid: String = "1",
            geometry: String = "aaa_bbb",
            coordinates: List<Point> = listOf(
                Point.fromLngLat(1.1, 1.2),
                Point.fromLngLat(2.1, 2.2),
                Point.fromLngLat(3.1, 3.2),
            )
        ): DirectionsRoute.Builder =
            DirectionsRoute.builder()
                .requestUuid(uuid)
                .geometry(geometry)
                .duration(100.0)
                .distance(200.0)
                .routeOptions(
                    RouteOptions.builder()
                        .applyDefaultNavigationOptions()
                        .coordinatesList(coordinates)
                        .build()
                )
    }

    @Before
    fun setup() {
        val mockkTripSession = mockk<TripSession> {
            every { registerOffRouteObserver(capture(offRouteObserverSlot)) } just runs
        }
        routesUpdateReasonHelper = RoutesUpdateReasonHelper(mockkTripSession)
    }

    @Test
    fun testCase() {
        offRouteObserverSlot.captured.onOffRouteStateChanged(isOffRoute)

        assertEquals(
            testName,
            expectedReason,
            routesUpdateReasonHelper.getReason(legacyRoutes, newRoutes),
        )
    }
}
