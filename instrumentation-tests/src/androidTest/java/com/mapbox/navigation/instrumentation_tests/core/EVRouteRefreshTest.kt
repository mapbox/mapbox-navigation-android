package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.annotation.IdRes
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.options.RoutingTilesOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteRefreshOptions
import com.mapbox.navigation.core.EVDataObserver
import com.mapbox.navigation.core.EVDataUpdater
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routeProgressUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routesUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAndWaitForUpdate
import com.mapbox.navigation.instrumentation_tests.utils.http.FailByRequestMockRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRefreshHandler
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
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
import java.util.concurrent.CopyOnWriteArraySet
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
    private val evDataUpdater = TestEVDataUpdater()

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

        mapboxNavigation.setEVDataUpdater(evDataUpdater)
        evDataUpdater.updateData(
            mapOf(
                KEY_ENERGY_CONSUMPTION_CURVE to "0,300;20,120;40,150",
                KEY_EV_INITIAL_CHARGE to "80",
                KEY_EV_PRECONDITIONING_TIME to "10",
                KEY_AUXILIARY_CONSUMPTION to "300"
            )
        )
        mapboxNavigation.setNavigationRoutes(requestedRoutes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
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
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)

        mapboxNavigation.setNavigationRoutes(requestedRoutes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        waitUntilRefresh()

        checkDoesNotHaveParameters(
            refreshHandler.handledRequests.first().requestUrl!!,
            evDataKeys
        )
        checkHasParameters(
            refreshHandler.handledRequests.first().requestUrl!!,
            mapOf(KEY_ENGINE to VALUE_ELECTRIC)
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
        mapboxNavigation.setEVDataUpdater(evDataUpdater)
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
            KEY_EV_INITIAL_CHARGE to initialCharge,
            KEY_EV_PRECONDITIONING_TIME to preconditioningTime,
            KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption
        )
        evDataUpdater.updateData(evData)

        mapboxNavigation.setNavigationRoutes(requestedRoutes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
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

        mapboxNavigation.setNavigationRoutes(requestedRoutes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
        waitUntilRefresh()

        val noUpdaterRefreshUrl = refreshHandler.handledRequests.first().requestUrl!!
        checkDoesNotHaveParameters(noUpdaterRefreshUrl, evDataKeys)
        checkHasParameters(noUpdaterRefreshUrl, mapOf(KEY_ENGINE to VALUE_ELECTRIC))

        val consumptionCurve = "0,300;20,120;40,150"
        val initialCharge = "80"
        val preconditioningTime = "10"
        val auxiliaryConsumption = "300"
        val firstEvData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
            KEY_EV_INITIAL_CHARGE to initialCharge,
            KEY_EV_PRECONDITIONING_TIME to preconditioningTime,
            KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption
        )
        mapboxNavigation.setEVDataUpdater(evDataUpdater)
        evDataUpdater.updateData(firstEvData)
        waitUntilNewRefresh()

        checkHasParameters(
            refreshHandler.handledRequests.last().requestUrl!!,
            firstEvData + (KEY_ENGINE to VALUE_ELECTRIC)
        )

        val newInitialCharge = "60"
        evDataUpdater.updateData(
            mapOf(
                KEY_EV_INITIAL_CHARGE to newInitialCharge,
                KEY_EV_PRECONDITIONING_TIME to null,
            )
        )
        waitUntilNewRefresh()

        val urlWithTwiceUpdatedData = refreshHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            urlWithTwiceUpdatedData,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
                KEY_EV_INITIAL_CHARGE to newInitialCharge,
                KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption
            )
        )
        checkDoesNotHaveParameters(urlWithTwiceUpdatedData, setOf(KEY_EV_PRECONDITIONING_TIME))

        mapboxNavigation.setEVDataUpdater(null)
        waitUntilNewRefresh()

        val removedUpdaterRefreshUrl = refreshHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            removedUpdaterRefreshUrl,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
                KEY_EV_INITIAL_CHARGE to newInitialCharge,
                KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption
            )
        )
        checkDoesNotHaveParameters(removedUpdaterRefreshUrl, setOf(KEY_EV_PRECONDITIONING_TIME))

        val newUpdater = TestEVDataUpdater()
        mapboxNavigation.setEVDataUpdater(newUpdater)
        val newUpdaterCharge = "45"
        evDataUpdater.updateData(mapOf(KEY_EV_INITIAL_CHARGE to "50"))
        newUpdater.updateData(mapOf(KEY_EV_INITIAL_CHARGE to newUpdaterCharge))
        waitUntilNewRefresh()

        checkHasParameters(
            refreshHandler.handledRequests.last().requestUrl!!,
            mapOf(KEY_EV_INITIAL_CHARGE to newUpdaterCharge)
        )

        newUpdater.updateData(emptyMap())
        waitUntilNewRefresh()

        val urlAfterEmptyUpdate = refreshHandler.handledRequests.last().requestUrl!!
        checkHasParameters(
            urlAfterEmptyUpdate,
            mapOf(
                KEY_ENGINE to VALUE_ELECTRIC,
                KEY_ENERGY_CONSUMPTION_CURVE to consumptionCurve,
                KEY_EV_INITIAL_CHARGE to newUpdaterCharge,
                KEY_AUXILIARY_CONSUMPTION to auxiliaryConsumption
            )
        )
        checkDoesNotHaveParameters(urlAfterEmptyUpdate, setOf(KEY_EV_PRECONDITIONING_TIME))
    }

    @Test
    fun ev_route_refresh_updates_ev_annotations_and_waypoints_for_the_whole_route() = sdkTest {
        addRefreshRequestHandler(
            R.raw.ev_route_refresh_response,
            acceptedGeometryIndex = 0
        )
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)
        mapboxNavigation.setEVDataUpdater(evDataUpdater)
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to "0,300;20,160;80,140;120,180",
            KEY_EV_INITIAL_CHARGE to "17000",
            KEY_EV_PRECONDITIONING_TIME to "10",
            KEY_AUXILIARY_CONSUMPTION to "300"
        )
        evDataUpdater.updateData(evData)

        mapboxNavigation.setNavigationRoutesAndWaitForUpdate(requestedRoutes)
        mapboxNavigation.startTripSession()
        stayOnInitialPosition()
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
    fun ev_route_refresh_updates_ev_annotations_and_waypoints_for_truncated_current_leg() =
        sdkTest {
            val geometryIndex = 384
            addRefreshRequestHandler(
                R.raw.ev_route_refresh_response_starting_from_384,
                geometryIndex
            )
            val requestedRoutes = requestRoutes(twoCoordinates, electric = true)
            mapboxNavigation.setEVDataUpdater(evDataUpdater)
            val evData = mapOf(
                KEY_ENERGY_CONSUMPTION_CURVE to "0,300;20,160;80,140;120,180",
                KEY_EV_INITIAL_CHARGE to "17000",
                KEY_EV_PRECONDITIONING_TIME to "10",
                KEY_AUXILIARY_CONSUMPTION to "300"
            )
            evDataUpdater.updateData(evData)
            mapboxNavigation.setNavigationRoutes(requestedRoutes)
            // corresponds to currentRouteGeometryIndex = 384
            stayOnPosition(48.209765, 11.478632)
            mapboxNavigation.startTripSession()
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
        mapboxNavigation.setEVDataUpdater(evDataUpdater)
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to "0,300;20,160;80,140;120,180",
            KEY_EV_INITIAL_CHARGE to "17000",
            KEY_EV_PRECONDITIONING_TIME to "10",
            KEY_AUXILIARY_CONSUMPTION to "300"
        )
        evDataUpdater.updateData(evData)
        mapboxNavigation.setNavigationRoutes(requestedRoutes)
        stayOnInitialPosition()
        mapboxNavigation.startTripSession()

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
        val routeGeometryIndex = 1050
        val legGeometryIndex = 300
        addRefreshRequestHandler(
            R.raw.ev_route_refresh_response_for_second_leg,
            acceptedGeometryIndex = routeGeometryIndex
        )
        val requestedRoutes = requestRoutes(twoCoordinates, electric = true)
        mapboxNavigation.setEVDataUpdater(evDataUpdater)
        val evData = mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to "0,300;20,160;80,140;120,180",
            KEY_EV_INITIAL_CHARGE to "17000",
            KEY_EV_PRECONDITIONING_TIME to "10",
            KEY_AUXILIARY_CONSUMPTION to "300"
        )
        evDataUpdater.updateData(evData)
        mapboxNavigation.setNavigationRoutes(requestedRoutes, initialLegIndex = 1)
        // corresponds to currentRouteGeometryIndex = 1050
        stayOnPosition(48.435946, 10.86999)
        mapboxNavigation.startTripSession()
        mapboxNavigation.routeProgressUpdates().filter { progress ->
            progress.currentRouteGeometryIndex == routeGeometryIndex
        }.first()

        val updatedRoutes = waitUntilRefresh().navigationRoutes

        assertEquals(
            listOf(29, 13),
            requestedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd()
        )
        assertEquals(
            listOf(43, 38, 10),
            requestedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd(legGeometryIndex)
        )
        assertEquals(
            listOf(null, 8097, null),
            requestedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
        )

        assertEquals(
            listOf(29, 13),
            updatedRoutes[0].getSocAnnotationsFromLeg(0)!!.firstLastAnd()
        )
        assertEquals(
            listOf(43, 28, 1),
            updatedRoutes[0].getSocAnnotationsFromLeg(1)!!.firstLastAnd(legGeometryIndex)
        )
        assertEquals(
            listOf(null, 8097, null),
            updatedRoutes[0].directionsResponse.waypoints()?.extractChargeAtArrival()
        )
    }

    private fun stayOnInitialPosition() {
        stayOnPosition(twoCoordinates[0].latitude(), twoCoordinates[0].longitude())
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

    private fun generateRouteOptions(coordinates: List<Point>, electric: Boolean): RouteOptions {
        return RouteOptions.builder().applyDefaultNavigationOptions()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .alternatives(true)
            .enableRefresh(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .apply {
                if (electric) {
                    annotations("state_of_charge")
                    unrecognizedProperties(
                        mapOf(
                            KEY_ENGINE to VALUE_ELECTRIC,
                            KEY_EV_INITIAL_CHARGE to "18000",
                            KEY_ENERGY_CONSUMPTION_CURVE to "0,300;20,160;80,140;120,180",
                            KEY_EV_PRECONDITIONING_TIME to "10",
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
        electric: Boolean
    ): List<NavigationRoute> {
        return mapboxNavigation.requestRoutes(generateRouteOptions(coordinates, electric))
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
        acceptedGeometryIndex: Int
    ): MockDirectionsRefreshHandler {
        return MockDirectionsRefreshHandler(
            responseTestUuid,
            readRawFileText(activity, fileId),
            acceptedGeometryIndex = acceptedGeometryIndex
        ).also {
            mockWebServerRule.requestHandlers.add(FailByRequestMockRequestHandler(it))
        }
    }

    private fun getOffRouteLocation(originLocation: Point): Location =
        mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude() + 0.002
            longitude = originLocation.longitude()
        }
}

private class DynamicResponseModifier : (String) -> String {

    var numberOfInvocations = 0

    override fun invoke(p1: String): String {
        numberOfInvocations++
        val originalResponse = DirectionsRefreshResponse.fromJson(p1)
        val newRoute = originalResponse.route()!!
            .toBuilder()
            .legs(
                originalResponse.route()!!.legs()!!.map {
                    it
                        .toBuilder()
                        .annotation(
                            it.annotation()!!
                                .toBuilder()
                                .speed(
                                    it.annotation()!!.speed()!!.map {
                                        it + numberOfInvocations * 0.1
                                    }
                                )
                                .build()
                        )
                        .build()
                }
            )
            .build()
        return DirectionsRefreshResponse.builder()
            .route(newRoute)
            .code(originalResponse.code())
            .message(originalResponse.message())
            .build()
            .toJson()
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class TestEVDataUpdater : EVDataUpdater {

    private val observers = CopyOnWriteArraySet<EVDataObserver>()

    override fun registerEVDataObserver(observer: EVDataObserver) {
        observers.add(observer)
    }

    override fun unregisterEVDataObserver(observer: EVDataObserver) {
        observers.remove(observer)
    }

    fun updateData(data: Map<String, String?>) {
        observers.forEach { it.onEVDataUpdated(data) }
    }
}
