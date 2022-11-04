package com.mapbox.navigation.qa_test_app.view.componentinstaller.components

import android.location.Location
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowNewRawLocation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@ExperimentalPreviewMapboxNavigationAPI
class FindRouteOnLongPress(
    val mapView: MapView,
    private val customizeRouteOptions: RouteOptions.Builder.() -> Unit = {}
) : UIComponent(), OnMapLongClickListener {

    private var originLocation: Location? = null
    private var mapboxNavigation: MapboxNavigation? = null

    override fun onMapLongClick(point: Point): Boolean {
        findRoute(point)
        return false
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        this.mapboxNavigation = mapboxNavigation

        coroutineScope.launch {
            originLocation = runCatching {
                val locationEngine = mapboxNavigation.navigationOptions.locationEngine
                locationEngine.getLastLocation()
            }.getOrNull()

            mapboxNavigation.flowNewRawLocation().collect {
                originLocation = it
            }
        }

        mapView.gestures.addOnMapLongClickListener(this)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapView.gestures.removeOnMapLongClickListener(this)
        this.mapboxNavigation = null
    }

    private fun findRoute(destination: Point) = mapboxNavigation?.also { mapboxNavigation ->
        val origin = originLocation?.run { Point.fromLngLat(longitude, latitude) } ?: return@also

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(mapView.context)
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .alternatives(true)
            .apply(customizeRouteOptions)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setRoutes(routes.reversed())
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) = Unit

                override fun onCanceled(
                    routeOptions: RouteOptions,
                    routerOrigin: RouterOrigin
                ) = Unit
            }
        )
    }

    @Throws(SecurityException::class)
    private suspend fun LocationEngine.getLastLocation() =
        suspendCancellableCoroutine<Location> { cont ->
            getLastLocation(object : LocationEngineCallback<LocationEngineResult> {
                override fun onSuccess(result: LocationEngineResult) {
                    result.lastLocation?.also {
                        cont.resume(it)
                    }
                }

                override fun onFailure(exception: Exception) {
                    cont.resumeWithException(exception)
                }
            })
        }
}
