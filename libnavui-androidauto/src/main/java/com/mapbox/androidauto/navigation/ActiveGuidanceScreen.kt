package com.mapbox.androidauto.navigation

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.R
import com.mapbox.androidauto.action.MapboxActionProvider
import com.mapbox.androidauto.action.MapboxMapActionStrip
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.location.CarLocationRenderer
import com.mapbox.androidauto.navigation.roadlabel.CarRoadLabelRenderer
import com.mapbox.androidauto.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.androidauto.preview.CarRouteLineRenderer
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * After a route has been selected. This view gives turn-by-turn instructions
 * for completing the route.
 */
internal class ActiveGuidanceScreen constructor(
    private val mapboxCarContext: MapboxCarContext,
    private val actionProviders: List<MapboxActionProvider>,
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
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("ActiveGuidanceScreen onResume")
                mapboxCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(carRoadLabelRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                mapboxCarContext.mapboxCarMap.setGestureHandler(carNavigationCamera.gestureHandler)
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
        })
    }

    override fun onGetTemplate(): Template {
        logAndroidAuto("ActiveGuidanceScreen onGetTemplate")
        val actionStrip = ActionStrip.Builder().apply {
            actionProviders.forEach {
                this.addAction(it.getAction(this@ActiveGuidanceScreen))
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
