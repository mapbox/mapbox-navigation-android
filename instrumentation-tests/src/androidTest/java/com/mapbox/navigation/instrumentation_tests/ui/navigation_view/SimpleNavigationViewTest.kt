package com.mapbox.navigation.instrumentation_tests.ui.navigation_view

import androidx.test.espresso.Espresso
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.NavigationViewInitIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoute
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.ui.NavigationViewOptions
import kotlinx.android.synthetic.main.activity_basic_navigation_view.*
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class SimpleNavigationViewTest :
    BaseTest<BasicNavigationViewActivity>(BasicNavigationViewActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var initIdlingResource: NavigationViewInitIdlingResource

    protected lateinit var mockRoute: MockRoute

    protected lateinit var mapboxNavigation: MapboxNavigation

    @Before
    fun setup() {
        initIdlingResource = NavigationViewInitIdlingResource(
            activity.navigationView,
            activity.findViewById(R.id.navigationMapView)
        )
        initIdlingResource.register()
        Espresso.onIdle()

        mockRoute = MockRoutesProvider.dc_very_short(activity)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

        val route = mockRoute.routeResponse.routes()[0]

        runOnMainSync {
            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = mockRoute.routeWaypoints.first().latitude()
                longitude = mockRoute.routeWaypoints.first().longitude()
            }
            mockLocationReplayerRule.playRoute(route)
            activity.navigationView.startNavigation(
                NavigationViewOptions.builder(activity)
                    .directionsRoute(route)
                    .voiceInstructionLoaderBaseUrl(mockWebServerRule.baseUrl)
                    .build()
            )
            mapboxNavigation = activity.navigationView.retrieveMapboxNavigation()!!
        }
    }

    @After
    fun tearDown() {
        initIdlingResource.unregister()
    }
}
