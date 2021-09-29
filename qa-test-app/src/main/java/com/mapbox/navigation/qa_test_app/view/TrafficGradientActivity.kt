package com.mapbox.navigation.qa_test_app.view

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.TrafficGradientActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

class TrafficGradientActivity : AppCompatActivity() {

    private val binding: TrafficGradientActivityLayoutBinding by lazy {
        TrafficGradientActivityLayoutBinding.inflate(layoutInflater)
    }

    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(Color.MAGENTA)
            .routeLowCongestionColor(Color.CYAN)
            .build()
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    private lateinit var options: MapboxRouteLineOptions

    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }

    private lateinit var routeLineApi: MapboxRouteLineApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        options = MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .build()
        routeLineApi = MapboxRouteLineApi(options)
        initGradientSelector()
        initStyle()
    }

    override fun onDestroy() {
        super.onDestroy()
        routeLineApi.cancel()
        routeLineView.cancel()
    }

    private fun initGradientSelector() {
        binding.gradientOptionHard.setOnClickListener {
            options = options.toBuilder(this)
                .displaySoftGradientForTraffic(false)
                .build()
            resetRouteLineApi()
        }

        binding.gradientOptionSoft.setOnClickListener {
            options = options.toBuilder(this)
                .displaySoftGradientForTraffic(true)
                .build()
            resetRouteLineApi()
        }
    }

    private fun resetRouteLineApi() {
        routeLineApi = MapboxRouteLineApi(options)
        binding.mapView.getMapboxMap().getStyle()?.let { style ->
            routeLineApi.setRoutes(listOf(RouteLine(getRoute(), null))) {
                routeLineView.renderRouteDrawData(style, it)
            }
        }
    }

    private fun initStyle() {
        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) { style ->

            val route = getRoute()
            routeLineApi.setRoutes(listOf(RouteLine(route, null))) {
                routeLineView.renderRouteDrawData(style, it)
            }

            val routeOrigin = Utils.getRouteOriginPoint(route)
            val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(14.0).build()
            binding.mapView.getMapboxMap().setCamera(cameraOptions)
        }
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsString = Utils.readRawFileText(this, R.raw.route_with_restrictions)
        return DirectionsRoute.fromJson(routeAsString)
    }
}
