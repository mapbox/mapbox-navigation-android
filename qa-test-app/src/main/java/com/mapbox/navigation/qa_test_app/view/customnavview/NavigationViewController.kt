package com.mapbox.navigation.qa_test_app.view.customnavview

import android.location.Location
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowNewRawLocation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.qa_test_app.utils.fetchRoute
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

internal class NavigationViewController(
    lifecycleOwner: LifecycleOwner,
    private val navigationView: NavigationView
) : LifecycleEventObserver, UIComponent() {
    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    val location = MutableStateFlow<Location?>(null)
    private val mapboxNavigation = MutableStateFlow<MapboxNavigation?>(null)

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                MapboxNavigationApp.registerObserver(this)
            }
            Lifecycle.Event.ON_DESTROY -> {
                MapboxNavigationApp.unregisterObserver(this)
            }
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation.value = mapboxNavigation
        mapboxNavigation.flowNewRawLocation().observe {
            location.value = it
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        this.mapboxNavigation.value = null
    }

    suspend fun startActiveGuidance(destination: Point) {
        val routes = fetchRoute(destination)
        navigationView.api.startActiveGuidance(routes)
    }

    suspend fun fetchRoute(destination: Point): List<NavigationRoute> {
        val origin = location.filterNotNull().first().toPoint()
        val mapboxNavigation = this.mapboxNavigation.filterNotNull().first()
        return mapboxNavigation.fetchRoute(origin, destination)
    }
}
