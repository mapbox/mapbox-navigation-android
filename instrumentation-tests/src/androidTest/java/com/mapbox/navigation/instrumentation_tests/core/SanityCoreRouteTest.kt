package com.mapbox.navigation.instrumentation_tests.core

import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SanityCoreRouteTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation

    private lateinit var routeCompleteIdlingResource: RouteProgressStateIdlingResource

    @Before
    fun setup() {
        Espresso.onIdle()

        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .build()
        )
        routeCompleteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.COMPLETE
        )
    }

    @Test
    fun route_completes() {
        // prepare
        val mockRoute = MockRoutesProvider.dc_very_short(activity)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        routeCompleteIdlingResource.register()

        val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
            requiredState(RouteProgressState.TRACKING)
            requiredState(RouteProgressState.COMPLETE)
        }

        // execute
        runOnMainSync {
            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = mockRoute.routeWaypoints.first().latitude()
                longitude = mockRoute.routeWaypoints.first().longitude()
            }
        }
        runOnMainSync {
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(activity)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .coordinatesList(mockRoute.routeWaypoints).build(),
                object : RoutesRequestCallback {
                    override fun onRoutesReady(routes: List<DirectionsRoute>) {
                        mapboxNavigation.setRoutes(routes)
                        mockLocationReplayerRule.playRoute(routes[0])
                    }

                    override fun onRoutesRequestFailure(
                        throwable: Throwable,
                        routeOptions: RouteOptions
                    ) {
                        // no impl
                    }

                    override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                        // no impl
                    }
                }
            )
        }

        // assert and clean up
        Espresso.onIdle()
        expectedStates.assert()
        routeCompleteIdlingResource.unregister()
    }
}
