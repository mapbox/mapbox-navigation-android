package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.instrumentation_tests.utils.history.MapboxHistoryTestRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.ArrivalIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SanityCoreRouteTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    @get:Rule
    val mapboxHistoryTestRule = MapboxHistoryTestRule()

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var routeCompleteIdlingResource: RouteProgressStateIdlingResource

    override fun setupMockLocation(): Location {
        val mockRoute = RoutesProvider.dc_very_short(context)
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }
    }

    @Before
    fun setup() {
        Espresso.onIdle()

        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .build()
            )
            mapboxHistoryTestRule.historyRecorder = mapboxNavigation.historyRecorder
        }
        routeCompleteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.COMPLETE
        )
    }

    @Test
    fun route_completes() {
        // prepare
        val mockRoute = RoutesProvider.dc_very_short(activity)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        routeCompleteIdlingResource.register()

        val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
            requiredState(RouteProgressState.TRACKING)
            requiredState(RouteProgressState.COMPLETE)
        }

        // execute
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
        }
        runOnMainSync {
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(activity)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
                object : RouterCallback {
                    override fun onRoutesReady(
                        routes: List<DirectionsRoute>,
                        routerOrigin: RouterOrigin
                    ) {
                        mapboxNavigation.setRoutes(routes)
                        mockLocationReplayerRule.playRoute(routes[0])
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions
                    ) {
                        // no impl
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        routerOrigin: RouterOrigin
                    ) {
                        // no impl
                    }
                }
            )
        }

        // assert and clean up
        mapboxHistoryTestRule.stopRecordingOnCrash("no route complete") {
            Espresso.onIdle()
        }
        expectedStates.assert()
        routeCompleteIdlingResource.unregister()
    }

    @Test
    fun route_with_two_legs_completes() {
        // prepare
        val mockRoute = RoutesProvider.dc_very_short_two_legs(activity)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        val arrivalIdlingResource = ArrivalIdlingResource(mapboxNavigation)
        arrivalIdlingResource.register()

        val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
            requiredState(RouteProgressState.TRACKING)
            requiredState(RouteProgressState.COMPLETE)
            requiredState(RouteProgressState.TRACKING)
            requiredState(RouteProgressState.COMPLETE)
        }

        // execute
        runOnMainSync {
            mapboxNavigation.historyRecorder.startRecording()
        }
        runOnMainSync {
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(activity)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
                object : RouterCallback {
                    override fun onRoutesReady(
                        routes: List<DirectionsRoute>,
                        routerOrigin: RouterOrigin
                    ) {
                        mapboxNavigation.setRoutes(routes)
                        mockLocationReplayerRule.playRoute(routes.first())
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions
                    ) {
                        // no impl
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        routerOrigin: RouterOrigin
                    ) {
                        // no impl
                    }
                }
            )
        }

        // assert and clean up
        mapboxHistoryTestRule.stopRecordingOnCrash("no route complete") {
            Espresso.onIdle()
        }
        expectedStates.assert()
        arrivalIdlingResource.unregister()
    }
}
