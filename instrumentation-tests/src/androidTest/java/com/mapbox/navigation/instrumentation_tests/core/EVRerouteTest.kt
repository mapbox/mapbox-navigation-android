package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import android.util.Log
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.navigateNextRouteLeg
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.testing.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.location.moveAlongTheRouteUntilTracking
import com.mapbox.navigation.testing.utils.nativeRerouteControllerNoRetryConfig
import com.mapbox.navigation.testing.utils.readRawFileText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URI

private const val KEY_ENGINE = "engine"
private const val KEY_ENERGY_CONSUMPTION_CURVE = "energy_consumption_curve"
private const val KEY_EV_INITIAL_CHARGE = "ev_initial_charge"
private const val KEY_AUXILIARY_CONSUMPTION = "auxiliary_consumption"
private const val KEY_EV_PRECONDITIONING_TIME = "ev_pre_conditioning_time"
private const val VALUE_ELECTRIC = "electric"
private const val KEY_WAYPOINTS_POWER = "waypoints.charging_station_power"
private const val KEY_WAYPOINTS_CURRENT_TYPE = "waypoints.charging_station_current_type"
private const val KEY_WAYPOINTS_STATION_ID = "waypoints.charging_station_id"
private val userProvidedCpoiKeys = setOf(
    KEY_WAYPOINTS_POWER,
    KEY_WAYPOINTS_CURRENT_TYPE,
    KEY_WAYPOINTS_STATION_ID,
)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(Parameterized::class)
class EVRerouteTest(
    private val runOptions: RerouteTestRunOptions,
) : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    data class RerouteTestRunOptions(
        val nativeReroute: Boolean,
    ) {
        override fun toString(): String {
            return if (nativeReroute) {
                "native reroute"
            } else {
                "platform reroute"
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            RerouteTestRunOptions(nativeReroute = false),
            RerouteTestRunOptions(nativeReroute = true),
        )
    }

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private val twoCoordinates = listOf(
        Point.fromLngLat(11.5852259, 48.1760993),
        Point.fromLngLat(10.3406374, 49.16479),
    )

    private val threeCoordinates = listOf(
        Point.fromLngLat(11.585226, 48.176099),
        Point.fromLngLat(11.063842, 48.39023),
        Point.fromLngLat(10.32645, 49.069138),
    )

    private val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = twoCoordinates[0].latitude() + 0.004
        longitude = twoCoordinates[0].longitude()
    }

    private lateinit var routeHandler: MockDirectionsRequestHandler
    private val initialEnergyConsumptionCurve = "0,300;20,160;80,140;120,180"
    private val initialInitialCharge = "18000"
    private val initialAuxiliaryConsumption = "300"
    private val initialEvPreconditioningTime = "10"

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = twoCoordinates[0].latitude()
        longitude = twoCoordinates[0].longitude()
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
                    .deviceProfile(
                        DeviceProfile.Builder().customConfig(
                            getTestCustomConfig(),
                        ).build(),
                    )
                    .build(),
            )
            mockWebServerRule.requestHandlers.clear()
            mapboxHistoryTestRule.historyRecorder = mapboxNavigation.historyRecorder
            mapboxNavigation.historyRecorder.startRecording()
            routeHandler = MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.ev_route_response_for_reroute_1),
                twoCoordinates,
                relaxedExpectedCoordinates = true,
            )
            mockWebServerRule.requestHandlers.add(routeHandler)
        }
    }

    @After
    fun after() {
        runBlocking {
            val path = mapboxNavigation.historyRecorder.stopRecording()
            Log.i("Test history file", "history file recorder: $path")
        }
    }

    private fun getTestCustomConfig(): String = if (runOptions.nativeReroute) {
        nativeRerouteControllerNoRetryConfig
    } else {
        ""
    }

    /**
     * Verifies that when no EV data has been explicitly provided via
     * [MapboxNavigation.onEVDataUpdated], the reroute request uses the EV parameters from the
     * original route options (engine type, energy consumption curve, initial charge, auxiliary
     * consumption, and pre-conditioning time). CPOI (charging station) waypoint parameters must
     * not be included in the request. Also verifies the behavior persists after setting a new
     * route with a different initial charge.
     */
    @Test
    fun ev_reroute_parameters_for_ev_route_with_no_ev_data() = sdkTest {
        val requestedRoutes = requestRoutes(
            twoCoordinates,
            electric = true,
            waypointsPerRoute = true,
        )

        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        mapboxNavigation.moveAlongTheRouteUntilTracking(
            requestedRoutes.first(),
            mockLocationReplayerRule,
        )
        stayOnPosition(offRouteLocationUpdate.latitude, offRouteLocationUpdate.longitude)
        waitForReroute()

        val url1 = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            url1,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
                KEY_EV_INITIAL_CHARGE to initialInitialCharge,
                KEY_AUXILIARY_CONSUMPTION to initialAuxiliaryConsumption,
                KEY_EV_PRECONDITIONING_TIME to initialEvPreconditioningTime,
            ),
        )
        checkDoesNotHaveParameters(url1, userProvidedCpoiKeys)

        val newInitialCharge = "18000"
        val newRequestedRoutes = requestRoutes(
            twoCoordinates,
            electric = true,
            initialCharge = newInitialCharge,
            waypointsPerRoute = true,
        )
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(newRequestedRoutes)
        mapboxNavigation.moveAlongTheRouteUntilTracking(
            newRequestedRoutes.first(),
            mockLocationReplayerRule,
            minEventsCount = 1,
        )
        stayOnPosition(offRouteLocationUpdate.latitude, offRouteLocationUpdate.longitude)
        waitForReroute()

        val url2 = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            url2,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
                KEY_EV_INITIAL_CHARGE to newInitialCharge,
                KEY_AUXILIARY_CONSUMPTION to initialAuxiliaryConsumption,
                KEY_EV_PRECONDITIONING_TIME to initialEvPreconditioningTime,
            ),
        )
        checkDoesNotHaveParameters(url2, userProvidedCpoiKeys)
    }

    /**
     * Verifies that EV parameters provided via [MapboxNavigation.onEVDataUpdated] override the
     * route option defaults in the reroute request. The reroute URL must include the updated
     * consumption curve, initial charge, pre-conditioning time, and auxiliary consumption values.
     * CPOI (charging station) waypoint parameters must not be included in the request.
     */
    @Test
    fun ev_reroute_parameters_for_ev_route_with_ev_data() = sdkTest {
        val requestedRoutes = requestRoutes(
            twoCoordinates,
            electric = true,
            waypointsPerRoute = true,
        )

        val consumptionCurve = "0,300;20,120;40,150"
        val initialCharge = "80"
        val preconditioningTime = "10"
        val auxiliaryConsumption = "300"
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
            KEY_EV_INITIAL_CHARGE to initialCharge,
            KEY_EV_PRECONDITIONING_TIME to preconditioningTime,
            KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption,
        )
        mapboxNavigation.onEVDataUpdated(evData)

        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        mapboxNavigation.moveAlongTheRouteUntilTracking(
            requestedRoutes.first(),
            mockLocationReplayerRule,
        )
        stayOnPosition(offRouteLocationUpdate.latitude, offRouteLocationUpdate.longitude)
        waitForReroute()

        val url = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            url,
            evData + (KEY_ENGINE to VALUE_ELECTRIC),
        )
        checkDoesNotHaveParameters(url, userProvidedCpoiKeys)
    }

    /**
     * Verifies that multiple sequential [MapboxNavigation.onEVDataUpdated] calls accumulate
     * correctly and are reflected in subsequent reroute requests. Confirms that passing an
     * empty map to [MapboxNavigation.onEVDataUpdated] does not reset previously set values —
     * the last non-empty values must persist and appear in the next reroute request URL.
     */
    @Test
    fun ev_reroute_parameters_for_ev_route_with_ev_data_updates() = sdkTest {
        val requestedRoutes = requestRoutes(
            twoCoordinates,
            electric = true,
            waypointsPerRoute = true,
        )

        mockLocationReplayerRule.playRoute(requestedRoutes.first().directionsRoute)
        mapboxNavigation.startTripSession()

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        mapboxNavigation.routeProgressUpdates().first {
            it.currentState == RouteProgressState.TRACKING
        }
        mapboxNavigation.replanRoute()
        waitForReroute()

        val noDataRefreshUrl = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            noDataRefreshUrl,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
                KEY_EV_PRECONDITIONING_TIME to initialEvPreconditioningTime,
                KEY_EV_INITIAL_CHARGE to initialInitialCharge,
                KEY_EV_INITIAL_CHARGE to initialInitialCharge,
            ),
        )

        val consumptionCurve = "0,301;20,121;40,151"
        val initialCharge = "80"
        val preconditioningTime = "10"
        val auxiliaryConsumption = "299"
        val firstEvData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
            KEY_EV_INITIAL_CHARGE to initialCharge,
            KEY_EV_PRECONDITIONING_TIME to preconditioningTime,
            KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption,
        )
        mapboxNavigation.onEVDataUpdated(firstEvData)
        mapboxNavigation.routeProgressUpdates().first {
            it.currentState == RouteProgressState.TRACKING
        }
        var oldRequestsCount = routeHandler.handledRequests.size
        mapboxNavigation.replanRoute()
        waitForNewRequest(oldRequestsCount)

        val firstUrl = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            firstUrl,
            firstEvData + (KEY_ENGINE to VALUE_ELECTRIC),
        )
        checkDoesNotHaveParameters(firstUrl, userProvidedCpoiKeys)

        val newInitialCharge = "60"
        mapboxNavigation.onEVDataUpdated(
            mapOf(KEY_EV_INITIAL_CHARGE to newInitialCharge),
        )
        mapboxNavigation.routeProgressUpdates().first {
            it.currentState == RouteProgressState.TRACKING
        }
        oldRequestsCount = routeHandler.handledRequests.size
        mapboxNavigation.replanRoute()
        waitForNewRequest(oldRequestsCount)

        val urlWithTwiceUpdatedData = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            urlWithTwiceUpdatedData,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
                KEY_EV_INITIAL_CHARGE to newInitialCharge,
                KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption,
                KEY_EV_PRECONDITIONING_TIME to preconditioningTime,
            ),
        )
        checkDoesNotHaveParameters(urlWithTwiceUpdatedData, userProvidedCpoiKeys)

        mapboxNavigation.onEVDataUpdated(emptyMap())
        mapboxNavigation.routeProgressUpdates().first {
            it.currentState == RouteProgressState.TRACKING
        }
        oldRequestsCount = routeHandler.handledRequests.size
        mapboxNavigation.replanRoute()
        waitForNewRequest(oldRequestsCount)

        val urlAfterEmptyUpdate = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            urlAfterEmptyUpdate,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
                KEY_EV_INITIAL_CHARGE to newInitialCharge,
                KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption,
                KEY_EV_PRECONDITIONING_TIME to preconditioningTime,
            ),
        )
        checkDoesNotHaveParameters(urlAfterEmptyUpdate, userProvidedCpoiKeys)
    }

    /**
     * Verifies that user-provided CPOI (Charging Point of Interest) data — station power,
     * current type, and station IDs — is correctly forwarded in reroute requests on a
     * 3-waypoint EV route. Both an initial reroute and a subsequent reroute from a different
     * off-route position must include the full set of CPOI parameters.
     */
    @Test
    fun ev_reroute_parameters_with_user_provided_cpoi_data() = sdkTest {
        val stationPower = ";3000;6500"
        val stationCurrentType = ";dc;dc"
        val stationIds = ";ocm-176357;ocm-190632"
        val threeCoordinates = listOf(
            Point.fromLngLat(11.585226, 48.176099),
            Point.fromLngLat(11.063842, 48.39023),
            Point.fromLngLat(10.32645, 49.069138),
        )

        mockWebServerRule.requestHandlers.clear()
        routeHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(activity, R.raw.ev_route_response_custom_station_data),
            threeCoordinates,
            relaxedExpectedCoordinates = true,
        )
        mockWebServerRule.requestHandlers.add(routeHandler)

        val requestedRoutes = requestRoutes(
            threeCoordinates,
            electric = true,
            stationPower = stationPower,
            stationCurrentType = stationCurrentType,
            stationIds = stationIds,
            waypointsPerRoute = true,
        )

        mapboxNavigation.startTripSession()
        stayOnPosition(threeCoordinates[0].latitude(), threeCoordinates[0].longitude(), 135f)
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        mapboxNavigation.moveAlongTheRouteUntilTracking(
            requestedRoutes.first(),
            mockLocationReplayerRule,
        )
        stayOnPosition(offRouteLocationUpdate.latitude, offRouteLocationUpdate.longitude)
        waitForReroute()

        val url1 = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            url1,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_WAYPOINTS_POWER to stationPower,
                KEY_WAYPOINTS_CURRENT_TYPE to stationCurrentType,
                KEY_WAYPOINTS_STATION_ID to stationIds,
            ),
        )

        val offRouteLocationUpdate2 = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = offRouteLocationUpdate.latitude + 0.002
            longitude = offRouteLocationUpdate.longitude
        }
        stayOnPosition(offRouteLocationUpdate2.latitude, offRouteLocationUpdate2.longitude)

        waitForNewReroute()

        val url2 = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            url2,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_WAYPOINTS_POWER to stationPower,
                KEY_WAYPOINTS_CURRENT_TYPE to stationCurrentType,
                KEY_WAYPOINTS_STATION_ID to stationIds,
            ),
        )
    }

    /**
     * Verifies that when rerouting from the second leg of a multi-leg EV route with
     * user-provided CPOI data, only the CPOI parameters for the remaining charging stations
     * (from the current waypoint onward) are included in the reroute request. Data for
     * already-passed charging stations must be stripped from the request.
     */
    @Test
    fun ev_reroute_parameters_on_second_leg_with_user_provided_cpoi_data() = sdkTest {
        val stationPower = ";3000;6500"
        val stationCurrentType = ";dc;dc"
        val stationIds = ";ocm-176357;ocm-190632"
        val threeCoordinates = listOf(
            Point.fromLngLat(11.585226, 48.176099),
            Point.fromLngLat(11.063842, 48.39023),
            Point.fromLngLat(10.32645, 49.069138),
        )
        val offRouteSecondLegLocation = Point.fromLngLat(
            threeCoordinates[1].longitude(),
            threeCoordinates[1].latitude() + 0.002,
        )

        mockWebServerRule.requestHandlers.clear()
        routeHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(activity, R.raw.ev_route_response_custom_station_data),
            threeCoordinates,
            relaxedExpectedCoordinates = false,
        )
        mockWebServerRule.requestHandlers.add(routeHandler)

        val requestedRoutes = requestRoutes(
            threeCoordinates,
            electric = true,
            stationPower = stationPower,
            stationCurrentType = stationCurrentType,
            stationIds = stationIds,
            waypointsPerRoute = true,
        )

        mapboxNavigation.startTripSession()
        stayOnPosition(threeCoordinates[0].latitude(), threeCoordinates[0].longitude(), 135f)
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)

        mapboxNavigation.routeProgressUpdates().first()
        mapboxNavigation.navigateNextRouteLeg()

        val rerouteHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(
                activity,
                R.raw.ev_reroute_from_second_leg_response_custom_station_data,
            ),
            listOf(offRouteSecondLegLocation, threeCoordinates[1]),
            relaxedExpectedCoordinates = true,
        )
        mockWebServerRule.requestHandlers.add(rerouteHandler)

        stayOnPosition(offRouteSecondLegLocation.latitude(), offRouteSecondLegLocation.longitude())
        waitForReroute()

        checkHasParameters(
            rerouteHandler.handledRequests.last().requestUrl!!,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_WAYPOINTS_POWER to ";6500",
                KEY_WAYPOINTS_CURRENT_TYPE to ";dc",
                KEY_WAYPOINTS_STATION_ID to ";ocm-190632",
            ),
        )
    }

    private suspend fun requestRoutes(
        coordinates: List<Point>,
        electric: Boolean,
        minChargeAtDestination: Int = 6000,
        initialCharge: String = initialInitialCharge,
        stationIds: String? = null,
        stationPower: String? = null,
        stationCurrentType: String? = null,
        waypointsPerRoute: Boolean? = null,
    ): List<NavigationRoute> {
        return mapboxNavigation.requestRoutes(
            generateRouteOptions(
                coordinates,
                electric,
                minChargeAtDestination,
                initialCharge,
                stationIds,
                stationPower,
                stationCurrentType,
                waypointsPerRoute,
            ),
        )
            .getSuccessfulResultOrThrowException()
            .routes
    }

    private fun generateRouteOptions(
        coordinates: List<Point>,
        electric: Boolean,
        minChargeAtDestination: Int,
        initialCharge: String,
        stationIds: String?,
        stationPower: String?,
        stationCurrentType: String?,
        waypointsPerRoute: Boolean?,
    ): RouteOptions {
        return RouteOptions.builder().applyDefaultNavigationOptions()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .alternatives(true)
            .enableRefresh(true)
            .coordinatesList(coordinates)
            .waypointsPerRoute(waypointsPerRoute)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .apply {
                if (electric) {
                    annotations("state_of_charge")
                    unrecognizedProperties(
                        mutableMapOf(
                            KEY_ENGINE to VALUE_ELECTRIC,
                            KEY_EV_INITIAL_CHARGE to initialCharge,
                            KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
                            KEY_EV_PRECONDITIONING_TIME to initialEvPreconditioningTime,
                            KEY_AUXILIARY_CONSUMPTION to initialAuxiliaryConsumption,
                            "ev_min_charge_at_charging_station" to "6000",
                            "ev_min_charge_at_destination" to "$minChargeAtDestination",
                            "ev_max_charge" to "60000",
                            "ev_connector_types" to "ccs_combo_type1,ccs_combo_type2",
                            "ev_charging_curve" to "0,100000;40000,70000;60000,30000;80000,10000",
                            "ev_max_ac_charging_power" to "14400",
                            "ev_unconditioned_charging_curve" to
                                "0,50000;42000,35000;60000,15000;80000,5000",
                        ).also {
                            if (stationIds != null) {
                                it[KEY_WAYPOINTS_STATION_ID] = stationIds
                            }
                            if (stationPower != null) {
                                it[KEY_WAYPOINTS_POWER] = stationPower
                            }
                            if (stationCurrentType != null) {
                                it[KEY_WAYPOINTS_CURRENT_TYPE] = stationCurrentType
                            }
                        },
                    )
                }
            }
            .build()
    }

    private fun stayOnInitialPosition() {
        stayOnPosition(twoCoordinates[0].latitude(), twoCoordinates[0].longitude(), 180f)
    }

    private fun stayOnPosition(
        latitude: Double,
        longitude: Double,
        bearing: Float = 0f,
    ) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                this.latitude = latitude
                this.longitude = longitude
                this.bearing = bearing
                this.speed = 5f
            },
            times = 120,
        )
    }

    private suspend fun waitForReroute() {
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
            .first()
    }

    private suspend fun waitForNewReroute() {
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REROUTE }
            .take(2)
            .toList()
    }

    private fun checkHasParameters(url: HttpUrl, expectedParameters: Map<String, String>) {
        expectedParameters.forEach {
            assertEquals(
                "parameter ${it.key}=${it.value}",
                it.value,
                url.queryParameter(it.key),
            )
        }
    }

    private fun checkDoesNotHaveParameters(url: HttpUrl, forbiddenNames: Set<String>) {
        forbiddenNames.forEach {
            assertFalse("parameter $it", it in url.queryParameterNames)
        }
    }

    private suspend fun waitForNewRequest(oldRequestsCount: Int) {
        while (routeHandler.handledRequests.size == oldRequestsCount) {
            delay(50)
        }
    }
}
