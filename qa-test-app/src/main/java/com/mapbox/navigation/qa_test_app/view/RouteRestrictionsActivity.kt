package com.mapbox.navigation.qa_test_app.view

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.RouteRestrictionsActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.qa_test_app.utils.Utils.getRouteOriginPoint
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

class RouteRestrictionsActivity : AppCompatActivity() {

    private val binding: RouteRestrictionsActivityLayoutBinding by lazy {
        RouteRestrictionsActivityLayoutBinding.inflate(layoutInflater)
    }

    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder().restrictedRoadColor(Color.MAGENTA).build()
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    private val options: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label")
            .displayRestrictedRoadSections(true)
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
        initStyle()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    private fun initStyle() {
        binding.mapView.getMapboxMap().loadStyleUri(
            Style.MAPBOX_STREETS
        ) { style ->

            val route = getRoute()
            routeLineApi.setRoutes(listOf(RouteLine(route, null))) {
                routeLineView.renderRouteDrawData(style, it)
            }

            val routeOrigin = getRouteOriginPoint(route)
            val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(14.0).build()
            binding.mapView.getMapboxMap().setCamera(cameraOptions)
        }
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsString = Utils.readRawFileText(this, R.raw.route_with_restrictions)
        return DirectionsRoute.fromJson(routeAsString)
    }
}
