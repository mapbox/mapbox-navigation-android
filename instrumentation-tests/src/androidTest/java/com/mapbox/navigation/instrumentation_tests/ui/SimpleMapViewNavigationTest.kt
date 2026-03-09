package com.mapbox.navigation.instrumentation_tests.ui

import android.content.Context
import androidx.test.espresso.Espresso
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.Location
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.BasicNavigationViewActivity
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.MapboxNavigationRule
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.testing.utils.idling.MapStyleInitIdlingResource
import com.mapbox.navigation.testing.utils.location.MockLocationReplayerRule
import com.mapbox.navigation.testing.utils.routes.MockRoute
import com.mapbox.navigation.testing.utils.routes.RoutesProvider
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
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

    override fun setupMockLocation(): android.location.Location =
        mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 27.5558128
            longitude = 53.8949337
        }

    @Before
    fun setup() {
        initIdlingResource = MapStyleInitIdlingResource(activity.binding.mapView)
        initIdlingResource.register()
        Espresso.onIdle()

        mockRoute = getRoute(activity)
        mockWebServerRule.requestHandlers.addAll(mockRoute.mockRequestHandlers)

        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .build(),
            )
            mapboxNavigation.startTripSession()
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .applyLanguageAndVoiceUnitOptions(activity)
                    .baseUrl(mockWebServerRule.baseUrl)
                    .coordinatesList(mockRoute.routeWaypoints).build(),
                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        @RouterOrigin routerOrigin: String,
                    ) {
                        mapboxNavigation.setNavigationRoutes(routes)
                        mockLocationReplayerRule.playRoute(routes[0].directionsRoute)
                    }

                    override fun onFailure(
                        reasons: List<RouterFailure>,
                        routeOptions: RouteOptions,
                    ) {
                        // no impl
                    }

                    override fun onCanceled(
                        routeOptions: RouteOptions,
                        @RouterOrigin routerOrigin: String,
                    ) {
                        // no impl
                    }
                },
            )
        }
    }

    protected fun addRouteLine() {
        runOnMainSync {
            val apiOptions = MapboxRouteLineApiOptions.Builder().build()
            val viewOptions = MapboxRouteLineViewOptions.Builder(activity)
                .routeLineBelowLayerId(LocationComponentConstants.LOCATION_INDICATOR_LAYER)
                .build()
            routeLineView = MapboxRouteLineView(viewOptions)
            routeLineApi = MapboxRouteLineApi(apiOptions)

            mapboxNavigation.registerRoutesObserver {
                routeLineApi.setNavigationRoutes(
                    it.navigationRoutes,
                ) { result ->
                    routeLineView.renderRouteDrawData(
                        activity.mapboxMap.getStyle()!!,
                        result,
                    )
                }
            }
        }
    }

    protected fun addNavigationCamera() {
        runOnMainSync {
            mapboxNavigationViewportDataSource = MapboxNavigationViewportDataSource(
                activity.mapboxMap,
            )
            navigationCamera = NavigationCamera(
                activity.mapboxMap,
                activity.binding.mapView.camera,
                mapboxNavigationViewportDataSource,
            )
            navigationCamera.requestNavigationCameraToFollowing()

            mapboxNavigation.registerRoutesObserver { result ->
                mapboxNavigationViewportDataSource.onRouteChanged(result.navigationRoutes.first())
                mapboxNavigationViewportDataSource.evaluate()
            }

            mapboxNavigation.registerRouteProgressObserver { routeProgress ->
                mapboxNavigationViewportDataSource.onRouteProgressChanged(routeProgress)
                mapboxNavigationViewportDataSource.evaluate()
            }

            mapboxNavigation.registerLocationObserver(
                object : LocationObserver {

                    override fun onNewRawLocation(rawLocation: Location) {}

                    override fun onNewLocationMatcherResult(
                        locationMatcherResult: LocationMatcherResult,
                    ) {
                        mapboxNavigationViewportDataSource.onLocationChanged(
                            locationMatcherResult.enhancedLocation,
                        )
                        mapboxNavigationViewportDataSource.evaluate()
                    }
                },
            )
        }
    }

    protected fun addLocationPuck() {
        runOnMainSync {
            navigationLocationProvider = NavigationLocationProvider()
            locationPlugin = activity.binding.mapView.location
            locationPlugin.setLocationProvider(navigationLocationProvider)
            locationPlugin.locationPuck = LocationPuck2D(
                bearingImage = ImageHolder.Companion.from(
                    R.drawable.mapbox_navigation_puck_icon,
                ),
            )
            locationPlugin.enabled = true
            mapboxNavigation.registerLocationObserver(
                object : LocationObserver {

                    override fun onNewRawLocation(rawLocation: Location) {}

                    override fun onNewLocationMatcherResult(
                        locationMatcherResult: LocationMatcherResult,
                    ) {
                        navigationLocationProvider.changePosition(
                            locationMatcherResult.enhancedLocation,
                            locationMatcherResult.keyPoints,
                        )
                    }
                },
            )
        }
    }

    @After
    fun tearDown() {
        initIdlingResource.unregister()
    }

    open fun getRoute(context: Context): MockRoute {
        return RoutesProvider.dc_very_short(context)
    }
}
