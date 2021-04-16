package com.mapbox.navigation.instrumentation_tests.ui

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.getCameraAnimationsPlugin
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.getLocationComponentPlugin
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.core.trip.session.MapMatcherResultObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.idling.MapStyleInitIdlingResource
import com.mapbox.navigation.instrumentation_tests.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoute
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class SimpleMapViewNavigationTest :
    BaseTest<BasicNavigationViewActivity>(BasicNavigationViewActivity::class.java) {

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    @get:Rule
    val mockLocationReplayerRule = MockLocationReplayerRule(mockLocationUpdatesRule)

    private lateinit var initIdlingResource: MapStyleInitIdlingResource

    protected lateinit var mockRoute: MockRoute

    protected lateinit var mapboxNavigation: MapboxNavigation

    protected lateinit var routeLineApi: MapboxRouteLineApi
    protected lateinit var routeLineView: MapboxRouteLineView

    protected lateinit var mapboxNavigationViewportDataSource: MapboxNavigationViewportDataSource
    protected lateinit var navigationCamera: NavigationCamera

    protected lateinit var navigationLocationProvider: NavigationLocationProvider
    protected lateinit var locationPlugin: LocationComponentPlugin

    @Before
    fun setup() {
        initIdlingResource = MapStyleInitIdlingResource(activity.binding.mapView)
        initIdlingResource.register()
        Espresso.onIdle()

        mockRoute = getRoute(activity)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

        val route = mockRoute.routeResponse.routes()[0]

        runOnMainSync {
            mockLocationUpdatesRule.pushLocationUpdate {
                latitude = mockRoute.routeWaypoints.first().latitude()
                longitude = mockRoute.routeWaypoints.first().longitude()
            }
            mockLocationReplayerRule.playRoute(route)

            mapboxNavigation = MapboxNavigation(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .build()
            )
            mapboxNavigation.setRoutes(listOf(route))
            mapboxNavigation.startTripSession()
        }
    }

    protected fun addRouteLine() {
        runOnMainSync {
            routeLineView = MapboxRouteLineView(MapboxRouteLineOptions.Builder(activity).build())
            routeLineApi = MapboxRouteLineApi(MapboxRouteLineOptions.Builder(activity).build())

            mapboxNavigation.registerRoutesObserver(object : RoutesObserver {
                override fun onRoutesChanged(routes: List<DirectionsRoute>) {
                    routeLineApi.setRoutes(
                        listOf(RouteLine(mockRoute.routeResponse.routes()[0], null)),
                        object : MapboxNavigationConsumer<Expected<RouteSetValue, RouteLineError>> {
                            override fun accept(result: Expected<RouteSetValue, RouteLineError>) {
                                routeLineView.renderRouteDrawData(
                                    activity.mapboxMap.getStyle()!!,
                                    result
                                )
                            }
                        }
                    )
                }
            })
        }
    }

    protected fun addNavigationCamera() {
        runOnMainSync {
            mapboxNavigationViewportDataSource = MapboxNavigationViewportDataSource(
                activity.mapboxMap
            )
            navigationCamera = NavigationCamera(
                activity.mapboxMap,
                activity.binding.mapView.getCameraAnimationsPlugin(),
                mapboxNavigationViewportDataSource
            )
            navigationCamera.requestNavigationCameraToFollowing()

            mapboxNavigation.registerRoutesObserver(object : RoutesObserver {
                override fun onRoutesChanged(routes: List<DirectionsRoute>) {
                    mapboxNavigationViewportDataSource.onRouteChanged(routes.first())
                    mapboxNavigationViewportDataSource.evaluate()
                }
            })

            mapboxNavigation.registerRouteProgressObserver(object : RouteProgressObserver {
                override fun onRouteProgressChanged(routeProgress: RouteProgress) {
                    mapboxNavigationViewportDataSource.onRouteProgressChanged(routeProgress)
                    mapboxNavigationViewportDataSource.evaluate()
                }
            })

            mapboxNavigation.registerMapMatcherResultObserver(object : MapMatcherResultObserver {
                override fun onNewMapMatcherResult(mapMatcherResult: MapMatcherResult) {
                    mapboxNavigationViewportDataSource.onLocationChanged(
                        mapMatcherResult.enhancedLocation
                    )
                    mapboxNavigationViewportDataSource.evaluate()
                }
            })
        }
    }

    protected fun addLocationPuck() {
        runOnMainSync {
            navigationLocationProvider = NavigationLocationProvider()
            locationPlugin = activity.binding.mapView.getLocationComponentPlugin()
            locationPlugin.setLocationProvider(navigationLocationProvider)
            locationPlugin.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    activity,
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            locationPlugin.enabled = true
            mapboxNavigation.registerMapMatcherResultObserver(object : MapMatcherResultObserver {
                override fun onNewMapMatcherResult(mapMatcherResult: MapMatcherResult) {
                    navigationLocationProvider.changePosition(
                        mapMatcherResult.enhancedLocation,
                        mapMatcherResult.keyPoints
                    )
                }
            })
        }
    }

    @After
    fun tearDown() {
        initIdlingResource.unregister()
    }

    open fun getRoute(context: Context): MockRoute {
        return MockRoutesProvider.dc_very_short(context)
    }
}
