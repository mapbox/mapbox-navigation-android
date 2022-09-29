package com.mapbox.androidauto.car.navigation

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.MainMapActionStrip
import com.mapbox.androidauto.car.action.MapboxActionProvider
import com.mapbox.androidauto.car.location.CarLocationRenderer
import com.mapbox.androidauto.car.navigation.roadlabel.RoadLabelSurfaceLayer
import com.mapbox.androidauto.car.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.androidauto.car.preview.CarRouteLine
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * After a route has been selected. This view gives turn-by-turn instructions
 * for completing the route.
 */
@OptIn(MapboxExperimental::class)
class ActiveGuidanceScreen(
    private val mainCarContext: MainCarContext,
    private val actionProviders: List<MapboxActionProvider>,
) : Screen(mainCarContext.carContext) {

    val carRouteLine = CarRouteLine()
    val carLocationRenderer = CarLocationRenderer()
    val carSpeedLimitRenderer = CarSpeedLimitRenderer(mainCarContext)
    val carNavigationCamera = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.FOLLOWING,
        alternativeCarCameraMode = CarCameraMode.OVERVIEW,
    )
    private val roadLabelSurfaceLayer = RoadLabelSurfaceLayer(carContext)
    private val navigationInfoProvider = CarNavigationInfoProvider()
        .invalidateOnChange(this)
    private val carActiveGuidanceMarkers = CarActiveGuidanceMarkers()
    private val mapActionStripBuilder = MainMapActionStrip(this, carNavigationCamera)
    private val carArrivalTrigger = CarArrivalTrigger()

    init {
        logAndroidAuto("ActiveGuidanceScreen constructor")
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("ActiveGuidanceScreen onResume")
                mainCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                mainCarContext.mapboxCarMap.registerObserver(roadLabelSurfaceLayer)
                mainCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                mainCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                mainCarContext.mapboxCarMap.setGestureHandler(carNavigationCamera.gestureHandler)
                mainCarContext.mapboxCarMap.registerObserver(carRouteLine)
                mainCarContext.mapboxCarMap.registerObserver(carActiveGuidanceMarkers)
                mainCarContext.mapboxCarMap.registerObserver(navigationInfoProvider)
                MapboxNavigationApp.registerObserver(carArrivalTrigger)
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("ActiveGuidanceScreen onPause")
                mainCarContext.mapboxCarMap.unregisterObserver(roadLabelSurfaceLayer)
                mainCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                mainCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                mainCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                mainCarContext.mapboxCarMap.setGestureHandler(null)
                mainCarContext.mapboxCarMap.unregisterObserver(carRouteLine)
                mainCarContext.mapboxCarMap.unregisterObserver(carActiveGuidanceMarkers)
                mainCarContext.mapboxCarMap.unregisterObserver(navigationInfoProvider)
                MapboxNavigationApp.unregisterObserver(carArrivalTrigger)
            }
        })
    }

    override fun onGetTemplate(): Template {
        logAndroidAuto("ActiveGuidanceScreen onGetTemplate")
        val actionStrip = ActionStrip.Builder().apply {
            actionProviders.forEach {
                when (it) {
                    is MapboxActionProvider.ScreenActionProvider -> {
                        this.addAction(it.getAction(this@ActiveGuidanceScreen))
                    }
                    is MapboxActionProvider.ActionProvider -> {
                        this.addAction(it.getAction())
                    }
                }
            }
            this.addAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.car_action_navigation_stop_button))
                    .setOnClickListener {
                        carArrivalTrigger.triggerArrival()
                    }.build()
            )
        }.build()

        return NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.PRIMARY)
            .setActionStrip(actionStrip)
            .setMapActionStrip(mapActionStripBuilder.build())
            .apply { navigationInfoProvider.setNavigationInfo(this) }
            .build()
    }
}
