package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.annotation.IdRes
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.DynamicResponseModifier
import com.mapbox.navigation.instrumentation_tests.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.toApproximateCoordinates
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.testing.ui.utils.coroutines.requestRoutes
import com.mapbox.navigation.testing.ui.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.routesUpdates
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import com.mapbox.navigation.testing.ui.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
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
import java.util.concurrent.TimeUnit

private const val KEY_ENGINE = "engine"
private const val KEY_ENERGY_CONSUMPTION_CURVE = "energy_consumption_curve"
private const val KEY_EV_INITIAL_CHARGE = "ev_initial_charge"
private const val KEY_AUXILIARY_CONSUMPTION = "auxiliary_consumption"
private const val KEY_EV_PRECONDITIONING_TIME = "ev_pre_conditioning_time"
private const val VALUE_ELECTRIC = "electric"
private val evDataKeys = setOf(
    KEY_EV_INITIAL_CHARGE,
    KEY_ENERGY_CONSUMPTION_CURVE,
    KEY_EV_PRECONDITIONING_TIME,
    KEY_AUXILIARY_CONSUMPTION
)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class EVRouteRefreshTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation
    private val responseTestUuid = "ev_route_response_for_refresh"
    private val twoCoordinates = listOf(
        Point.fromLngLat(11.5852259, 48.1760993),
        Point.fromLngLat(10.3406374, 49.16479)
    )
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
            val routeRefreshOptions = RouteRefreshOptions.Builder()
                .intervalMillis(TimeUnit.SECONDS.toMillis(30))
                .build()
            RouteRefreshOptions::class.java.getDeclaredField("intervalMillis").apply {
                isAccessible = true
                set(routeRefreshOptions, 1_500L)
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
    fun ev_route_refresh_parameters_for_non_ev_route() = sdkTest {
        val refreshHandler = addRefreshRequestHandler(
            R.raw.ev_route_refresh_response,
            acceptedGeometryIndex = 0
        )
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
        mapboxNavigation.setNavigationRoutes(requestedRoutes)
        waitUntilRefresh()

        checkDoesNotHaveParameters(
            refreshHandler.handledRequests.first().requestUrl!!,
            evDataKeys + KEY_ENGINE
        )
    }

    @Test
    fun ev_route_refresh_parameters_for_ev_route_with_no_ev_data() = sdkTest {
        val refreshHandler = addRefreshRequestHandler(
            R.raw.ev_route_refresh_response,
            acceptedGeometryIndex = 0
        )
        refreshHandler.jsonResponseModifier = DynamicResponseModifier()
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)

        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutes(requestedRoutes)
        waitUntilRefresh()

        checkHasParameters(
            refreshHandler.handledRequests.first().requestUrl!!,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
                KEY_EV_INITIAL_CHARGE to initialInitialCharge,
                KEY_AUXILIARY_CONSUMPTION to initialAuxiliaryConsumption,
                KEY_EV_PRECONDITIONING_TIME to initialEvPreconditioningTime,
            )
        )

        val newInitialCharge = "17900"
        val newRequestedRoutes = requestRoutes(
            twoCoordinates,
            electric = true,
            initialCharge = newInitialCharge
        )
        mapboxNavigation.setNavigationRoutes(newRequestedRoutes)
        waitUntilNewRefresh()
        checkHasParameters(
            refreshHandler.handledRequests.last().requestUrl!!,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
                KEY_EV_INITIAL_CHARGE to newInitialCharge,
                KEY_AUXILIARY_CONSUMPTION to initialAuxiliaryConsumption,
                KEY_EV_PRECONDITIONING_TIME to initialEvPreconditioningTime,
            )
        )
    }

    @Test
    fun ev_route_refresh_parameters_for_ev_route_with_ev_data() = sdkTest {
        val refreshHandler = addRefreshRequestHandler(
            R.raw.ev_route_refresh_response,
            acceptedGeometryIndex = 0
        )
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
        mapboxNavigation.setNavigationRoutes(requestedRoutes)
        waitUntilRefresh()

        checkHasParameters(
            refreshHandler.handledRequests.first().requestUrl!!,
            evData + (KEY_ENGINE to VALUE_ELECTRIC)
        )
    }

    @Test
    fun ev_route_refresh_parameter_for_ev_route_with_ev_data_updates() = sdkTest {
        val refreshHandler = addRefreshRequestHandler(
            R.raw.ev_route_refresh_response,
            acceptedGeometryIndex = 0
        )
        refreshHandler.jsonResponseModifier = DynamicResponseModifier()
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)

        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutes(requestedRoutes)
        waitUntilRefresh()

        val noDataRefreshUrl = refreshHandler.handledRequests.first().requestUrl!!
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
        waitUntilNewRefresh()

        checkHasParameters(
            refreshHandler.handledRequests.last().requestUrl!!,
            firstEvData + (KEY_ENGINE to VALUE_ELECTRIC)
        )

        val newInitialCharge = "60"
        mapboxNavigation.onEVDataUpdated(
            mapOf(KEY_EV_INITIAL_CHARGE to newInitialCharge)
        )
        waitUntilNewRefresh()

        val urlWithTwiceUpdatedData = refreshHandler.handledRequests.last().requestUrl!!
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
        waitUntilNewRefresh()

        val urlAfterEmptyUpdate = refreshHandler.handledRequests.last().requestUrl!!
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
    fun ev_route_refresh_updates_ev_annotations_and_waypoints_for_the_whole_route() = sdkTest {
        addRefreshRequestHandler(
            R.raw.ev_route_refresh_response,
            acceptedGeometryIndex = 0
        )
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
            KEY_EV_INITIAL_CHARGE to "17000",
            KEY_EV_PRECONDITIONING_TIME to "10",
            KEY_AUXILIARY_CONSUMPTION to "300"
        )
        mapboxNavigation.onEVDataUpdated(evData)

        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        val updatedRoutes = waitUntilRefresh().navigationRoutes

        assertEquals(
            listOf(29, 13),
            requestedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd()
        )
        assertEquals(
            listOf(43, 10),
            requestedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd()
        )
        assertEquals(
            listOf(null, 8097, null),
            requestedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
        )

        assertEquals(
            listOf(28, 12),
            updatedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd()
        )
        assertEquals(
            listOf(42, 10),
            updatedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd()
        )
        assertEquals(
            listOf(null, 7286, null),
            updatedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
        )
    }

    @Test
    fun ev_route_refresh_updates_waypoints_per_route() = sdkTest {
        replaceOriginalResponseHandler(R.raw.ev_route_response_for_refresh_with_waypoints_per_route)
        addRefreshRequestHandler(
            R.raw.ev_route_refresh_response,
            acceptedGeometryIndex = 0,
            testUuid = "ev_route_response_for_refresh_with_waypoints_per_route"
        )
        val requestedRoutes = requestRoutes(
            twoCoordinates,
            electric = true,
            waypointsPerRoute = true
        )
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to "0,300;20,160;80,140;120,180",
            KEY_EV_INITIAL_CHARGE to "17000",
            KEY_EV_PRECONDITIONING_TIME to "10",
            KEY_AUXILIARY_CONSUMPTION to "300"
        )
        mapboxNavigation.onEVDataUpdated(evData)

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        val updatedRoutes = waitUntilRefresh().navigationRoutes

        assertEquals(
            listOf(null, 8097, null),
            requestedRoutes[0].waypoints?.extractChargeAtArrival()
        )

        assertEquals(
            listOf(null, 7286, null),
            updatedRoutes[0].waypoints?.extractChargeAtArrival()
        )
        assertEquals(updatedRoutes[0].directionsRoute.waypoints(), updatedRoutes[0].waypoints)
        val tolerance = 0.00001
        assertEquals(
            updatedRoutes[0].internalWaypoints().map {
                it.name to it.location.toApproximateCoordinates(tolerance)
            },
            updatedRoutes[0].waypoints?.map {
                it.name() to it.location().toApproximateCoordinates(tolerance)
            }
        )
    }

    @Test
    fun ev_route_refresh_updates_ev_annotations_and_waypoints_for_truncated_current_leg() =
        sdkTest {
            val geometryIndex = 384
            addRefreshRequestHandler(
                R.raw.ev_route_refresh_response_starting_from_384,
                geometryIndex
            )
            val requestedRoutes = requestRoutes(twoCoordinates, electric = true)
            val evData = mapOf(
                KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
                KEY_EV_INITIAL_CHARGE to "17000",
                KEY_EV_PRECONDITIONING_TIME to "10",
                KEY_AUXILIARY_CONSUMPTION to "300"
            )
            mapboxNavigation.onEVDataUpdated(evData)
            mapboxNavigation.startTripSession()
            // corresponds to currentRouteGeometryIndex = 384
            stayOnPosition(48.209765, 11.478632)
            mapboxNavigation.setNavigationRoutes(requestedRoutes)
            mapboxNavigation.routeProgressUpdates().filter { progress ->
                progress.currentRouteGeometryIndex == geometryIndex
            }.first()

            val updatedRoutes = waitUntilRefresh().navigationRoutes

            assertEquals(
                listOf(29, 24, 13),
                requestedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd(geometryIndex)
            )
            assertEquals(
                listOf(43, 10),
                requestedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd()
            )
            assertEquals(
                listOf(null, 8097, null),
                requestedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
            )

            assertEquals(
                listOf(29, 28, 13),
                updatedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd(geometryIndex)
            )
            assertEquals(
                listOf(43, 10),
                updatedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd()
            )
            assertEquals(
                listOf(null, 10188, null),
                updatedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
            )
        }

    @Test
    fun ev_route_refresh_updates_ev_annotations_and_waypoints_for_truncated_next_leg() = sdkTest {
        addRefreshRequestHandler(
            R.raw.ev_route_refresh_response_with_truncated_next_leg,
            acceptedGeometryIndex = 0
        )
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
            KEY_EV_INITIAL_CHARGE to "17000",
            KEY_EV_PRECONDITIONING_TIME to "10",
            KEY_AUXILIARY_CONSUMPTION to "300"
        )
        mapboxNavigation.onEVDataUpdated(evData)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        mapboxNavigation.setNavigationRoutes(requestedRoutes)

        val updatedRoutes = waitUntilRefresh().navigationRoutes

        assertEquals(
            listOf(29, 13),
            requestedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd()
        )
        assertEquals(
            listOf(43, 10),
            requestedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd()
        )
        assertEquals(
            listOf(null, 8097, null),
            requestedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
        )

        assertEquals(
            listOf(28, 12),
            updatedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd()
        )
        assertEquals(
            listOf(42, 10),
            updatedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd()
        )
        assertEquals(
            listOf(null, 7286, null),
            updatedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
        )
    }

    @Test
    fun ev_route_refresh_updates_ev_annotations_and_waypoints_for_second_leg() = sdkTest {
        val routeGeometryIndex = 774
        val legGeometryIndex = 26
        replaceOriginalResponseHandler(R.raw.ev_route_response_for_refresh_with_2_waypoints)
        addRefreshRequestHandler(
            R.raw.ev_route_refresh_response_for_second_leg,
            acceptedGeometryIndex = routeGeometryIndex,
            testUuid = "ev_route_response_for_refresh_with_2_waypoints"
        )
        val requestedRoutes = requestRoutes(
            twoCoordinates,
            electric = true,
            minChargeAtDestination = 35000
        )
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to initialEnergyConsumptionCurve,
            KEY_EV_INITIAL_CHARGE to "30000",
            KEY_EV_PRECONDITIONING_TIME to "10",
            KEY_AUXILIARY_CONSUMPTION to "300"
        )
        mapboxNavigation.onEVDataUpdated(evData)
        mapboxNavigation.startTripSession()
        // corresponds to currentRouteGeometryIndex = 774
        val geometryIndexLocation = Point.fromLngLat(11.064252, 48.391238)
        stayOnPosition(
            geometryIndexLocation.latitude(),
            geometryIndexLocation.longitude(),
            90f
        )
        mapboxNavigation.setNavigationRoutes(requestedRoutes, initialLegIndex = 1)

        mapboxNavigation
            .routeProgressUpdates()
            .filter { progress ->
                progress.currentRouteGeometryIndex == routeGeometryIndex
            }.first()

        val updatedRoutes = waitUntilRefresh().navigationRoutes

        assertEquals(
            listOf(29, 13),
            requestedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd()
        )
        assertEquals(
            listOf(39, 39, 10),
            requestedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd(legGeometryIndex)
        )
        assertEquals(
            listOf(null, 7911, 6000, null),
            requestedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
        )

        assertEquals(
            listOf(29, 13),
            updatedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd()
        )
        assertEquals(
            listOf(39, 49, 21),
            updatedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd(legGeometryIndex)
        )
        assertEquals(
            listOf(null, 7911, 12845, null),
            updatedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
        )
    }

    private fun stayOnInitialPosition() {
        stayOnPosition(twoCoordinates[0].latitude(), twoCoordinates[0].longitude())
    }

    private fun stayOnPosition(
        latitude: Double,
        longitude: Double,
        bearing: Float = 190f
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

    private fun generateRouteOptions(
        coordinates: List<Point>,
        electric: Boolean,
        minChargeAtDestination: Int,
        initialCharge: String,
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
                        mapOf(
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
                        )
                    )
                }
            }
            .build()
    }

    private suspend fun waitUntilRefresh(): RoutesUpdatedResult {
        return mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()
    }

    private suspend fun waitUntilNewRefresh(): RoutesUpdatedResult {
        return mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .take(2)
            .toList()
            .last()
    }

    private suspend fun requestRoutes(
        coordinates: List<Point>,
        electric: Boolean,
        minChargeAtDestination: Int = 6000,
        initialCharge: String = initialInitialCharge,
        waypointsPerRoute: Boolean? = null,
    ): List<NavigationRoute> {
        return mapboxNavigation.requestRoutes(
            generateRouteOptions(
                coordinates,
                electric,
                minChargeAtDestination,
                initialCharge,
                waypointsPerRoute
            )
        )
            .getSuccessfulResultOrThrowException()
            .routes
    }

    private fun checkHasParameters(url: HttpUrl, expectedParameters: Map<String, String>) {
        expectedParameters.forEach {
            assertEquals("parameter ${it.key}=${it.value}", it.value, url.queryParameter(it.key))
        }
    }

    private fun checkDoesNotHaveParameters(url: HttpUrl, forbiddenNames: Set<String>) {
        forbiddenNames.forEach {
            assertFalse("parameter $it", it in url.queryParameterNames)
        }
    }

    private fun <T> List<T>.firstLastAnd(vararg otherIndices: Int): List<T> =
        mutableListOf(first()).also { result ->
            otherIndices.forEach { index -> result.add(get(index)) }
            result.add(last())
        }

    private fun NavigationRoute.getSocAnnotationsFromLeg(legIndex: Int): List<Int>? =
        directionsRoute.legs()?.get(legIndex)
            ?.annotation()
            ?.unrecognizedJsonProperties
            ?.get("state_of_charge")
            ?.asJsonArray
            ?.map { it.asInt }

    private fun List<DirectionsWaypoint>.extractChargeAtArrival(): List<Int?> =
        map {
            it.unrecognizedJsonProperties
                ?.get("metadata")
                ?.asJsonObject
                ?.get("charge_at_arrival")?.asInt
        }

    private fun addRefreshRequestHandler(
        @IdRes fileId: Int,
        acceptedGeometryIndex: Int,
        testUuid: String = responseTestUuid,
    ): MockDirectionsRefreshHandler {
        return MockDirectionsRefreshHandler(
            testUuid,
            readRawFileText(activity, fileId),
            acceptedGeometryIndex = acceptedGeometryIndex,
        ).also {
            mockWebServerRule.requestHandlers.add(FailByRequestMockRequestHandler(it))
        }
    }

    private fun replaceOriginalResponseHandler(@IdRes fileId: Int) {
        val routeHandler = MockDirectionsRequestHandler(
            "driving-traffic",
            readRawFileText(activity, fileId),
            twoCoordinates,
            relaxedExpectedCoordinates = true
        )
        mockWebServerRule.requestHandlers.remove(this.routeHandler)
        mockWebServerRule.requestHandlers.add(0, routeHandler)
    }
}
