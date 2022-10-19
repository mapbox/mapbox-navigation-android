package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
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
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.getSuccessfulResultOrThrowException
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.requestRoutes
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.routesUpdates
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
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
    private val responseTestUuid = "route_response_route_refresh"
    private val twoCoordinates = listOf(
        Point.fromLngLat(-121.496066, 38.577764),
        Point.fromLngLat(-121.480279, 38.57674)
    )
    private lateinit var refreshHandler: MockDirectionsRefreshHandler
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
            mockWebServerRule.requestHandlers.add(
                MockDirectionsRequestHandler(
                    "driving-traffic",
                    readRawFileText(activity, R.raw.route_response_route_refresh),
                    twoCoordinates
                )
            )
            refreshHandler = MockDirectionsRefreshHandler(
                responseTestUuid,
                readRawFileText(activity, R.raw.route_response_route_refresh_annotations),
            )
            mockWebServerRule.requestHandlers.add(FailByRequestMockRequestHandler(refreshHandler))
        }
    }

    @Test
    fun evRouteRefreshParametersForNonEVRoute() = sdkTest {
        val requestedRoutes = requestRoutes(twoCoordinates, engine = null)

        mapboxNavigation.setEVDataUpdater(evDataUpdater)
        evDataUpdater.updateData(mapOf(
            KEY_ENERGY_CONSUMPTION_CURVE to "0,300;20,120;40,150",
            KEY_EV_INITIAL_CHARGE to "80",
            KEY_EV_PRECONDITIONING_TIME to "10",
            KEY_AUXILIARY_CONSUMPTION to "300"
        ))
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
    fun evRouteRefreshParametersForEVRouteWithEVNoData() = sdkTest {
        val requestedRoutes = requestRoutes(twoCoordinates, engine = VALUE_ELECTRIC)

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
    fun evRouteRefreshParametersForEVRouteWithEVData() = sdkTest {
        val requestedRoutes = requestRoutes(twoCoordinates, engine = VALUE_ELECTRIC)

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
    fun evRouteRefreshParametersForEVRouteWithEVDataUpdates() = sdkTest {
        refreshHandler.jsonResponseModifier = DynamicResponseModifier()
        val requestedRoutes = requestRoutes(twoCoordinates, engine = VALUE_ELECTRIC)

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

    private fun generateRouteOptions(coordinates: List<Point>, engine: String?): RouteOptions {
        return RouteOptions.builder().applyDefaultNavigationOptions()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .alternatives(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .apply {
                if (engine != null) {
                    unrecognizedProperties(mapOf(KEY_ENGINE to engine))
                }
            }
            .build()
    }

    private suspend fun waitUntilRefresh() {
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .first()
    }

    private suspend fun waitUntilNewRefresh() {
        mapboxNavigation.routesUpdates()
            .filter { it.reason == RoutesExtra.ROUTES_UPDATE_REASON_REFRESH }
            .take(2)
            .toList()
    }

    private suspend fun requestRoutes(coordinates: List<Point>, engine: String?): List<NavigationRoute> {
        return mapboxNavigation.requestRoutes(generateRouteOptions(coordinates, engine))
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
}

private class DynamicResponseModifier: (String) -> String {

    var numberOfInvocations = 0

    override fun invoke(p1: String): String {
        numberOfInvocations++
        val originalResponse = DirectionsRefreshResponse.fromJson(p1)
        val newRoute = originalResponse.route()!!
            .toBuilder()
            .legs(originalResponse.route()!!.legs()!!.map {
                it
                    .toBuilder()
                    .annotation(it.annotation()!!
                        .toBuilder()
                        .speed(it.annotation()!!.speed()!!.map { it + numberOfInvocations * 0.1 })
                        .build()
                    )
                    .build()
            })
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
