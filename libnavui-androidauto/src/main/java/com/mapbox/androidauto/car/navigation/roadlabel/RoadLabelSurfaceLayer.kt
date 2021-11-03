package com.mapbox.androidauto.car.navigation.roadlabel

import android.graphics.Color
import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.androidauto.car.navigation.MapUserStyleObserver
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.androidauto.logAndroidAutoFailure
import com.mapbox.androidauto.surfacelayer.CarSurfaceLayer
import com.mapbox.androidauto.surfacelayer.textview.CarTextLayerHost
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.model.RouteShield

/**
 * This will show the current road name at the bottom center of the screen.
 *
 * In your [Screen], create an instance of this class and enable by
 * registering it to the [MapboxCarMap.registerObserver]. Disable by
 * removing the listener with [MapboxCarMap.unregisterObserver].
 */
@OptIn(MapboxExperimental::class, ExperimentalPreviewMapboxNavigationAPI::class)
class RoadLabelSurfaceLayer(
    val carContext: CarContext,
    val mapboxNavigation: MapboxNavigation,
) : CarSurfaceLayer() {

    private val roadLabelRenderer = RoadLabelRenderer(carContext.resources)
    private val carTextLayerHost = CarTextLayerHost()
    private val routeShieldApi = MapboxRouteShieldApi()
    private val mapUserStyleObserver = MapUserStyleObserver()

    private val roadNameObserver = object : RoadNameObserver(
        mapboxNavigation,
        routeShieldApi,
        mapUserStyleObserver
    ) {
        override fun onRoadUpdate(road: List<RoadComponent>, shields: List<RouteShield>) {
            val bitmap = roadLabelRenderer.render(road, shields, roadLabelOptions())
            carTextLayerHost.offerBitmap(bitmap)
        }
    }

    override fun children() = listOf(carTextLayerHost.mapScene)

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("RoadLabelSurfaceLayer carMapSurface loaded")
        super.onAttached(mapboxCarMapSurface)
        mapboxCarMapSurface.mapSurface.getMapboxMap().getStyle { style ->
            val aboveLayer = style.styleLayers.last().id.takeUnless {
                it == BELOW_LAYER
            }

            style.addPersistentStyleCustomLayer(
                layerId = CAR_NAVIGATION_VIEW_LAYER_ID,
                carTextLayerHost,
                LayerPosition(aboveLayer, BELOW_LAYER, null)
            ).error?.let {
                logAndroidAutoFailure("Add custom layer exception $it")
            }

            mapUserStyleObserver.onAttached(mapboxCarMapSurface)
            val bitmap = roadLabelRenderer.render(
                roadNameObserver.currentRoad,
                roadNameObserver.currentShields,
                roadLabelOptions()
            )
            carTextLayerHost.offerBitmap(bitmap)
            mapboxNavigation.registerLocationObserver(roadNameObserver)
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("RoadLabelSurfaceLayer carMapSurface detached")
        mapboxCarMapSurface.mapSurface.getMapboxMap().getStyle()
            ?.removeStyleLayer(CAR_NAVIGATION_VIEW_LAYER_ID)
        mapboxNavigation.unregisterLocationObserver(roadNameObserver)
        routeShieldApi.cancel()
        mapUserStyleObserver.onDetached(mapboxCarMapSurface)
        super.onDetached(mapboxCarMapSurface)
    }

    private fun roadLabelOptions(): RoadLabelOptions =
        if (carContext.isDarkMode) {
            DARK_OPTIONS
        } else {
            LIGHT_OPTIONS
        }

    private companion object {
        private const val CAR_NAVIGATION_VIEW_LAYER_ID = "car_road_label_layer_id"
        private const val BELOW_LAYER = LocationComponentConstants.LOCATION_INDICATOR_LAYER

        private val DARK_OPTIONS = RoadLabelOptions.Builder()
            .shadowColor(null)
            .roundedLabelColor(Color.BLACK)
            .textColor(Color.WHITE)
            .build()

        private val LIGHT_OPTIONS = RoadLabelOptions.Builder()
            .roundedLabelColor(Color.WHITE)
            .textColor(Color.BLACK)
            .build()
    }
}
