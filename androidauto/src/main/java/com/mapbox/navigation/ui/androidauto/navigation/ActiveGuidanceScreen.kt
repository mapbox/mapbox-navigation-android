package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.Screen
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.action.MapboxMapActionStrip
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.location.CarLocationRenderer
import com.mapbox.navigation.ui.androidauto.navigation.roadlabel.CarRoadLabelRenderer
import com.mapbox.navigation.ui.androidauto.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.navigation.ui.androidauto.preview.CarRouteLineRenderer
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen

/**
 * After a route has been selected. This view gives turn-by-turn instructions
 * for completing the route.
 */
internal class ActiveGuidanceScreen constructor(
    private val mapboxCarContext: MapboxCarContext,
) : Screen(mapboxCarContext.carContext) {

    val carRouteLineRenderer = CarRouteLineRenderer()
    val carLocationRenderer = CarLocationRenderer()
    val carSpeedLimitRenderer = CarSpeedLimitRenderer(mapboxCarContext)
    val carNavigationCamera = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.FOLLOWING,
        alternativeCarCameraMode = CarCameraMode.OVERVIEW,
    )
    private val carRoadLabelRenderer = CarRoadLabelRenderer()
    private val navigationInfoProvider = CarNavigationInfoProvider()
        .invalidateOnChange(this)
    private val carActiveGuidanceMarkers = CarActiveGuidanceMarkers()
    private val mapActionStripBuilder = MapboxMapActionStrip(this, carNavigationCamera)
    private val carArrivalTrigger = CarArrivalTrigger()

    init {
        logAndroidAuto("ActiveGuidanceScreen constructor")
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    logAndroidAuto("ActiveGuidanceScreen onResume")
                    mapboxCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carRoadLabelRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                    mapboxCarContext.mapboxCarMap.setGestureHandler(
                        carNavigationCamera.gestureHandler,
                    )
                    mapboxCarContext.mapboxCarMap.registerObserver(carRouteLineRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carActiveGuidanceMarkers)
                    mapboxCarContext.mapboxCarMap.registerObserver(navigationInfoProvider)
                    MapboxNavigationApp.registerObserver(carArrivalTrigger)
                }

                override fun onPause(owner: LifecycleOwner) {
                    logAndroidAuto("ActiveGuidanceScreen onPause")
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carRoadLabelRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                    mapboxCarContext.mapboxCarMap.setGestureHandler(null)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carRouteLineRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carActiveGuidanceMarkers)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(navigationInfoProvider)
                    MapboxNavigationApp.unregisterObserver(carArrivalTrigger)
                }
            },
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onGetTemplate(): Template {
        logAndroidAuto("ActiveGuidanceScreen onGetTemplate")
        return NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.PRIMARY)
            .setActionStrip(
                mapboxCarContext.options.actionStripProvider
                    .getActionStrip(this, MapboxScreen.ACTIVE_GUIDANCE),
            )
            .setMapActionStrip(mapActionStripBuilder.build())
            .apply { navigationInfoProvider.setNavigationInfo(this) }
            .build()
    }
}
