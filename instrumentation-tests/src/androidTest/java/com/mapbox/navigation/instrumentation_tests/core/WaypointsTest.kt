package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.annotation.IdRes
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.LegWaypoint
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.ApproximateCoordinates
import com.mapbox.navigation.testing.utils.assertions.waitUntilHasSize
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.readRawFileText
import com.mapbox.navigation.testing.utils.toApproximateCoordinates
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.URI
import kotlin.math.abs

class WaypointsTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private val evCoordinates = listOf(
        Point.fromLngLat(11.5852259, 48.1760993),
        Point.fromLngLat(10.3406374, 49.16479),
    )

    private val nonEvCoordinates = listOf(
        Point.fromLngLat(-121.496066, 38.577764),
        Point.fromLngLat(-121.480279, 38.57674),
        Point.fromLngLat(-121.468434, 38.58225),
    )

    private lateinit var mapboxNavigation: MapboxNavigation
    private val tolerance = 0.0001
    private val expectedEvWaypointsNamesAndLocations = listOf(
        "Leopoldstraße" to ApproximateCoordinates(48.176099, 11.585226, tolerance),
        "" to ApproximateCoordinates(48.39023, 11.063842, tolerance),
        "" to ApproximateCoordinates(49.164725, 10.340713, tolerance),
    )
    private val expectedFirstNonEvWaypointsNamesAndLocations = listOf(
        "9th Street" to ApproximateCoordinates(38.577764, -121.496066, tolerance),
        "J Street" to ApproximateCoordinates(38.576795, -121.480256, tolerance),
        "C Street" to ApproximateCoordinates(38.582195, -121.468458, tolerance),
    )
    private val expectedSecondNonEvWaypointsNamesAndLocations = listOf(
        "9th Street" to ApproximateCoordinates(38.577764, -121.496066, tolerance),
        "J Street" to ApproximateCoordinates(38.576795, -121.480256, tolerance),
        "C Street changed for tests" to ApproximateCoordinates(38.582195, -121.468458, tolerance),
    )

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = nonEvCoordinates[0].latitude()
        longitude = nonEvCoordinates[0].longitude()
        bearing = 190f
    }

    @Before
    fun setup() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .routingTilesOptions(
                        RoutingTilesOptions.Builder()
                            .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                            .build(),
                    )
                    .navigatorPredictionMillis(0L)
                    .build(),
            )
            mockWebServerRule.requestHandlers.clear()
        }
    }

    @Test
    fun ev_route_with_waypoints_in_response_root_by_default() = sdkTest {
        addResponseHandler(R.raw.ev_route_response_with_waypoints_in_root, evCoordinates)
        val routes = requestRoutes(evCoordinates, electric = true, waypointsPerRoute = null)

        checkWaypointsInRoot(expectedEvWaypointsNamesAndLocations, routes[0])
    }

    @Test
    fun ev_route_with_waypoints_in_response_root() = sdkTest {
        addResponseHandler(R.raw.ev_route_response_with_waypoints_in_root, evCoordinates)
        val routes = requestRoutes(evCoordinates, electric = true, waypointsPerRoute = false)

        checkWaypointsInRoot(expectedEvWaypointsNamesAndLocations, routes[0])
    }

    @Test
    fun ev_route_with_waypoints_per_route() = sdkTest {
        addResponseHandler(R.raw.ev_route_response_with_waypoints_per_route, evCoordinates)
        val routes = requestRoutes(evCoordinates, electric = true, waypointsPerRoute = true)

        checkWaypointsPerRoute(expectedEvWaypointsNamesAndLocations, routes[0])
    }

    @Test
    fun non_ev_route_with_waypoints_in_response_root_by_default() = sdkTest {
        addResponseHandler(R.raw.route_response_with_waypoints_in_root, nonEvCoordinates)
        val routes = requestRoutes(nonEvCoordinates, electric = false, waypointsPerRoute = null)

        checkWaypointsInRoot(expectedFirstNonEvWaypointsNamesAndLocations, routes[0])
        checkWaypointsInRoot(expectedFirstNonEvWaypointsNamesAndLocations, routes[1])
    }

    @Test
    fun non_ev_route_with_waypoints_in_response_root() = sdkTest {
        addResponseHandler(R.raw.route_response_with_waypoints_in_root, nonEvCoordinates)
        val routes = requestRoutes(nonEvCoordinates, electric = false, waypointsPerRoute = false)

        checkWaypointsInRoot(expectedFirstNonEvWaypointsNamesAndLocations, routes[0])
        checkWaypointsInRoot(expectedFirstNonEvWaypointsNamesAndLocations, routes[1])
    }

    @Test
    fun non_ev_route_with_waypoints_per_route() = sdkTest {
        addResponseHandler(R.raw.route_response_with_waypoints_per_route, nonEvCoordinates)
        val routes = requestRoutes(nonEvCoordinates, electric = false, waypointsPerRoute = true)

        checkWaypointsPerRoute(expectedFirstNonEvWaypointsNamesAndLocations, routes[0])
        checkWaypointsPerRoute(expectedSecondNonEvWaypointsNamesAndLocations, routes[1])
    }

    @Test
    fun leg_destination_non_ev_route() = sdkTest {
        val coordinates = listOf(
            Point.fromLngLat(140.025878, 35.660315),
            Point.fromLngLat(140.02985194436837, 35.6621859075361),
            Point.fromLngLat(140.0277017481984, 35.65792632910045),
            Point.fromLngLat(140.038772, 35.660329),
            Point.fromLngLat(140.0231453915486, 35.667495318461164),
            Point.fromLngLat(140.03969561587877, 35.67009382118668),
        )
        addResponseHandler(R.raw.route_response_with_many_waypoints, coordinates)
        stayOnPosition(coordinates[0], 270f)
        mapboxNavigation.startTripSession()
        mapboxNavigation.flowLocationMatcherResult().filter {
            abs(it.enhancedLocation.latitude - coordinates[0].latitude()) < 0.01 &&
                abs(it.enhancedLocation.longitude - coordinates[0].longitude()) < 0.01
        }.take(3).toList()
        val routes = mapboxNavigation.requestRoutes(
            generateRouteOptions(coordinates, electric = false, waypointsPerRoute = false)
                .toBuilder()
                .waypointIndicesList(listOf(0, 1, 3, 5))
                .build(),
        )
            .getSuccessfulResultOrThrowException()
            .routes

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)

        val nextWaypoints = mutableListOf<LegWaypoint?>()
        mapboxNavigation.registerArrivalObserver(
            object : ArrivalObserver {
                override fun onWaypointArrival(routeProgress: RouteProgress) {
                    nextWaypoints.add(routeProgress.currentLegProgress?.legDestination)
                }

                override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
                }

                override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
                }
            },
        )
        stayOnPosition(coordinates[1], 90f)
        nextWaypoints.waitUntilHasSize(1)
        var legWaypoint = nextWaypoints[0]!!

        checkLocation(coordinates[1], legWaypoint.location)
        assertEquals(LegWaypoint.REGULAR, legWaypoint.type)

        stayOnPosition(coordinates[2], 270f)
        delay(1000)
        assertEquals(1, nextWaypoints.size)

        stayOnPosition(coordinates[3], 180f)
        nextWaypoints.waitUntilHasSize(2)
        legWaypoint = nextWaypoints[1]!!
        checkLocation(coordinates[3], legWaypoint.location)
        assertEquals(LegWaypoint.REGULAR, legWaypoint.type)

        stayOnPosition(coordinates[4], 45f)
        delay(1000)
        assertEquals(2, nextWaypoints.size)
    }

    @Test
    fun leg_destination_ev_route() = sdkTest {
        val coordinates = listOf(
            Point.fromLngLat(48.39023, 11.063842),
            Point.fromLngLat(49.164725, 10.340713),
        )
        addResponseHandler(R.raw.ev_route_response_for_refresh_with_2_waypoints, coordinates)
        stayOnPosition(coordinates[0])
        mapboxNavigation.startTripSession()
        mapboxNavigation.flowLocationMatcherResult().filter {
            abs(it.enhancedLocation.latitude - coordinates[0].latitude()) < 0.01 &&
                abs(it.enhancedLocation.longitude - coordinates[0].longitude()) < 0.01
        }.take(3).toList()
        val routes = requestRoutes(coordinates, electric = true, waypointsPerRoute = false)

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(routes)

        val nextWaypoints = mutableListOf<LegWaypoint?>()
        mapboxNavigation.registerArrivalObserver(
            object : ArrivalObserver {
                override fun onWaypointArrival(routeProgress: RouteProgress) {
                    nextWaypoints.add(routeProgress.currentLegProgress?.legDestination)
                }

                override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
                }

                override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
                }
            },
        )
        stayOnPosition(routes[0].waypoints!![1].location(), 315f)
        nextWaypoints.waitUntilHasSize(1)
        var legWaypoint = nextWaypoints[0]!!

        checkLocation(
            routes[0].waypoints!![1].location(),
            legWaypoint.location,
        )
        assertEquals(LegWaypoint.EV_CHARGING_ADDED, legWaypoint.type)

        stayOnPosition(routes[0].waypoints!![2].location(), 0f)
        nextWaypoints.waitUntilHasSize(2)
        legWaypoint = nextWaypoints[1]!!

        checkLocation(
            routes[0].waypoints!![2].location(),
            legWaypoint.location,
        )
        assertEquals(LegWaypoint.EV_CHARGING_ADDED, legWaypoint.type)
    }

    private fun checkLocation(expected: Point, actual: Point) {
        assertEquals(expected.latitude(), actual.latitude(), 0.00001)
        assertEquals(expected.longitude(), actual.longitude(), 0.00001)
    }

    private fun stayOnPosition(point: Point, bearing: Float = 0f) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                latitude = point.latitude()
                longitude = point.longitude()
                this.bearing = bearing
            },
            100,
        )
    }

    private suspend fun requestRoutes(
        coordinates: List<Point>,
        electric: Boolean,
        waypointsPerRoute: Boolean? = null,
    ): List<NavigationRoute> {
        return mapboxNavigation.requestRoutes(
            generateRouteOptions(coordinates, electric, waypointsPerRoute),
        )
            .getSuccessfulResultOrThrowException()
            .routes
    }

    private fun generateRouteOptions(
        coordinates: List<Point>,
        electric: Boolean,
        waypointsPerRoute: Boolean?,
    ): RouteOptions {
        return RouteOptions.builder().applyDefaultNavigationOptions()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .alternatives(true)
            .coordinatesList(coordinates)
            .waypointsPerRoute(waypointsPerRoute)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .apply {
                if (electric) {
                    annotations("state_of_charge")
                    unrecognizedProperties(
                        mapOf(
                            "engine" to "electric",
                            "ev_initial_charge" to "18000",
                            "energy_consumption_curve" to "0,300;20,160;80,140;120,180",
                            "ev_pre_conditioning_time" to "10",
                            "ev_min_charge_at_charging_station" to "6000",
                            "ev_min_charge_at_destination" to "6000",
                            "ev_max_charge" to "60000",
                            "ev_connector_types" to "ccs_combo_type1,ccs_combo_type2",
                            "energy_consumption_curve" to "0,300;20,160;80,140;120,180",
                            "ev_charging_curve" to "0,100000;40000,70000;60000,30000;80000,10000",
                            "ev_max_ac_charging_power" to "14400",
                            "ev_unconditioned_charging_curve" to
                                "0,50000;42000,35000;60000,15000;80000,5000",
                            "auxiliary_consumption" to "300",
                        ),
                    )
                }
            }
            .build()
    }

    private fun checkWaypointsInRoot(
        expected: List<Pair<String, ApproximateCoordinates>>,
        route: NavigationRoute,
    ) {
        assertEquals(
            expected,
            route.waypoints!!.map {
                it.name() to it.location().toApproximateCoordinates(tolerance)
            },
        )
        assertEquals(
            expected,
            route.internalWaypoints().map {
                it.name to it.location.toApproximateCoordinates(tolerance)
            },
        )
        assertEquals(
            expected,
            route.waypoints!!.map {
                it.name() to it.location().toApproximateCoordinates(tolerance)
            },
        )
        assertNull(route.directionsRoute.waypoints())
    }

    private fun checkWaypointsPerRoute(
        expected: List<Pair<String, ApproximateCoordinates>>,
        route: NavigationRoute,
    ) {
        assertEquals(
            expected,
            route.waypoints!!.map {
                it.name() to it.location().toApproximateCoordinates(tolerance)
            },
        )
        assertEquals(
            expected,
            route.internalWaypoints().map {
                it.name to it.location.toApproximateCoordinates(tolerance)
            },
        )
        assertEquals(
            expected,
            route.directionsRoute.waypoints()!!.map {
                it.name() to it.location().toApproximateCoordinates(tolerance)
            },
        )
    }

    private fun addResponseHandler(@IdRes fileId: Int, coordinates: List<Point>) {
        val routeHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(activity, fileId),
            coordinates,
            relaxedExpectedCoordinates = true,
        )
        mockWebServerRule.requestHandlers.add(routeHandler)
    }
}
