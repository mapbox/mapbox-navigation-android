package com.mapbox.navigation.qa_test_app.view.componentinstaller

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style.Companion.MAPBOX_STREETS
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.requireMapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityRestAreaBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.qa_test_app.utils.fetchRoute
import com.mapbox.navigation.qa_test_app.view.componentinstaller.components.MapMarkersLegSteps
import com.mapbox.navigation.qa_test_app.view.componentinstaller.components.MapMarkersRestStops
import com.mapbox.navigation.qa_test_app.view.componentinstaller.components.RestAreaGuideMap
import com.mapbox.navigation.qa_test_app.view.componentinstaller.components.SimpleFollowingCamera
import com.mapbox.navigation.ui.base.installer.installComponents
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.guidance.restarea.api.MapboxRestAreaApi
import com.mapbox.navigation.ui.maps.locationPuck
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.routeArrow
import com.mapbox.navigation.ui.maps.routeLine
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@SuppressLint("MissingPermission")
class RestAreaActivity : AppCompatActivity() {

    private lateinit var binding: LayoutActivityRestAreaBinding

    private val mapboxMap: MapboxMap get() = binding.mapView.getMapboxMap()

    private val mapboxReplayer = MapboxReplayer()
    private val replayProgressObserver = ReplayProgressObserver(mapboxReplayer)

    private var started: Boolean = false

    private val mapboxNavigation by requireMapboxNavigation(
        onCreatedObserver = object : UIComponent() {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                super.onAttached(mapboxNavigation)

                mapboxNavigation.startTripSession()
                mapboxReplayer.pushRealLocation(this@RestAreaActivity, 0.0)
                mapboxReplayer.play()

                mapboxNavigation.flowRoutesUpdated().observe { result ->
                    if (result.navigationRoutes.isNotEmpty()) {
                        startSimulation(result.navigationRoutes[0])
                    } else {
                        stopSimulation()
                    }
                }

                mapboxNavigation.flowRouteProgress().observe { routeProgress ->
                    replayProgressObserver.onRouteProgressChanged(routeProgress)
                }
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                super.onDetached(mapboxNavigation)

                mapboxReplayer.finish()
            }
        }
    ) {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(getMapboxRouteAccessToken(this))
                .locationEngine(ReplayLocationEngine(mapboxReplayer))
                .build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityRestAreaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initStyle()
        initNavigation()
        initControls()
    }

    private fun initStyle() {
        mapboxMap.loadStyleUri(MAPBOX_STREETS)
    }

    private fun initNavigation() {
        MapboxNavigationApp.installComponents(this) {
            locationPuck(binding.mapView)
            routeArrow(binding.mapView)
            routeLine(binding.mapView) {
                options = MapboxRouteLineOptions.Builder(this@RestAreaActivity)
                    .withRouteLineResources(RouteLineResources.Builder().build())
                    .withRouteLineBelowLayerId("road-label")
                    .build()
            }
            component(SimpleFollowingCamera(binding.mapView))
            component(
                RestAreaGuideMap(
                    MapboxRestAreaApi(getMapboxRouteAccessToken(this@RestAreaActivity)),
                    binding.restAreaView
                )
            )
            component(MapMarkersRestStops(binding.mapView))
            component(MapMarkersLegSteps(binding.mapView))
        }
    }

    private fun initControls() {
        binding.startButton.setOnClickListener {
            if (!started) {
                TestCoordinates.of(binding.spinnerRoutes.selectedItem as? String)?.also {
                    fetchAndSetRoute(it.coordinates)
                    started = true
                }
            } else {
                mapboxNavigation.setNavigationRoutes(emptyList())
                started = false
            }
            binding.startButton.text = if (started) "STOP" else "START"
            binding.spinnerRoutes.isEnabled = !started
        }
    }

    private fun startSimulation(navigationRoutes: NavigationRoute) {
        stopSimulation()
        val replayEvents = ReplayRouteMapper()
            .mapDirectionsRouteGeometry(navigationRoutes.directionsRoute)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents.first())
        mapboxReplayer.play()
    }

    private fun stopSimulation() {
        mapboxReplayer.stop()
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushRealLocation(this, 0.0)
    }

    private fun fetchAndSetRoute(coordinates: Pair<Point, Point>) {
        val (origin, destination) = coordinates
        lifecycleScope.launch {
            val routes = mapboxNavigation.fetchRoute(origin, destination)
            mapboxNavigation.setNavigationRoutes(routes)
        }
    }

    private fun getMapboxRouteAccessToken(context: Context): String {
        val tokenResId = context.resources
            .getIdentifier("mapbox_access_token_sapa", "string", context.packageName)
        return if (tokenResId != 0) {
            context.getString(tokenResId)
        } else {
            Toast.makeText(this, "Missing mapbox_access_token_sapa", Toast.LENGTH_LONG).show()
            Utils.getMapboxAccessToken(this)
        }
    }

    private enum class TestCoordinates(
        val coordinates: Pair<Point, Point>
    ) {
        SAPA_TOKYO_LONG(
            Point.fromLngLat(139.65973759944154, 35.69781247239969) to
                Point.fromLngLat(139.75685114382554, 35.672241648222666)
        ),

        // same as SAPA_TOKYO_LONG but closer to the REST_STOP
        SAPA_TOKYO_SHORT(
            Point.fromLngLat(139.690511, 35.695178) to
                Point.fromLngLat(139.75685114382554, 35.672241648222666)
        );

        companion object {
            fun of(value: String?): TestCoordinates? = value?.let {
                runCatching { valueOf(value) }.getOrNull()
            }
        }
    }
}
