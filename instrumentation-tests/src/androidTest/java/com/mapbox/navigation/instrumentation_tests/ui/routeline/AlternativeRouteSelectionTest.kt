package com.mapbox.navigation.instrumentation_tests.ui.routeline

import android.content.Context
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.MapStyleInitIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.idling.RouteProgressStateIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AlternativeRouteSelectionTest : BaseTest<BasicNavigationViewActivity>(
    BasicNavigationViewActivity::class.java
) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var initIdlingResource: MapStyleInitIdlingResource
    private lateinit var routeProgressIdlingResource: RouteProgressStateIdlingResource

    protected lateinit var directionsResponse: DirectionsResponse

    protected lateinit var mapboxNavigation: MapboxNavigation

    protected lateinit var routeLineApi: MapboxRouteLineApi
    protected lateinit var routeLineView: MapboxRouteLineView

    @Before
    fun setUp() {
        initIdlingResource = MapStyleInitIdlingResource(activity.binding.mapView)
        initIdlingResource.register()
        Espresso.onIdle()

        directionsResponse = getRoute(activity)
        val coordinates = directionsResponse.waypoints()?.mapNotNull { it.location() } ?: listOf()
        val route = directionsResponse.routes()[0]

        mapboxNavigation = MapboxNavigation(
            NavigationOptions.Builder(activity)
                .accessToken(getMapboxAccessTokenFromResources(activity))
                .build()
        )

        routeProgressIdlingResource = RouteProgressStateIdlingResource(
            mapboxNavigation,
            RouteProgressState.LOCATION_TRACKING
        )

        runOnMainSync {
            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = coordinates.first().latitude()
                longitude = coordinates.first().longitude()
            }
            mockLocationReplayerRule.playRoute(route)

            mapboxNavigation.setRoutes(directionsResponse.routes())
        }
    }

    @Test
    fun selectAlternateRoute() {
        addRouteLine()
        routeProgressIdlingResource.register()

        runOnMainSync {
            assertEquals(3, routeLineApi.getRoutes().size)

            val soonToBePrimaryRoute = routeLineApi.getRoutes()[1]
            routeLineApi.updateToPrimaryRoute(soonToBePrimaryRoute)

            assertEquals(soonToBePrimaryRoute, routeLineApi.getRoutes()[0])

            mapboxNavigation.setRoutes(routeLineApi.getRoutes())
            mapboxNavigation.registerRouteProgressObserver(object : RouteProgressObserver {
                override fun onRouteProgressChanged(routeProgress: RouteProgress) {
                    // only need one route progress for this test
                    mapboxNavigation.unregisterRouteProgressObserver(this)

                    assertEquals(routeLineApi.getRoutes()[0], routeProgress.route)
                    routeProgressIdlingResource.unregister()
                }
            })
            mapboxNavigation.startTripSession()
        }

        Espresso.onIdle()
        mapboxNavigation.onDestroy()
    }

    fun addRouteLine() {
        runOnMainSync {
            routeLineView = MapboxRouteLineView(MapboxRouteLineOptions.Builder(activity).build())
            routeLineApi = MapboxRouteLineApi(MapboxRouteLineOptions.Builder(activity).build())

            mapboxNavigation.registerRoutesObserver(object : RoutesObserver {
                override fun onRoutesChanged(routes: List<DirectionsRoute>) {
                    val routeLines = directionsResponse.routes().map {
                        RouteLine(it, null)
                    }
                    routeLineApi.setRoutes(routeLines).apply {
                        routeLineView.renderRouteDrawData(activity.mapboxMap.getStyle()!!, this)
                    }
                    mapboxNavigation.unregisterRoutesObserver(this)
                }
            })
        }
    }

    fun getRoute(context: Context): DirectionsResponse {
        return MockRoutesProvider.loadDirectionsResponse(context, R.raw.multiple_routes)
    }
}
