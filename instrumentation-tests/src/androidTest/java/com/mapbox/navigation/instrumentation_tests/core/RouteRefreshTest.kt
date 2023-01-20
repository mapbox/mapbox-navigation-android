package com.mapbox.navigation.instrumentation_tests.core

import android.content.Context
import android.location.Location
import androidx.annotation.IntegerRes
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.Incident
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.clearNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.roadObjectsOnRoute
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routesUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForAlternativesUpdate
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockRoutingTileEndpointErrorRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.IdlingPolicyTimeoutRule
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoute
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider.toNavigationRoutes
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteRefreshTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val idlingPolicyRule = IdlingPolicyTimeoutRule(35, TimeUnit.SECONDS)

    private lateinit var mapboxNavigation: MapboxNavigation
    private val twoCoordinates = listOf(
        Point.fromLngLat(-121.496066, 38.577764),
        Point.fromLngLat(-121.480279, 38.57674)
    )
    private val threeCoordinates = listOf(
        Point.fromLngLat(-121.496066, 38.577764),
        Point.fromLngLat(-121.480279, 38.57674),
        Point.fromLngLat(-121.468434, 38.58225)
    )
    private val threeCoordinatesWithIncidents = listOf(
        Point.fromLngLat(-75.474061, 38.546280),
        Point.fromLngLat(-75.525486, 38.772959),
        Point.fromLngLat(-74.698765, 39.822911)
    )
    private val multilegCoordinates = listOf(
        Point.fromLngLat(38.577764, -121.496066),
        Point.fromLngLat(38.576795, -121.480256),
        Point.fromLngLat(38.582195, -121.468458)
    )

    private lateinit var failByRequestRouteRefreshResponse: FailByRequestMockRequestHandler

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = twoCoordinates[0].latitude()
        longitude = twoCoordinates[0].longitude()
        bearing = 190f
    }

    @Before
    fun setup() {
        setupMockRequestHandlers(
            twoCoordinates,
            R.raw.route_response_route_refresh,
            R.raw.route_response_route_refresh_annotations,
            "route_response_route_refresh"
        )

        runOnMainSync {
            val routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(TimeUnit.SECONDS.toMillis(30))
                .build()
            RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
                isAccessible = true
                set(routeRefreshOptions, 3_000L)
            }
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .routeRefreshOptions(routeRefreshOptions)
                    .routingTilesOptions(
                        RoutingTilesOptions.Builder()
                            .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                            .build()
                    )
                    .navigatorPredictionMillis(0L)
                    .build()
            )
        }
    }

    @After
    fun tearDown() {
        if (this::failByRequestRouteRefreshResponse.isInitialized) {
            failByRequestRouteRefreshResponse.failResponse = false
        }
    }

    @Test
    fun expect_route_refresh_to_update_traffic_annotations_incidents_and_closures_for_all_routes() =
        sdkTest {
            val routeOptions = generateRouteOptions(twoCoordinates)
            val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
                .reversed()

            mapboxNavigation.setNavigationRoutes(requestedRoutes)
            mapboxNavigation.startTripSession()
            stayOnInitialPosition()
            val routeUpdates = mapboxNavigation.routesUpdates()
                .take(2)
                .map { it.navigationRoutes }
                .toList()
            val initialRoutes = routeUpdates[0]
            val refreshedRoutes = routeUpdates[1]

            val routeProgress = mapboxNavigation.routeProgressUpdates()
                .filter { routeProgress -> isRefreshedRouteDistance(routeProgress) }
                .first()
            val roadObjectsFromObserver = mapboxNavigation.roadObjectsOnRoute()
                .filter { upcomingRoadObjects ->
                    upcomingRoadObjects.size == 2 &&
                        upcomingRoadObjects.map { it.roadObject.id }
                            .containsAll(listOf("11589180127444257", "14158569638505033"))
                }
                .first()

            assertEquals(roadObjectsFromObserver, refreshedRoutes.first().upcomingRoadObjects)
            assertEquals(
                routeProgress.navigationRoute.upcomingRoadObjects,
                refreshedRoutes.first().upcomingRoadObjects
            )
            assertEquals(
                "the test works only with 2 routes",
                2,
                requestedRoutes.size
            )
            // incidents
            assertEquals(
                listOf("11589180127444257"),
                initialRoutes[0].getIncidentsIdFromTheRoute(0)
            )
            assertEquals(
                listOf("11589180127444257", "14158569638505033").sorted(),
                refreshedRoutes[0].getIncidentsIdFromTheRoute(0)?.sorted()
            )
            assertEquals(
                listOf("11589180127444257"),
                initialRoutes[1].getIncidentsIdFromTheRoute(0)
            )
            assertEquals(
                listOf("11589180127444257", "14158569638505033").sorted(),
                refreshedRoutes[1].getIncidentsIdFromTheRoute(0)?.sorted()
            )
            // closures
            assertEquals(
                listOf(
                    Closure.builder()
                        .geometryIndexStart(5)
                        .geometryIndexEnd(6)
                        .build()
                ),
                initialRoutes[0].directionsRoute.legs()!![0].closures()
            )
            assertEquals(
                null,
                initialRoutes[1].directionsRoute.legs()!![0].closures()
            )
            assertEquals(
                listOf(
                    Closure.builder()
                        .geometryIndexStart(1)
                        .geometryIndexEnd(3)
                        .build()
                ),
                refreshedRoutes[0].directionsRoute.legs()!![0].closures()
            )
            assertEquals(
                listOf(
                    Closure.builder()
                        .geometryIndexStart(1)
                        .geometryIndexEnd(3)
                        .build()
                ),
                refreshedRoutes[1].directionsRoute.legs()!![0].closures()
            )

            assertEquals(
                "initial should be the same as requested",
                requestedRoutes[0].getDurationAnnotationsFromLeg(0),
                initialRoutes[0].getDurationAnnotationsFromLeg(0)
            )
            assertEquals(
                227.918,
                initialRoutes[0].getSumOfDurationAnnotationsFromLeg(0),
                0.0001
            )
            assertEquals(
                287.063,
                refreshedRoutes[0].getSumOfDurationAnnotationsFromLeg(0),
                0.0001
            )
            assertEquals(
                287.063,
                refreshedRoutes[0].directionsRoute.duration(),
                0.0001
            )
            assertEquals(
                287.063,
                refreshedRoutes[0].directionsRoute.legs()!!.first().duration()!!,
                0.0001
            )

            assertEquals(
                requestedRoutes[1].getSumOfDurationAnnotationsFromLeg(0),
                initialRoutes[1].getSumOfDurationAnnotationsFromLeg(0),
                0.0
            )
            assertEquals(
                224.2239,
                initialRoutes[1].getSumOfDurationAnnotationsFromLeg(0),
                0.0001
            )
            assertEquals(
                258.767,
                refreshedRoutes[1].getSumOfDurationAnnotationsFromLeg(0),
                0.0001
            )
            assertEquals(258.767, refreshedRoutes[1].directionsRoute.duration(), 0.0001)
            assertEquals(
                258.767,
                refreshedRoutes[1].directionsRoute.legs()!!.first().duration()!!,
                0.0001
            )
            assertEquals(
                requestedRoutes[0].directionsResponse.waypoints(),
                refreshedRoutes[0].directionsResponse.waypoints()
            )
            assertEquals(
                requestedRoutes[1].directionsResponse.waypoints(),
                refreshedRoutes[1].directionsResponse.waypoints()
            )
        }

    @Test
    fun routeRefreshesWorksAfterSettingsNewRoutes() = sdkTest {
        val routeOptions = generateRouteOptions(twoCoordinates)
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()

        waitForRouteToSuccessfullyRefresh()
        mapboxNavigation.clearNavigationRoutesAndWaitForUpdate()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        waitForRouteToSuccessfullyRefresh()
    }

    @Test
    fun routeSuccessfullyRefreshesAfterInvalidationOfExpiringData() = sdkTest {
        val routeOptions = generateRouteOptions(twoCoordinates)
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        failByRequestRouteRefreshResponse.failResponse = true
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        // act
        val refreshedRoutes = mapboxNavigation.routesUpdates()
            .filter { it.reason == ROUTES_UPDATE_REASON_REFRESH }
            .map { it.navigationRoutes }
            .first()
        val refreshedRouteCongestions = refreshedRoutes
            .first()
            .directionsRoute
            .legs()
            ?.firstOrNull()
            ?.annotation()
            ?.congestion()
        assertTrue(
            "expected unknown congestions, but they were $refreshedRouteCongestions",
            refreshedRouteCongestions?.all { it == "unknown" } ?: false
        )
        failByRequestRouteRefreshResponse.failResponse = false
        waitForRouteToSuccessfullyRefresh()
    }

    @Test
    fun routeAlternativeMetadataUpdatedAlongWithOnlyPrimaryRouteRefresh() = sdkTest {
        val routeOptions = generateRouteOptions(twoCoordinates)
        mockWebServerRule.requestHandlers.remove(failByRequestRouteRefreshResponse)
        mockWebServerRule.requestHandlers.add(
            FailByRequestMockRequestHandler(
                MockDirectionsRefreshHandler(
                    "route_response_route_refresh",
                    readRawFileText(activity, R.raw.route_response_route_refresh_annotations),
                    // it will fail for alternative refresh since index will be 1
                    routeIndex = 0
                )
            )
        )
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        val alternativesMetadataLegacy = mapboxNavigation.getAlternativeMetadataFor(routes).first()
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.routesUpdates()
            .first { it.reason == ROUTES_UPDATE_REASON_REFRESH }

        val alternativesMetadata = mapboxNavigation.getAlternativeMetadataFor(
            mapboxNavigation.getNavigationRoutes()
        ).first()

        assertNotNull(alternativesMetadataLegacy)
        assertNotNull(alternativesMetadata)
        assertEquals(
            alternativesMetadataLegacy.navigationRoute.id,
            alternativesMetadata.navigationRoute.id
        )
        assertNotEquals(alternativesMetadataLegacy, alternativesMetadata)
        assertEquals(227.918, alternativesMetadataLegacy.infoFromStartOfPrimary.duration, 0.001)
        // fork index is 4. So take 4 durations from primary refresh response
        // and add missing durations from the original alternative response.
        assertEquals(235.048, alternativesMetadata.infoFromStartOfPrimary.duration, 0.001)
    }

    @Test
    fun routeAlternativeMetadataUpdatedAlongWithRouteRefresh() = sdkTest {
        val routeOptions = generateRouteOptions(twoCoordinates)
        setUpSeparateRefreshHandlersForPrimaryAndAlternative()
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        val alternativesMetadataLegacy = mapboxNavigation.getAlternativeMetadataFor(routes).first()
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.routesUpdates()
            .first { it.reason == ROUTES_UPDATE_REASON_REFRESH }

        val alternativesMetadata = mapboxNavigation.getAlternativeMetadataFor(
            mapboxNavigation.getNavigationRoutes()
        ).first()

        assertNotNull(alternativesMetadataLegacy)
        assertNotNull(alternativesMetadata)
        assertEquals(
            alternativesMetadataLegacy.navigationRoute.id,
            alternativesMetadata.navigationRoute.id
        )
        assertNotEquals(alternativesMetadataLegacy, alternativesMetadata)
        assertEquals(227.918, alternativesMetadataLegacy.infoFromStartOfPrimary.duration, 0.001)
        // fork index is 4. So take 4 durations from primary refresh response
        // (they should be the same as in alternative refresh response)
        // and add missing durations from the alternative refresh response.
        assertEquals(285.185, alternativesMetadata.infoFromStartOfPrimary.duration, 0.001)
    }

    @Test
    fun expect_route_refresh_to_update_all_native_routes() = sdkTest {
        val routeOptions = generateRouteOptions(twoCoordinates)
        setUpSeparateRefreshHandlersForPrimaryAndAlternative()
        val routes = mapboxNavigation.requestRoutes(routeOptions)
            .getSuccessfulResultOrThrowException()
            .routes
        mapboxNavigation.startTripSession()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)
        stayOnInitialPosition()
        val oldAlternative = mapboxNavigation.getNavigationRoutes()[1]
        val oldInfo = mapboxNavigation.getAlternativeMetadataFor(oldAlternative)!!.infoFromFork
        mapboxNavigation.routesUpdates()
            .first { it.reason == ROUTES_UPDATE_REASON_REFRESH }
        val newAlternative = mapboxNavigation.getNavigationRoutes()[1]

        assertNotEquals(
            oldInfo,
            mapboxNavigation.getAlternativeMetadataFor(newAlternative)!!.infoFromFork
        )
    }

    @Test
    fun expect_route_refresh_to_update_annotations_for_truncated_current_leg() =
        sdkTest {
            setupMockRequestHandlers(
                twoCoordinates,
                R.raw.route_response_route_refresh,
                R.raw.route_response_route_refresh_truncated_first_leg,
                "route_response_route_refresh",
                acceptedGeometryIndex = 3
            )
            val routeOptions = generateRouteOptions(twoCoordinates)
            val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes

            mapboxNavigation.setNavigationRoutes(requestedRoutes)
            mapboxNavigation.startTripSession()
            // corresponds to currentRouteGeometryIndex = 3
            stayOnPosition(38.577344, -121.496248)
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentRouteGeometryIndex == 3 }
                .first()
            val refreshedRoutes = mapboxNavigation.routesUpdates()
                .filter {
                    it.reason == ROUTES_UPDATE_REASON_REFRESH
                }
                .first()
                .navigationRoutes

            assertEquals(224.224, requestedRoutes[0].getSumOfDurationAnnotationsFromLeg(0), 0.0001)
            assertEquals(172.175, refreshedRoutes[0].getSumOfDurationAnnotationsFromLeg(0), 0.0001)

            assertEquals(227.918, requestedRoutes[1].getSumOfDurationAnnotationsFromLeg(0), 0.0001)
            assertEquals(235.641, refreshedRoutes[1].getSumOfDurationAnnotationsFromLeg(0), 0.0001)
        }

    @Test
    fun route_refresh_updates_annotations_for_new_alternative_with_different_number_of_legs() =
        sdkTest {
            setupMockRequestHandlers(
                multilegCoordinates,
                R.raw.route_response_single_route_multileg,
                R.raw.route_response_single_route_multileg_refreshed,
                "route_response_single_route_multileg",
                acceptedGeometryIndex = 70
            )
            mockWebServerRule.requestHandlers.add(
                FailByRequestMockRequestHandler(
                    MockDirectionsRefreshHandler(
                        "route_response_single_route_multileg_alternative",
                        readRawFileText(
                            activity,
                            R.raw.route_response_single_route_multileg_alternative_refreshed
                        ),
                        acceptedGeometryIndex = 11
                    )
                )
            )
            val routeOptions = generateRouteOptions(multilegCoordinates)
            val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            // alternative which was requested on the second leg of the original route,
            // so the alternative has only one leg while the original route has two
            val alternativeRoute = alternativeForMultileg(activity).toNavigationRoutes().first()

            mapboxNavigation.setNavigationRoutes(requestedRoutes, initialLegIndex = 1)
            mapboxNavigation.startTripSession()

            // corresponds to currentRouteGeometryIndex = 70 for primary route and 11 for alternative route
            stayOnPosition(38.581798, -121.476146)
            mapboxNavigation.routeProgressUpdates()
                .filter {
                    it.currentRouteGeometryIndex == 70
                }
                .first()

            mapboxNavigation.setNavigationRoutesAndWaitForAlternativesUpdate(
                requestedRoutes + alternativeRoute,
                initialLegIndex = 1
            )

            val refreshedRoutes = mapboxNavigation.routesUpdates()
                .filter {
                    it.reason == ROUTES_UPDATE_REASON_REFRESH
                }
                .first()
                .navigationRoutes

            assertEquals(
                requestedRoutes[0].getSumOfDurationAnnotationsFromLeg(0),
                refreshedRoutes[0].getSumOfDurationAnnotationsFromLeg(0),
                0.0001
            )

            assertEquals(201.673, requestedRoutes[0].getSumOfDurationAnnotationsFromLeg(1), 0.0001)
            assertEquals(202.881, refreshedRoutes[0].getSumOfDurationAnnotationsFromLeg(1), 0.0001)

            assertEquals(194.3, alternativeRoute.getSumOfDurationAnnotationsFromLeg(0), 0.0001)
            assertEquals(187.126, refreshedRoutes[1].getSumOfDurationAnnotationsFromLeg(0), 0.0001)
        }

    @Test
    fun route_refresh_updates_annotations_for_new_alternative_with_more_legs() =
        sdkTest {
            setupMockRequestHandlers(
                multilegCoordinates,
                R.raw.route_response_single_route_multileg,
                R.raw.route_response_single_route_multileg_refreshed,
                "route_response_single_route_multileg",
                acceptedGeometryIndex = 70
            )
            mockWebServerRule.requestHandlers.add(
                FailByRequestMockRequestHandler(
                    MockDirectionsRefreshHandler(
                        "route_response_single_route_multileg_alternative",
                        readRawFileText(
                            activity,
                            R.raw.route_response_single_route_multileg_alternative_refreshed
                        ),
                        acceptedGeometryIndex = 11
                    )
                )
            )
            val routeOptions = generateRouteOptions(multilegCoordinates)
            val alternativeRoutes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes
            // alternative which was requested on the second leg of the original route,
            // so the alternative has only one leg while the original route has two
            val primaryRoute = alternativeForMultileg(activity).toNavigationRoutes().first()

            // corresponds to currentRouteGeometryIndex = 70 for alternative route and 11 for the primary route
            mockLocationUpdatesRule.pushLocationUpdate(
                mockLocationUpdatesRule.generateLocationUpdate {
                latitude = 38.581798
                longitude = -121.476146
                }
            )

            mapboxNavigation.setNavigationRoutes(listOf(primaryRoute) + alternativeRoutes, initialLegIndex = 0)
            mapboxNavigation.startTripSession()

            mapboxNavigation.routeProgressUpdates()
                .filter {
                    it.currentRouteGeometryIndex == 11
                }
                .first()

            val refreshedRoutes = mapboxNavigation.routesUpdates()
                .filter {
                    it.reason == ROUTES_UPDATE_REASON_REFRESH
                }
                .first()
                .navigationRoutes

            assertEquals(
                alternativeRoutes[0].getSumOfDurationAnnotationsFromLeg(0),
                refreshedRoutes[1].getSumOfDurationAnnotationsFromLeg(0),
                0.0001
            )

            assertEquals(201.673, alternativeRoutes[0].getSumOfDurationAnnotationsFromLeg(1), 0.0001)
            assertEquals(202.881, refreshedRoutes[1].getSumOfDurationAnnotationsFromLeg(1), 0.0001)

            assertEquals(194.3, primaryRoute.getSumOfDurationAnnotationsFromLeg(0), 0.0001)
            assertEquals(187.126, refreshedRoutes[0].getSumOfDurationAnnotationsFromLeg(0), 0.0001)
        }

    @Test
    fun expect_route_refresh_to_update_annotations_incidents_and_closures_for_truncated_next_leg() =
        sdkTest {
            setupMockRequestHandlers(
                threeCoordinates,
                R.raw.route_response_route_refresh_multileg,
                R.raw.route_response_route_refresh_truncated_next_leg,
                "route_response_route_refresh_multileg",
                acceptedGeometryIndex = 5
            )
            val routeOptions = generateRouteOptions(threeCoordinates)
            val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes

            mapboxNavigation.setNavigationRoutes(requestedRoutes)
            mapboxNavigation.startTripSession()
            // corresponds to currentRouteGeometryIndex = 5
            stayOnPosition(38.57622, -121.496731)
            mapboxNavigation.routeProgressUpdates()
                .filter { it.currentRouteGeometryIndex == 5 }
                .first()
            val refreshedRoutes = mapboxNavigation.routesUpdates()
                .filter {
                    it.reason == ROUTES_UPDATE_REASON_REFRESH
                }
                .first()
                .navigationRoutes

            // annotations
            assertEquals(201.673, requestedRoutes[0].getSumOfDurationAnnotationsFromLeg(1), 0.0001)
            assertEquals(189.086, refreshedRoutes[0].getSumOfDurationAnnotationsFromLeg(1), 0.0001)

            // incidents
            assertEquals(
                listOf(listOf("9457146989091490", 400, 405)),
                requestedRoutes[0].directionsRoute.legs()!![1].incidents()!!
                    .extract({ id() }, { geometryIndexStart() }, { geometryIndexEnd() })
            )
            assertEquals(
                listOf(listOf("9457146989091490", 396, 401)),
                refreshedRoutes[0].directionsRoute.legs()!![1].incidents()!!
                    .extract({ id() }, { geometryIndexStart() }, { geometryIndexEnd() })
            )

            // closures
            assertEquals(
                listOf(
                    Closure.builder()
                        .geometryIndexStart(312)
                        .geometryIndexEnd(313)
                        .build()
                ),
                requestedRoutes[0].directionsRoute.legs()!![1].closures()
            )
            assertEquals(
                listOf(
                    Closure.builder()
                        .geometryIndexStart(313)
                        .geometryIndexEnd(314)
                        .build()
                ),
                refreshedRoutes[0].directionsRoute.legs()!![1].closures()
            )

            // waypoints
            assertEquals(
                requestedRoutes[0].directionsResponse.waypoints(),
                refreshedRoutes[0].directionsResponse.waypoints()
            )
            assertEquals(
                requestedRoutes[1].directionsResponse.waypoints(),
                refreshedRoutes[1].directionsResponse.waypoints()
            )
        }

    @Test
    fun expect_route_refresh_to_update_annotations_incidents_and_closures_for_second_leg() =
        sdkTest {
            val currentRouteGeometryIndex = 2000
            // 437 points in leg #0, so currentLegGeometryIndex = 2000 - 437 + 1 (points are duplicated on the start and end of steps and legs) = 1564
            setupMockRequestHandlers(
                threeCoordinatesWithIncidents,
                R.raw.route_response_multileg_with_incidents,
                R.raw.route_response_route_refresh_multileg_with_incidents,
                "route_response_multileg_with_incidents",
                acceptedGeometryIndex = currentRouteGeometryIndex
            )
            val routeOptions = generateRouteOptions(threeCoordinatesWithIncidents)
            val requestedRoutes = mapboxNavigation.requestRoutes(routeOptions)
                .getSuccessfulResultOrThrowException()
                .routes

            mapboxNavigation.setNavigationRoutes(requestedRoutes, initialLegIndex = 1)
            mapboxNavigation.startTripSession()
            // corresponds to currentRouteGeometryIndex = 2000, currentLeg = 1
            stayOnPosition(39.80965, -75.281163)
            mapboxNavigation.routeProgressUpdates()
                .filter {
                    it.currentRouteGeometryIndex == currentRouteGeometryIndex
                }
                .first()
            val refreshedRoutes = mapboxNavigation.routesUpdates()
                .filter {
                    it.reason == ROUTES_UPDATE_REASON_REFRESH
                }
                .first()
                .navigationRoutes

            // annotations
            assertEquals(8595.694, requestedRoutes[0].getSumOfDurationAnnotationsFromLeg(1), 0.0001)
            assertEquals(8571.824, refreshedRoutes[0].getSumOfDurationAnnotationsFromLeg(1), 0.0001)

            // incidents
            assertEquals(
                listOf(
                    listOf("9457146989091490", 2019, 2024),
                    listOf("5945491930714919", 2044, 2126)
                ),
                requestedRoutes[0].directionsRoute.legs()!![1].incidents()!!
                    .extract({ id() }, { geometryIndexStart() }, { geometryIndexEnd() })
            )
            assertEquals(
                listOf(
                    listOf("9457146989091490", 2019, 2024),
                    listOf("5945491930714919", 2048, 2130)
                ),
                refreshedRoutes[0].directionsRoute.legs()!![1].incidents()!!
                    .extract({ id() }, { geometryIndexStart() }, { geometryIndexEnd() })
            )

            // closures
            assertEquals(
                listOf(
                    Closure.builder()
                        .geometryIndexStart(2001)
                        .geometryIndexEnd(2020)
                        .build()
                ),
                requestedRoutes[0].directionsRoute.legs()!![1].closures()
            )
            assertEquals(
                listOf(
                    Closure.builder()
                        .geometryIndexStart(2054)
                        .geometryIndexEnd(2061)
                        .build()
                ),
                refreshedRoutes[0].directionsRoute.legs()!![1].closures()
            )

            // waypoints
            assertEquals(
                requestedRoutes[0].directionsResponse.waypoints(),
                refreshedRoutes[0].directionsResponse.waypoints()
            )
        }

    private fun List<Incident>.extract(vararg extractors: Incident.() -> Any?): List<List<Any?>> {
        return map { incident ->
            extractors.map { extractor ->
                incident.extractor()
            }
        }
    }

    private fun stayOnInitialPosition() {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = twoCoordinates[0].latitude()
                longitude = twoCoordinates[0].longitude()
                bearing = 190f
            },
            times = 120
        )
    }

    private fun stayOnPosition(latitude: Double, longitude: Double) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                this.latitude = latitude
                this.longitude = longitude
                bearing = 190f
            },
            times = 120
        )
    }

    private suspend fun waitForRouteToSuccessfullyRefresh(): RouteProgress =
        mapboxNavigation.routeProgressUpdates()
            .filter { isRefreshedRouteDistance(it) }
            .first()

    private fun isRefreshedRouteDistance(it: RouteProgress): Boolean {
        val expectedDurationRemaining = 287.063
        // 30 seconds margin of error
        return (it.durationRemaining - expectedDurationRemaining).absoluteValue < 30
    }

    // Will be ignored when .baseUrl(mockWebServerRule.baseUrl) is commented out
    // in the requestDirectionsRouteSync function.
    private fun setupMockRequestHandlers(
        coordinates: List<Point>,
        @IntegerRes routesResponse: Int,
        @IntegerRes refreshResponse: Int,
        responseTestUuid: String,
        acceptedGeometryIndex: Int? = null,
    ) {
        mockWebServerRule.requestHandlers.clear()
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, routesResponse),
                coordinates
            )
        )
        failByRequestRouteRefreshResponse = FailByRequestMockRequestHandler(
            MockDirectionsRefreshHandler(
                responseTestUuid,
                readRawFileText(activity, refreshResponse),
                acceptedGeometryIndex
            )
        )
        mockWebServerRule.requestHandlers.add(failByRequestRouteRefreshResponse)
        mockWebServerRule.requestHandlers.add(
            MockRoutingTileEndpointErrorRequestHandler()
        )
    }

    private fun generateRouteOptions(coordinates: List<Point>): RouteOptions {
        return RouteOptions.builder().applyDefaultNavigationOptions()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .alternatives(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .build()
    }

    private fun setUpSeparateRefreshHandlersForPrimaryAndAlternative() {
        mockWebServerRule.requestHandlers.remove(failByRequestRouteRefreshResponse)
        mockWebServerRule.requestHandlers.add(
            FailByRequestMockRequestHandler(
                MockDirectionsRefreshHandler(
                    "route_response_route_refresh",
                    readRawFileText(activity, R.raw.route_response_route_refresh_annotations),
                    routeIndex = 0
                )
            )
        )
        mockWebServerRule.requestHandlers.add(
            FailByRequestMockRequestHandler(
                MockDirectionsRefreshHandler(
                    "route_response_route_refresh",
                    readRawFileText(
                        activity,
                        R.raw.route_response_route_refresh_alternative_annotations
                    ),
                    routeIndex = 1
                )
            )
        )
    }

    private fun alternativeForMultileg(context: Context): MockRoute {
        val jsonResponse = readRawFileText(context, R.raw.route_response_single_route_multileg_alternative)
        val coordinates = listOf(
            Point.fromLngLat(38.577427, -121.478077),
            Point.fromLngLat(38.582195, -121.468458)
        )
        return MockRoute(
            jsonResponse,
            DirectionsResponse.fromJson(jsonResponse),
            listOf(
                MockDirectionsRequestHandler(
                    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                    jsonResponse = jsonResponse,
                    expectedCoordinates = coordinates
                )
            ),
            coordinates,
            emptyList()
        )
    }
}

private fun NavigationRoute.getSumOfDurationAnnotationsFromLeg(legIndex: Int): Double =
    getDurationAnnotationsFromLeg(legIndex)
        ?.sum()!!

private fun NavigationRoute.getDurationAnnotationsFromLeg(legIndex: Int): List<Double>? =
    directionsRoute.legs()?.get(legIndex)
        ?.annotation()
        ?.duration()

private fun NavigationRoute.getIncidentsIdFromTheRoute(legIndex: Int): List<String>? =
    directionsRoute.legs()?.get(legIndex)
        ?.incidents()
        ?.map { it.id() }
