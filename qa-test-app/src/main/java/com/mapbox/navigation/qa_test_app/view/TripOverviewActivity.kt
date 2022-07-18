package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityTripOverviewBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressViewOptions
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView

class TripOverviewActivity : AppCompatActivity() {

    private val mapboxReplayer = MapboxReplayer()
    private val navigationLocationProvider = NavigationLocationProvider()

    private val tripProgressFormatter: TripProgressUpdateFormatter by lazy {
        val distanceFormatterOptions =
            DistanceFormatterOptions.Builder(this).build()
        TripProgressUpdateFormatter.Builder(this)
            .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
            .timeRemainingFormatter(TimeRemainingFormatter(this))
            .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(this))
            .build()
    }

    /**
     * TripProgress: The [MapboxTripProgressApi] consumes route progress data and produces trip related
     * data that is consumed by the [MapboxTripProgressView] in the view layout.
     */
    private val tripProgressApi: MapboxTripProgressApi by lazy {
        MapboxTripProgressApi(tripProgressFormatter)
    }

    private val binding: LayoutActivityTripOverviewBinding by lazy {
        LayoutActivityTripOverviewBinding.inflate(layoutInflater)
    }

    private val mapboxNavigation: MapboxNavigation by lazy {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxSapaAccessToken(this))
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(RouteLineResources.Builder().build())
            .withRouteLineBelowLayerId("road-label-navigation")
            .displayRestrictedRoadSections(true)
            .withVanishingRouteLineEnabled(true)
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(options)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initNavigation()
        initStyle()
    }

    override fun onStop() {
        super.onStop()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
        mapboxNavigation.onDestroy()
    }

    private fun initStyle() {
        binding.mapView.getMapboxMap().loadStyleUri(
            NavigationStyles.NAVIGATION_DAY_STYLE
        ) { style ->

            val route = getRoute()
            routeLineApi.setRoutes(listOf(RouteLine(route, null))) {
                routeLineView.renderRouteDrawData(style, it)
            }
            mapboxNavigation.setRoutes(listOf(route))
            val routeOrigin = Utils.getRouteOriginPoint(route)
            val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(15.0).build()
            binding.mapView.getMapboxMap().setCamera(cameraOptions)
            binding.legOverviewOne.updateOptions(
                TripProgressViewOptions
                    .Builder()
                    .timeRemainingTextAppearance(R.style.MyCustomTimeRemaining)
                    .build()
            )
            binding.legOverviewTwo.updateOptions(
                TripProgressViewOptions
                    .Builder()
                    .backgroundColor(R.color.secondaryVariant)
                    .timeRemainingTextAppearance(R.style.MyCustomTimeRemaining)
                    .build()
            )
            binding.tripOverview.updateOptions(
                TripProgressViewOptions
                    .Builder()
                    .timeRemainingTextAppearance(R.style.MyCustomTimeRemaining)
                    .build()
            )
        }
    }

    private fun initNavigation() {
        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxReplayer.pushRealLocation(this, 0.0)
        mapboxReplayer.playbackSpeed(1.5)
        mapboxReplayer.play()
    }

    private val routesObserver = RoutesObserver {
        val navigationRoute = it.navigationRoutes.first()
        val tripOverview = tripProgressApi.getTripDetails(navigationRoute)
        binding.tripOverview.renderTripOverview(tripOverview)
        binding.legOverviewOne.renderLegOverview(0, tripOverview)
        binding.legOverviewTwo.renderLegOverview(1, tripOverview)
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsString = Utils.readRawFileText(this, R.raw.multileg_route_two_legs)
        return DirectionsRoute.fromJson(routeAsString)
    }
}
