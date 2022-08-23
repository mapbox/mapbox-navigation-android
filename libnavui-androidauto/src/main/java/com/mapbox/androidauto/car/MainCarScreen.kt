package com.mapbox.androidauto.car

import androidx.car.app.Screen
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import com.mapbox.androidauto.car.location.CarLocationRenderer
import com.mapbox.androidauto.car.navigation.CarCameraMode
import com.mapbox.androidauto.car.navigation.CarNavigationCamera
import com.mapbox.androidauto.car.navigation.roadlabel.RoadLabelSurfaceLayer
import com.mapbox.androidauto.car.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.androidauto.car.preview.CarRouteLine
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.mapboxMapInstaller

/**
 * When the app is launched from Android Auto
 */
@OptIn(MapboxExperimental::class)
class MainCarScreen(
    mainCarContext: MainCarContext
) : Screen(mainCarContext.carContext) {

    private val carNavigationCamera = CarNavigationCamera(
        mainCarContext.mapboxNavigation,
        CarCameraMode.FOLLOWING,
        alternativeCarCameraMode = null,
    )

    private val mainActionStrip = MainActionStrip(this, mainCarContext)
    private val mapActionStripBuilder = MainMapActionStrip(this, carNavigationCamera)

    init {
        logAndroidAuto("MainCarScreen constructor")
        mapboxMapInstaller(mainCarContext.mapboxCarMap)
            .onResumed(
                CarRouteLine(mainCarContext),
                CarLocationRenderer(mainCarContext),
                RoadLabelSurfaceLayer(carContext, mainCarContext.mapboxNavigation),
                CarSpeedLimitRenderer(mainCarContext),
                carNavigationCamera,
            )
            .gestureHandler(carNavigationCamera.gestureHandler)
            .install()
    }

    override fun onGetTemplate(): Template {
        return NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.PRIMARY)
            .setActionStrip(mainActionStrip.builder().build())
            .setMapActionStrip(mapActionStripBuilder.build())
            .build()
    }
}
