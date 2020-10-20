package com.mapbox.navigation.examples.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.utils.extensions.ifNonNull
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.navigation_view_fragment_layout.*

class NavigationViewFragment : Fragment(), OnNavigationReadyCallback, NavigationListener {

    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private val route by lazy { getDirectionsRoute() }

    companion object {
        fun newInstance(): NavigationViewFragment {
            return NavigationViewFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.navigation_view_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationView.onCreate(savedInstanceState)
        activity?.let {
            navigationView.initialize(
                this,
                getInitialCameraPosition()
            )
        }
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

    override fun onDestroyView() {
        navigationView.onDestroy()
        super.onDestroyView()
    }

    fun navigationViewBackPressed(): Boolean = navigationView.onBackPressed()

    override fun onSaveInstanceState(outState: Bundle) {
        navigationView.onSaveInstanceState(outState)
        outState.putBoolean("myKey", true)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            navigationView.onRestoreInstanceState(savedInstanceState)
        }
    }

    override fun onNavigationReady(isRunning: Boolean) {
        if (!isRunning && !::navigationMapboxMap.isInitialized) {
            ifNonNull(navigationView.retrieveNavigationMapboxMap()) { navMapboxMap ->
                this.navigationMapboxMap = navMapboxMap
                this.navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.NORMAL)
                navigationView.retrieveMapboxNavigation()?.let { this.mapboxNavigation = it }
                val optionsBuilder = NavigationViewOptions.builder(requireContext())
                optionsBuilder.navigationListener(this)
                optionsBuilder.directionsRoute(route)
                optionsBuilder.shouldSimulateRoute(true)
                optionsBuilder.enableVanishingRouteLine(true)
                optionsBuilder.muteVoiceGuidance(true)
                navigationView.startNavigation(optionsBuilder.build())
            }
        }
    }

    override fun onNavigationRunning() {
        // todo
    }

    override fun onNavigationFinished() {
        (this.activity as NavigationViewFragmentActivity).finish()
    }

    override fun onCancelNavigation() {
        navigationView.stopNavigation()
        (this.activity as NavigationViewFragmentActivity).finish()
    }

    private fun getInitialCameraPosition(): CameraPosition {
        val originCoordinate = route.routeOptions()?.coordinates()?.get(0)
        return CameraPosition.Builder()
            .target(LatLng(originCoordinate!!.latitude(), originCoordinate.longitude()))
            .zoom(15.0)
            .build()
    }

    private fun getDirectionsRoute(): DirectionsRoute {
        val directionsRouteAsJson = resources
            .openRawResource(R.raw.sample_route_with_toll)
            .bufferedReader()
            .use { it.readText() }
        return DirectionsRoute.fromJson(directionsRouteAsJson)
    }
}
