package com.mapbox.navigation.examples.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.navigation.base.internal.VoiceUnit.IMPERIAL
import com.mapbox.navigation.base.internal.VoiceUnit.METRIC
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.extensions.coordinates
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.internal.MapboxDistanceFormatter
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.BannerInstructionsListener
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.mapbox.navigation.utils.internal.ifNonNull
import java.util.Locale
import kotlinx.android.synthetic.main.activity_navigation_view.*
import timber.log.Timber

/**
 * This activity shows how to use a [com.mapbox.navigation.ui.NavigationView]
 * to implement a basic turn-by-turn navigation experience.
 */
class NavigationViewActivity : AppCompatActivity(), OnNavigationReadyCallback,
    NavigationListener,
    BannerInstructionsListener {

    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var route: DirectionsRoute
    private val originPoint = Point.fromLngLat(-122.396796, 37.7918406)
    private val destinationPoint = Point.fromLngLat(-122.4140199, 37.7909168)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_view)

        navigationView.onCreate(savedInstanceState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigationView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        navigationView.onStart()
    }

    override fun onResume() {
        super.onResume()
        getDirectionsRoute()
        navigationView.onResume()
    }

    override fun onStop() {
        super.onStop()
        navigationView.onStop()
    }

    override fun onPause() {
        super.onPause()
        navigationView.onPause()
    }

    override fun onDestroy() {
        navigationView.onDestroy()
        mapboxNavigation.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigationView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
        if (::route.isInitialized) {
            outState.putString(Utils.PRIMARY_ROUTE_BUNDLE_KEY, route.toJson())
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Utils.getRouteFromBundle(savedInstanceState)?.let {
            route = it
        }
        navigationView.onRestoreInstanceState(savedInstanceState)
    }

    override fun onNavigationReady(isRunning: Boolean) {
        if (!isRunning && !::navigationMapboxMap.isInitialized) {
            ifNonNull(navigationView.retrieveNavigationMapboxMap()) { navMapboxMap ->
                this.navigationMapboxMap = navMapboxMap
                this.navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.NORMAL)

                val optionsBuilder = NavigationViewOptions.builder(this@NavigationViewActivity)
                optionsBuilder.navigationListener(this@NavigationViewActivity)
                optionsBuilder.directionsRoute(route)
                optionsBuilder.shouldSimulateRoute(true)
                optionsBuilder.bannerInstructionsListener(this@NavigationViewActivity)

                val navigationOptions =
                    MapboxNavigation.defaultNavigationOptionsBuilder(
                        applicationContext,
                        Mapbox.getAccessToken()
                    ).distanceFormatter(
                        MapboxDistanceFormatter.Builder()
                            .withLocale(Locale.JAPAN)
                            .withUnitType(IMPERIAL)
                            .build(applicationContext)
                    )
                        .build()
                optionsBuilder.navigationOptions(navigationOptions)

                navigationView.startNavigation(optionsBuilder.build())
            }
        }
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions {
        return instructions!!
    }

    override fun onNavigationRunning() {
        // Not needed in this example
    }

    override fun onNavigationFinished() {
        finish()
    }

    override fun onCancelNavigation() {
        navigationView.stopNavigation()
        finish()
    }

    private fun getInitialCameraPosition(): CameraPosition {
        return CameraPosition.Builder()
            .target(LatLng(originPoint.latitude(), originPoint.longitude()))
            .zoom(15.0)
            .build()
    }

    private fun getDirectionsRoute() {
        this.mapboxNavigation = MapboxNavigation(
            MapboxNavigation.defaultNavigationOptionsBuilder(
                this,
                Utils.getMapboxAccessToken(this)
            )
                .build()
        )

        if (!::route.isInitialized) {
            this.mapboxNavigation.requestRoutes(
                RouteOptions.builder().applyDefaultParams()
                    .accessToken(Utils.getMapboxAccessToken(applicationContext))
                    .coordinates(originPoint, null, destinationPoint)
                    .alternatives(true)
                    .voiceUnits(METRIC)
                    .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                    .build(),
                routesReqCallback
            )
        } else {
            initializeNavigationView()
        }
    }

    private fun initializeNavigationView() {
        navigationView.initialize(
            this,
            getInitialCameraPosition(),
            Utils.getMapboxAccessToken(this)
        )
    }

    // Callbacks and Observers
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            Timber.d("route request success %s", routes.toString())
            if (routes.isNotEmpty()) {
                route = routes[0]
                initializeNavigationView()
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }
}
