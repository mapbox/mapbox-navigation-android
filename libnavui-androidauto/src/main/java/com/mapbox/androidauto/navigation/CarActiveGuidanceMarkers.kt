package com.mapbox.androidauto.navigation

import com.mapbox.androidauto.internal.extensions.getStyle
import com.mapbox.androidauto.internal.extensions.handleStyleOnAttached
import com.mapbox.androidauto.internal.extensions.handleStyleOnDetached
import com.mapbox.androidauto.internal.extensions.mapboxNavigationForward
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.placeslistonmap.PlacesListOnMapLayerUtil
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

class CarActiveGuidanceMarkers : MapboxCarMapObserver {
    private var styleLoadedListener: OnStyleLoadedListener? = null
    private var mapboxCarMapSurface: MapboxCarMapSurface? = null
    private val navigationObserver = mapboxNavigationForward(this::onAttached, this::onDetached)
    private val routesObserver = RoutesObserver { updateMarkers() }
    private val placesLayerUtil = PlacesListOnMapLayerUtil()

    private fun updateMarkers() {
        val route = MapboxNavigationApp.current()?.getNavigationRoutes()?.firstOrNull()
            ?: return
        val coordinate = route.routeOptions.coordinatesList().lastOrNull()
            ?: return
        val featureCollection = FeatureCollection.fromFeature(Feature.fromGeometry(coordinate))
        mapboxCarMapSurface?.getStyle()?.let {
            placesLayerUtil.updatePlacesListOnMapLayer(it, featureCollection)
        }
    }

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("ActiveGuidanceScreen loaded")
        this.mapboxCarMapSurface = mapboxCarMapSurface
        val carContext = mapboxCarMapSurface.carContext
        styleLoadedListener = mapboxCarMapSurface.handleStyleOnAttached {
            placesLayerUtil.initializePlacesListOnMapLayer(it, carContext.resources)
            updateMarkers()
        }
        MapboxNavigationApp.registerObserver(navigationObserver)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        super.onDetached(mapboxCarMapSurface)
        logAndroidAuto("ActiveGuidanceScreen detached")
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        mapboxCarMapSurface.handleStyleOnDetached(styleLoadedListener)?.let {
            placesLayerUtil.removePlacesListOnMapLayer(it)
        }
    }

    private fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRoutesObserver(routesObserver)
    }

    private fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
    }
}
