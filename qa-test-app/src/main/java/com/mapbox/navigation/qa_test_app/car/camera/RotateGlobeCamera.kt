package com.mapbox.navigation.qa_test_app.car.camera

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.skyLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SkyType
import com.mapbox.maps.plugin.MapProjection
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.ui.car.map.MapboxCarMapObserver
import com.mapbox.navigation.ui.car.map.MapboxCarMapSurface
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

/**
 * Place holder experience while we add more experiences to the android auto sdk.
 * This is a nice map camera when you don't have the current location connected.
 */
@OptIn(ExperimentalMapboxNavigationAPI::class, MapboxExperimental::class)
class RotateGlobeCamera(
    private val lifecycleOwner: LifecycleOwner
) : MapboxCarMapObserver {

    private var rotateGlobeDelegate: RotateGlobeDelegate? = null

    override fun loaded(mapboxCarMapSurface: MapboxCarMapSurface) {
        addSkyLayer(mapboxCarMapSurface.style)
        mapboxCarMapSurface.mapSurface.getMapboxMap().setMapProjection(MapProjection.Globe)
        val rotateGlobeDelegate = RotateGlobeDelegate(mapboxCarMapSurface.mapSurface.getMapboxMap())
        this.rotateGlobeDelegate = rotateGlobeDelegate
        rotateGlobeDelegate.start(lifecycleOwner)
    }

    override fun detached(mapboxCarMapSurface: MapboxCarMapSurface) {
        rotateGlobeDelegate?.stop()
        rotateGlobeDelegate = null
    }

    private fun addSkyLayer(style: Style) {
        val skyLayerExists = style.styleLayers.any { layer -> layer.type == "sky" }
        if (!skyLayerExists) {
            style.addLayer(
                skyLayer("sky") {
                    skyType(SkyType.ATMOSPHERE)
                }
            )
        }
    }

    private class RotateGlobeDelegate(mapboxMap: MapboxMap) {
        fun start(lifecycleOwner: LifecycleOwner) {
            spinner.observe(lifecycleOwner, observer)
        }

        fun stop() {
            spinner.removeObserver(observer)
        }

        private var longitude = -52.0
        private val spinner: LiveData<Double> = flow {
            while (currentCoroutineContext().isActive) {
                emit(longitude)
                delay(80)
                longitude = (longitude + 0.8) % 360.0
            }
        }.asLiveData()

        private val observer = Observer<Double> { longitude ->
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .zoom(1.0)
                    .center(Point.fromLngLat(longitude, 0.0))
                    .build()
            )
        }
    }
}
