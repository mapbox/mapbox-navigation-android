package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.navigateNextRouteLeg
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import okhttp3.HttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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
private val evDataKeys = setOf(
    KEY_EV_INITIAL_CHARGE,
    KEY_ENERGY_CONSUMPTION_CURVE,
    KEY_EV_PRECONDITIONING_TIME,
    KEY_AUXILIARY_CONSUMPTION
)
private val userProvidedCpoiKeys = setOf(
    KEY_WAYPOINTS_POWER,
    KEY_WAYPOINTS_CURRENT_TYPE,
    KEY_WAYPOINTS_STATION_ID
)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class EVRerouteTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation
    private val twoCoordinates = listOf(
        Point.fromLngLat(11.5852259, 48.1760993),
        Point.fromLngLat(10.3406374, 49.16479)
    )
    private val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = twoCoordinates[0].latitude() + 0.002
        longitude = twoCoordinates[0].longitude()
    }
    private lateinit var routeHandler: MockDirectionsRequestHandler
    private val initialEnergyConsumptionCurve = "0,300;20,160;80,140;120,180"
    private val initialInitialCharge = "18000"
    private val initialAuxiliaryConsumption = "300"
    private val initialEvPreconditioningTime = "10"
    private val expectedStickyChargingStationsFromTheTestRoute = mapOf(
        KEY_WAYPOINTS_STATION_ID to ";ocm-176357;",
        KEY_WAYPOINTS_CURRENT_TYPE to ";dc;",
        KEY_WAYPOINTS_POWER to ";300000;"
    )

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
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .routingTilesOptions(
                        RoutingTilesOptions.Builder()
                            .tilesBaseUri(URI(mockWebServerRule.baseUrl))
                            .build()
                    )
                    .navigatorPredictionMillis(0L)
                    .build()
            )
            mockWebServerRule.requestHandlers.clear()
            routeHandler = MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.ev_route_response_for_refresh),
                twoCoordinates,
                relaxedExpectedCoordinates = true
            )
            mockWebServerRule.requestHandlers.add(routeHandler)
        }
    }

    @Test
    fun ev_reroute_parameters_for_non_ev_route() = sdkTest {
        val requestedRoutes = requestRoutes(twoCoordinates, electric = false)

        mapboxNavigation.onEVDataUpdated(
            mapOf(
                KEY_ENERGY_CONSUMPTION_CURVE to "0,300;20,120;40,150",
                KEY_EV_INITIAL_CHARGE to "80",
                KEY_EV_PRECONDITIONING_TIME to "10",
                KEY_AUXILIARY_CONSUMPTION to "300"
            )
        )
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        stayOnPosition(offRouteLocationUpdate.latitude, offRouteLocationUpdate.longitude)
        waitForReroute()

        checkDoesNotHaveParameters(
            routeHandler.handledRequests.last().requestUrl!!,
            evDataKeys + userProvidedCpoiKeys + KEY_ENGINE
        )
    }

    @Test
    fun ev_reroute_parameters_for_ev_route_with_no_ev_data() = sdkTest {
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)

        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
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
            ) + expectedStickyChargingStationsFromTheTestRoute
        )

        val newInitialCharge = "17900"
        val newRequestedRoutes = requestRoutes(
            twoCoordinates,
            electric = true,
            initialCharge = newInitialCharge
        )
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(newRequestedRoutes)
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
                KEY_WAYPOINTS_STATION_ID to ";ocm-176357;",
                KEY_WAYPOINTS_CURRENT_TYPE to ";dc;",
                KEY_WAYPOINTS_POWER to ";300000;"
            ) + expectedStickyChargingStationsFromTheTestRoute
        )
    }

    @Test
    fun ev_reroute_parameters_for_ev_route_with_ev_data() = sdkTest {
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)

        val consumptionCurve = "0,300;20,120;40,150"
        val initialCharge = "80"
        val preconditioningTime = "10"
        val auxiliaryConsumption = "300"
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
            KEY_EV_INITIAL_CHARGE to initialCharge,
            KEY_EV_PRECONDITIONING_TIME to preconditioningTime,
            KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption
        )
        mapboxNavigation.onEVDataUpdated(evData)

        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        stayOnPosition(offRouteLocationUpdate.latitude, offRouteLocationUpdate.longitude)
        waitForReroute()

        val url = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            url,
            evData + (KEY_ENGINE to VALUE_ELECTRIC) +
                expectedStickyChargingStationsFromTheTestRoute
        )
    }

    @Test
    fun ev_reroute_parameters_for_ev_route_with_ev_data_updates() = sdkTest {
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)

        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        stayOnPosition(offRouteLocationUpdate.latitude, offRouteLocationUpdate.longitude)
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
            )
        )

        val consumptionCurve = "0,301;20,121;40,151"
        val initialCharge = "80"
        val preconditioningTime = "11"
        val auxiliaryConsumption = "299"
        val firstEvData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
            KEY_EV_INITIAL_CHARGE to initialCharge,
            KEY_EV_PRECONDITIONING_TIME to preconditioningTime,
            KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption
        )
        mapboxNavigation.onEVDataUpdated(firstEvData)
        var oldRequestsCount = routeHandler.handledRequests.size
        stayOnInitialPosition()
        waitForNewRequest(oldRequestsCount)

        val firstUrl = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            firstUrl,
            firstEvData + (KEY_ENGINE to VALUE_ELECTRIC)
        )

        val newInitialCharge = "60"
        mapboxNavigation.onEVDataUpdated(
            mapOf(KEY_EV_INITIAL_CHARGE to newInitialCharge)
        )
        oldRequestsCount = routeHandler.handledRequests.size
        stayOnPosition(offRouteLocationUpdate.latitude, offRouteLocationUpdate.longitude)
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
            )
        )

        mapboxNavigation.onEVDataUpdated(emptyMap())
        oldRequestsCount = routeHandler.handledRequests.size
        stayOnInitialPosition()
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
            )
        )
    }

    @Test
    fun ev_reroute_parameters_with_user_provided_cpoi_data() = sdkTest {
        val userProvidedChargingStationsPower = ";3000;6500"
        val userProvidedChargingStationsCurrentTypes = ";dc;dc"
        val userProvidedChargingStationsIds = ";ocm-176357;ocm-190632"
        // The SDK transforms server provided charging stations to user provided for stickiness
        val expectedStationsPowerAfterReroute = ";3000;300000;6500"
        val expectedChargingStationsCurrentTypesAfterReroute = ";dc;dc;dc"
        val expectedChargingStationsIdsAfterReroute = ";ocm-176357;ocm-176357;ocm-190632"
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
            relaxedExpectedCoordinates = true
        )
        mockWebServerRule.requestHandlers.add(routeHandler)

        val requestedRoutes = requestRoutes(
            threeCoordinates,
            electric = true,
            stationPower = userProvidedChargingStationsPower,
            stationCurrentType = userProvidedChargingStationsCurrentTypes,
            stationIds = userProvidedChargingStationsIds,
            waypointsPerRoute = true,
        )

        mapboxNavigation.startTripSession()
        stayOnPosition(threeCoordinates[0].latitude(), threeCoordinates[0].longitude(), 135f)
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)

        mockWebServerRule.requestHandlers.remove(routeHandler)
        routeHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(activity, R.raw.ev_route_response_custom_station_data_reroute),
            threeCoordinates,
            relaxedExpectedCoordinates = true
        )
        mockWebServerRule.requestHandlers.add(routeHandler)
        stayOnPosition(offRouteLocationUpdate.latitude, offRouteLocationUpdate.longitude)
        waitForReroute()

        val url1 = routeHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            url1,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_WAYPOINTS_POWER to expectedStationsPowerAfterReroute,
                KEY_WAYPOINTS_CURRENT_TYPE to expectedChargingStationsCurrentTypesAfterReroute,
                KEY_WAYPOINTS_STATION_ID to expectedChargingStationsIdsAfterReroute,
            )
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
                KEY_WAYPOINTS_POWER to expectedStationsPowerAfterReroute,
                KEY_WAYPOINTS_CURRENT_TYPE to expectedChargingStationsCurrentTypesAfterReroute,
                KEY_WAYPOINTS_STATION_ID to expectedChargingStationsIdsAfterReroute,
            )
        )
    }

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
        val offRouteSecondLegLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = threeCoordinates[1].latitude() + 0.002
            longitude = threeCoordinates[1].longitude()
        }

        mockWebServerRule.requestHandlers.clear()
        routeHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(activity, R.raw.ev_route_response_custom_station_data),
            threeCoordinates,
            relaxedExpectedCoordinates = false
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
                R.raw.ev_reroute_from_second_leg_response_custom_station_data
            ),
            listOf(offRouteSecondLegLocation.toPoint(), threeCoordinates[1]),
            relaxedExpectedCoordinates = true
        )
        mockWebServerRule.requestHandlers.add(rerouteHandler)

        stayOnPosition(offRouteSecondLegLocation.latitude, offRouteSecondLegLocation.longitude)
        waitForReroute()

        checkHasParameters(
            rerouteHandler.handledRequests.last().requestUrl!!,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_WAYPOINTS_POWER to ";6500",
                KEY_WAYPOINTS_CURRENT_TYPE to ";dc",
                KEY_WAYPOINTS_STATION_ID to ";ocm-190632"
            )
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
            )
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
                        }
                    )
                }
            }
            .build()
    }

    private fun stayOnInitialPosition() {
        stayOnPosition(twoCoordinates[0].latitude(), twoCoordinates[0].longitude())
    }

    private fun stayOnPosition(
        latitude: Double,
        longitude: Double,
        bearing: Float = 0f
    ) {
        mockLocationReplayerRule.loopUpdate(
            mockLocationUpdatesRule.generateLocationUpdate {
                this.latitude = latitude
                this.longitude = longitude
                this.bearing = bearing
            },
            times = 120
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
                url.queryParameter(it.key)
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
