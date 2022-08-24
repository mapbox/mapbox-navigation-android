package com.mapbox.androidauto.car

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
class MainCarScreen(
    private val mainCarContext: MainCarContext
) : Screen(mainCarContext.carContext) {

    val carRouteLine = CarRouteLine(mainCarContext)
    val carLocationRenderer = CarLocationRenderer(mainCarContext)
    val carSpeedLimitRenderer = CarSpeedLimitRenderer(mainCarContext)
    val carNavigationCamera = CarNavigationCamera(
        mainCarContext.mapboxNavigation,
        CarCameraMode.FOLLOWING,
        alternativeCarCameraMode = null,
    )
    private val roadLabelSurfaceLayer = RoadLabelSurfaceLayer(carContext)
    private val mainActionStrip = MainActionStrip(this, mainCarContext)
    private val mapActionStripBuilder = MainMapActionStrip(this, carNavigationCamera)

    init {
        logAndroidAuto("MainCarScreen constructor")
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("MainCarScreen onResume")
                mainCarContext.mapboxCarMap.registerObserver(carRouteLine)
                mainCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                mainCarContext.mapboxCarMap.registerObserver(roadLabelSurfaceLayer)
                mainCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                mainCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                mainCarContext.mapboxCarMap.setGestureHandler(carNavigationCamera.gestureHandler)
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("MainCarScreen onPause")
                mainCarContext.mapboxCarMap.unregisterObserver(carRouteLine)
                mainCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                mainCarContext.mapboxCarMap.unregisterObserver(roadLabelSurfaceLayer)
                mainCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                mainCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                mainCarContext.mapboxCarMap.setGestureHandler(null)
            }
        })
    }

    override fun onGetTemplate(): Template {
        return NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.PRIMARY)
            .setActionStrip(mainActionStrip.builder().build())
            .setMapActionStrip(mapActionStripBuilder.build())
            .build()
    }
}
