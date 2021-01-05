package com.mapbox.navigation.instrumentation_tests.core

import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.routesRequestCallback
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.Utils
import com.mapbox.navigation.instrumentation_tests.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
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

        val options = MapboxNavigation.defaultNavigationOptionsBuilder(
            activity,
            Utils.getMapboxAccessToken(activity)!!
        ).build()
        mapboxNavigation = MapboxNavigationProvider.create(options)
        routeCompleteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.ROUTE_COMPLETE
        )
    }

    @Test
    fun route_completes() {
        // prepare
        val mockRoute = MockRoutesProvider.dc_very_short(activity)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        routeCompleteIdlingResource.register()

        val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
            optionalState(RouteProgressState.ROUTE_INVALID)
            requiredState(RouteProgressState.LOCATION_TRACKING)
            requiredState(RouteProgressState.ROUTE_COMPLETE)
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
                RouteOptions.builder().applyDefaultParams()
                    .baseUrl(mockWebServerRule.baseUrl)
                    .accessToken(Utils.getMapboxAccessToken(activity)!!)
                    .coordinates(mockRoute.routeWaypoints).build(),
                routesRequestCallback(
                    onRoutesReady = { mockLocationReplayerRule.playRoute(it[0]) }
                )
            )
        }

        // assert and clean up
        Espresso.onIdle()
        expectedStates.assert()
        routeCompleteIdlingResource.unregister()
    }
}
