package com.mapbox.navigation.instrumentation_tests.core

import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.assertions.RouteProgressStateTransitionAssertion
import com.mapbox.navigation.instrumentation_tests.utils.http.MockDirectionsRequestHandler
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// TODO: Refactor https://github.com/mapbox/mapbox-navigation-android/issues/4429
class CoreRerouteTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var mapboxNavigation: MapboxNavigation

    private lateinit var offRouteIdlingResource: RouteProgressStateIdlingResource
    private lateinit var locationTrackingIdlingResource: RouteProgressStateIdlingResource

    @Before
    fun setup() {
        Espresso.onIdle()

        mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .build()
        )
        locationTrackingIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.TRACKING
        )
        offRouteIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.OFF_ROUTE
        )
    }

    @Test
    fun reroute_completes() {
        // prepare
        val mockRoute = MockRoutesProvider.dc_very_short(activity)

        val originLocation = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = mockRoute.routeWaypoints.first().latitude()
            longitude = mockRoute.routeWaypoints.first().longitude()
        }

        val offRouteLocationUpdate = mockLocationUpdatesRule.generateLocationUpdate {
            latitude = originLocation.latitude + 0.002
            longitude = originLocation.longitude
        }

        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)
        mockWebServerRule.requestHandlers.add(
            MockDirectionsRequestHandler(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
                jsonResponse = readRawFileText(activity, R.raw.reroute_response_dc_very_short),
                expectedCoordinates = listOf(
                    Point.fromLngLat(
                        offRouteLocationUpdate.longitude,
                        offRouteLocationUpdate.latitude
                    ),
                    mockRoute.routeWaypoints.last()
                )
            )
        )
        locationTrackingIdlingResource.register()

        val expectedStates = RouteProgressStateTransitionAssertion(mapboxNavigation) {
            requiredState(RouteProgressState.TRACKING)
            requiredState(RouteProgressState.OFF_ROUTE)
            optionalState(RouteProgressState.INITIALIZED)
            requiredState(RouteProgressState.TRACKING)
        }

        // start a route
        runOnMainSync {
            mockLocationUpdatesRule.pushLocationUpdate(originLocation)
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(activity)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .coordinates(mockRoute.routeWaypoints).build(),
                object : RoutesRequestCallback {
                    override fun onRoutesReady(routes: List<DirectionsRoute>) {
                        mapboxNavigation.setRoutes(routes)
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

        // wait for tracking to start
        Espresso.onIdle()
        locationTrackingIdlingResource.unregister()

        // push off route location and wait for the off route event
        offRouteIdlingResource.register()
        runOnMainSync {
            mockLocationReplayerRule.loopUpdate(offRouteLocationUpdate, times = 5)
        }
        Espresso.onIdle()
        offRouteIdlingResource.unregister()

        // wait for tracking to start again
        locationTrackingIdlingResource.register()
        Espresso.onIdle()
        locationTrackingIdlingResource.unregister()

        // assert results
        expectedStates.assert()
    }
}
