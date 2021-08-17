package com.mapbox.navigation.qa_test_app.view

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.RouteTrafficUpdateActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RouteTrafficUpdateActivity : AppCompatActivity() {

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    private val binding: RouteTrafficUpdateActivityLayoutBinding by lazy {
        RouteTrafficUpdateActivityLayoutBinding.inflate(layoutInflater)
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
        initStyle()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        job.cancel()
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
                routeLineApi.setVanishingOffset(.27).apply {
                    routeLineView.renderRouteLineUpdate(style, this)
                }
            }

            val routeOrigin = Utils.getRouteOriginPoint(route)
            val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(14.0).build()
            binding.mapView.getMapboxMap().setCamera(cameraOptions)

            toggleTrafficCongestion()
        }
    }

    private fun getRoute(): DirectionsRoute {
        val routeAsString = Utils.readRawFileText(this, R.raw.route_with_restrictions)
        return DirectionsRoute.fromJson(routeAsString)
    }

    private fun toggleTrafficCongestion() {
        Toast.makeText(
            this,
            "Updating traffic congestion on route line in about 1 second.",
            Toast.LENGTH_LONG
        ).show()
        val routeAsString = Utils.readRawFileText(this, R.raw.route_with_restrictions)
        val congestions = listOf("severe", "heavy", "moderate", "low")
        scope.launch(Dispatchers.Main) {
            while (true) {
                delay(1000)
                val newCongestion = congestions.shuffled().first()
                val updatedRouteJson =
                    routeAsString.replace("unknown", newCongestion)
                        .replace("low", newCongestion)
                val updatedRoute = DirectionsRoute.fromJson(updatedRouteJson)
                routeLineApi.setRoutes(listOf(RouteLine(updatedRoute, null))) {
                    routeLineView.renderRouteDrawData(
                        binding.mapView.getMapboxMap().getStyle()!!,
                        it
                    )
                }
            }
        }
    }
}
