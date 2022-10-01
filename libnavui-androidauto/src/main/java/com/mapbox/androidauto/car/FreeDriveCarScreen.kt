package com.mapbox.androidauto.car

import androidx.annotation.UiThread
import androidx.car.app.Screen
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.androidauto.car.location.CarLocationRenderer
import com.mapbox.androidauto.car.navigation.CarCameraMode
import com.mapbox.androidauto.car.navigation.CarNavigationCamera
import com.mapbox.androidauto.car.navigation.roadlabel.RoadLabelSurfaceLayer
import com.mapbox.androidauto.car.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.androidauto.car.preview.CarRouteLine
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.maps.MapboxExperimental

/**
 * When the app is launched from Android Auto
 */
@OptIn(MapboxExperimental::class)
class FreeDriveCarScreen @UiThread constructor(
    private val mapboxCarContext: MapboxCarContext
) : Screen(mapboxCarContext.carContext) {

    val carRouteLine = CarRouteLine()
    val carLocationRenderer = CarLocationRenderer()
    val carSpeedLimitRenderer = CarSpeedLimitRenderer(mapboxCarContext)
    val carNavigationCamera = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.FOLLOWING,
        alternativeCarCameraMode = null,
    )
    private val roadLabelSurfaceLayer = RoadLabelSurfaceLayer(carContext)
    private val mapActionStripBuilder = MainMapActionStrip(this, carNavigationCamera)

    init {
        logAndroidAuto("FreeDriveCarScreen constructor")
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("FreeDriveCarScreen onResume")
                mapboxCarContext.mapboxCarMap.registerObserver(carRouteLine)
                mapboxCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(roadLabelSurfaceLayer)
                mapboxCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                mapboxCarContext.mapboxCarMap.setGestureHandler(carNavigationCamera.gestureHandler)
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("FreeDriveCarScreen onPause")
                mapboxCarContext.mapboxCarMap.unregisterObserver(carRouteLine)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                mapboxCarContext.mapboxCarMap.unregisterObserver(roadLabelSurfaceLayer)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                mapboxCarContext.mapboxCarMap.setGestureHandler(null)
            }
        })
    }

    override fun onGetTemplate(): Template {
        return NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.PRIMARY)
            .setActionStrip(FreeDriveActionStrip(this).builder().build())
            .setMapActionStrip(mapActionStripBuilder.build())
            .build()
    }
}
