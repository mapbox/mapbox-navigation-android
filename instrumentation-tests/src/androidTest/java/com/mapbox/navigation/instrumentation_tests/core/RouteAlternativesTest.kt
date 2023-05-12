package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouteAlternativesOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.FirstLocationIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.IdlingPolicyTimeoutRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteAlternativesIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteRequestIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * This test ensures that alternative route recommendations
 * are given during active guidance.
 */
class RouteAlternativesTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    @get:Rule
    val idlingPolicyRule = IdlingPolicyTimeoutRule(1, TimeUnit.MINUTES)

    private lateinit var mapboxNavigation: MapboxNavigation
    private val coordinates = listOf(
        Point.fromLngLat(-122.2750659, 37.8052036),
        Point.fromLngLat(-122.2647245, 37.8138895)
    )

    private companion object {
        private const val LOG_CATEGORY = "RouteAlternativesTest"
    }

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = coordinates[0].latitude()
        longitude = coordinates[0].longitude()
    }

    @Before
    fun setup() {
        runOnMainSync {
            val routeAlternativesOptions = RouteAlternativesOptions.Builder()
                .intervalMillis(TimeUnit.SECONDS.toMillis(30))
                .build()
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .routeAlternativesOptions(routeAlternativesOptions)
                    .build()
            )
            mapboxHistoryTestRule.historyRecorder = mapboxNavigation.historyRecorder
        }
    }

    @Test
    fun expect_initial_alternative_route_removed_after_navigating_past() {
        // Prepare with alternative routes.
        setupMockRequestHandlers(coordinates)
        val routes = requestDirectionsRouteSync(coordinates)

        // Play primary route
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
            mockLocationReplayerRule.playRoute(routes.first())
            mapboxNavigation.startTripSession()
        }

        // Wait for enhanced locations to start and then set the routes.
        val firstLocationIdlingResource = FirstLocationIdlingResource(mapboxNavigation)
        firstLocationIdlingResource.firstLocationSync()
        runOnMainSync {
            mapboxNavigation.setRoutes(routes)
        }

        // The alternative route is missed, so we expect the route to be removed.
        val firstAlternative = RouteAlternativesIdlingResource(mapboxNavigation)
        firstAlternative.register()
        mapboxHistoryTestRule.stopRecordingOnCrash("alternatives failed") {
            Espresso.onIdle()
        }
        firstAlternative.unregister()
        assertTrue(firstAlternative.calledOnMainThread)

        runBlocking(Dispatchers.Main) {
            val historyPath = mapboxNavigation.historyRecorder.stopRecording()
            logE("history path=$historyPath", LOG_CATEGORY)
        }

        // Verify alternative routes events were triggered.
        assertEquals(2, routes.size)
        assertTrue(mapboxNavigation.getNavigationRoutes().isNotEmpty())
        firstAlternative.verifyOnRouteAlternativesAndProgressReceived()
        firstAlternative.alternatives!!.forEach {
            assertFalse(routes.contains(it))
        }
    }

    @Test
    fun alternative_routes_observer_is_not_triggered_for_set_routes() {
        // Prepare with alternative routes.
        setupMockRequestHandlers(coordinates)
        val routes = requestDirectionsRouteSync(coordinates)

        // Play primary route
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
            mockLocationReplayerRule.playRoute(routes.first())
            mapboxNavigation.startTripSession()
        }

        // Wait for enhanced locations to start and then set the routes.
        val firstLocationIdlingResource = FirstLocationIdlingResource(mapboxNavigation)
        firstLocationIdlingResource.firstLocationSync()

        // Subscribing for alternatives
        val firstAlternative = RouteAlternativesIdlingResource(
            mapboxNavigation
        ) { _, alternatives, _ ->
            alternatives.isNotEmpty()
        }
        firstAlternative.register()

        runOnMainSync {
            mapboxNavigation.setRoutes(routes)
        }

        mapboxHistoryTestRule.stopRecordingOnCrash("alternatives failed") {
            Espresso.onIdle()
        }
        firstAlternative.unregister()

        assertTrue(firstAlternative.calledOnMainThread)

        runBlocking(Dispatchers.Main) {
            val historyPath = mapboxNavigation.historyRecorder.stopRecording()
            logE("history path=$historyPath", LOG_CATEGORY)
        }

        // Verify alternative routes events were triggered.
        assertEquals(2, routes.size)
        assertTrue(mapboxNavigation.getRoutes().isNotEmpty())
        firstAlternative.verifyOnRouteAlternativesAndProgressReceived()

        assertNotEquals(
            "Verify setRoutes and alternatives are not the same routes",
            routes.drop(1).sortedBy { it.hashCode() }, // drop primary route
            firstAlternative.alternatives!!.sortedBy { it.hashCode() }
        )
    }

    /**
     * The test verifies that if set routes to Navigation that are come from alternatives + n
     * (where n >= 1) external routes, alternatives routes observer is not triggered with these routes.
     * That was happening in legacy versions of NN.
     */
    @Test
    fun additional_alternative_is_not_force_to_invoke_alternatives_observer() {
        // Prepare with alternative routes.
        setupMockRequestHandlers(coordinates)
        val routes = requestDirectionsRouteSync(coordinates)

        // Play primary route
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
            mockLocationReplayerRule.playRoute(routes.first())
            mapboxNavigation.startTripSession()
        }

        // Wait for enhanced locations to start and then set the routes.
        val firstLocationIdlingResource = FirstLocationIdlingResource(mapboxNavigation)
        firstLocationIdlingResource.firstLocationSync()
        runOnMainSync {
            // infinity subscription to avoid triggering NN on every new observer
            mapboxNavigation.registerRouteAlternativesObserver(
                object : NavigationRouteAlternativesObserver {
                    override fun onRouteAlternatives(
                        routeProgress: RouteProgress,
                        alternatives: List<NavigationRoute>,
                        routerOrigin: RouterOrigin
                    ) = Unit

                    override fun onRouteAlternativesError(error: RouteAlternativesError) = Unit
                }
            )
            mapboxNavigation.setRoutes(routes)
        }

        // Subscribing for alternatives
        val firstAlternative = RouteAlternativesIdlingResource(
            mapboxNavigation
        ) { _, alternatives, _ ->
            alternatives.isNotEmpty()
        }
        firstAlternative.register()
        mapboxHistoryTestRule.stopRecordingOnCrash("alternatives failed") {
            Espresso.onIdle()
        }
        firstAlternative.unregister()

        assertNotNull(firstAlternative.alternatives)

        val nextAlternativeObserver = RouteAlternativesIdlingResource(
            mapboxNavigation
        ) { _, alternatives, _ ->
            alternatives.isNotEmpty()
        }
        nextAlternativeObserver.register()

        val externalAlternatives = DirectionsResponse.fromJson(
            readRawFileText(activity, R.raw.route_response_alternative_continue)
        ).routes().also {
            assertEquals(1, it.size)
        }

        lateinit var setRoutes: List<DirectionsRoute>
        runOnMainSync {
            setRoutes = (
                mutableListOf(
                    mapboxNavigation.getRoutes().first()
                ) + firstAlternative.alternatives!! + externalAlternatives
                ).also {
                assertTrue(
                    "Primary route + >=1 alternatives + external alternatives",
                    it.size >= 3
                )
            }
            mapboxNavigation.setRoutes(setRoutes)
        }

        mapboxHistoryTestRule.stopRecordingOnCrash("next alternatives failed") {
            Espresso.onIdle()
        }
        nextAlternativeObserver.unregister()

        runBlocking(Dispatchers.Main) {
            mapboxNavigation.historyRecorder.stopRecording {
                logE("history path=$it", LOG_CATEGORY)
            }
        }

        // Verify alternative routes events were triggered.
        firstAlternative.verifyOnRouteAlternativesAndProgressReceived()

        // Verify next alternative routes events were triggered
        nextAlternativeObserver.verifyOnRouteAlternativesAndProgressReceived()

        // verify that set routes with  alternatives + 1 external alternatives is not triggered
        // alternative observer
        assertNotEquals(
            "Alternative are not the same as setRoutes (alternatives might have " +
                "additional routes or remove one or a few but not equal)",
            setRoutes.drop(1).sortedBy { it.hashCode() }, // drop primary route
            nextAlternativeObserver.alternatives!!.sortedBy { it.hashCode() }
        )
    }

    private fun setupMockRequestHandlers(coordinates: List<Point>) {
        // Nav-native requests alternate routes, so we are only
        // ensuring the initial route has alternatives.
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                "driving-traffic",
                readRawFileText(activity, R.raw.route_response_alternative_start),
                coordinates
            )
        )
    }

    private fun requestDirectionsRouteSync(coordinates: List<Point>): List<DirectionsRoute> {
        val routeOptions = RouteOptions.builder().applyDefaultNavigationOptions()
            .alternatives(true)
            .coordinatesList(coordinates)
            .baseUrl(mockWebServerRule.baseUrl) // Comment out to test a real server
            .build()
        val routeRequestIdlingResource = RouteRequestIdlingResource(mapboxNavigation, routeOptions)
        return routeRequestIdlingResource.requestRoutesSync()
    }
}
