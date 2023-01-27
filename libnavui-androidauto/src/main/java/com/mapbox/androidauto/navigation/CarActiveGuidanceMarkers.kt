package com.mapbox.androidauto.navigation

import com.mapbox.androidauto.internal.extensions.getStyle
import com.mapbox.androidauto.internal.extensions.styleFlow
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.placeslistonmap.PlacesListOnMapLayerUtil
import com.mapbox.androidauto.routes.CarRoutesProvider
import com.mapbox.androidauto.routes.NavigationCarRoutesProvider
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.base.route.NavigationRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CarActiveGuidanceMarkers(
    private val carRoutesProvider: CarRoutesProvider = NavigationCarRoutesProvider(),
) : MapboxCarMapObserver {

    private val placesLayerUtil = PlacesListOnMapLayerUtil()
    private lateinit var scope: CoroutineScope

    private fun updateMarkers(style: Style, routes: List<NavigationRoute>) {
        val features = routes.take(1)
            .mapNotNull { it.routeOptions.coordinatesList().lastOrNull() }
            .map { Feature.fromGeometry(it) }
        placesLayerUtil.updatePlacesListOnMapLayer(style, FeatureCollection.fromFeatures(features))
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarActiveGuidanceMarkers attached")
        val carContext = mapboxCarMapSurface.carContext
        scope = MainScope()
        scope.launch {
            mapboxCarMapSurface.styleFlow().collectLatest { style ->
                placesLayerUtil.initializePlacesListOnMapLayer(style, carContext.resources)
                carRoutesProvider.navigationRoutes.collect { updateMarkers(style, it) }
            }
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onDetached(mapboxCarMapSurface)
        logAndroidAuto("CarActiveGuidanceMarkers detached")
        scope.cancel()
        mapboxCarMapSurface.getStyle()?.let { placesLayerUtil.removePlacesListOnMapLayer(it) }
    }
}
