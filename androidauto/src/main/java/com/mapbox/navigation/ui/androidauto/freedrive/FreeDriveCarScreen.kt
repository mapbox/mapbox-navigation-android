package com.mapbox.navigation.ui.androidauto.freedrive

import androidx.annotation.UiThread
import androidx.car.app.Screen
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.action.MapboxMapActionStrip
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.location.CarLocationRenderer
import com.mapbox.navigation.ui.androidauto.navigation.CarCameraMode
import com.mapbox.navigation.ui.androidauto.navigation.CarNavigationCamera
import com.mapbox.navigation.ui.androidauto.navigation.roadlabel.CarRoadLabelRenderer
import com.mapbox.navigation.ui.androidauto.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.navigation.ui.androidauto.preview.CarRouteLineRenderer
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen

/**
 * When the app is launched from Android Auto
 */
internal class FreeDriveCarScreen @UiThread constructor(
    private val mapboxCarContext: MapboxCarContext,
) : Screen(mapboxCarContext.carContext) {

    val carRouteLineRenderer = CarRouteLineRenderer()
    val carLocationRenderer = CarLocationRenderer()
    val carSpeedLimitRenderer = CarSpeedLimitRenderer(mapboxCarContext)
    val carNavigationCamera = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.FOLLOWING,
        alternativeCarCameraMode = null,
    )
    private val carRoadLabelRenderer = CarRoadLabelRenderer()
    private val mapActionStripBuilder = MapboxMapActionStrip(this, carNavigationCamera)

    init {
        logAndroidAuto("FreeDriveCarScreen constructor")
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    logAndroidAuto("FreeDriveCarScreen onResume")
                    mapboxCarContext.mapboxCarMap.registerObserver(carRouteLineRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carRoadLabelRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                    mapboxCarContext.mapboxCarMap.setGestureHandler(
                        carNavigationCamera.gestureHandler,
                    )
                }

                override fun onPause(owner: LifecycleOwner) {
                    logAndroidAuto("FreeDriveCarScreen onPause")
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carRouteLineRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carRoadLabelRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                    mapboxCarContext.mapboxCarMap.setGestureHandler(null)
                }
            },
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onGetTemplate(): Template {
        return NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.PRIMARY)
            .setActionStrip(
                mapboxCarContext.options.actionStripProvider
                    .getActionStrip(this, MapboxScreen.FREE_DRIVE),
            )
            .setMapActionStrip(mapActionStripBuilder.build())
            .build()
    }
}
