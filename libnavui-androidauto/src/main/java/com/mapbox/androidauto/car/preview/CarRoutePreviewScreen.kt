package com.mapbox.androidauto.car.preview

import android.text.SpannableString
import androidx.activity.OnBackPressedCallback
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.DurationSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.androidauto.ActiveGuidanceState
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSender
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.location.CarLocationRenderer
import com.mapbox.androidauto.car.navigation.CarCameraMode
import com.mapbox.androidauto.car.navigation.CarNavigationCamera
import com.mapbox.androidauto.car.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.androidauto.car.placeslistonmap.PlacesListOnMapLayerUtil
import com.mapbox.androidauto.car.search.PlaceRecord
import com.mapbox.androidauto.internal.car.extensions.handleStyleOnAttached
import com.mapbox.androidauto.internal.car.extensions.handleStyleOnDetached
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.navigation.audioguidance.muteAudioGuidance
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * After a destination has been selected. This view previews the route and lets
 * you select alternatives. From here, you can start turn-by-turn navigation.
 */
@MapboxExperimental
class CarRoutePreviewScreen(
    private val routePreviewCarContext: RoutePreviewCarContext,
    private val placeRecord: PlaceRecord,
    private val navigationRoutes: List<NavigationRoute>,
    private val placesLayerUtil: PlacesListOnMapLayerUtil = PlacesListOnMapLayerUtil(),
) : Screen(routePreviewCarContext.carContext) {

    private val carRoutesProvider = PreviewCarRoutesProvider(navigationRoutes)
    var selectedIndex = 0
    val carRouteLine = CarRouteLine(routePreviewCarContext.mainCarContext, carRoutesProvider)
    val carLocationRenderer = CarLocationRenderer(routePreviewCarContext.mainCarContext)
    val carSpeedLimitRenderer = CarSpeedLimitRenderer(routePreviewCarContext.mainCarContext)
    val carNavigationCamera = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.OVERVIEW,
        alternativeCarCameraMode = CarCameraMode.FOLLOWING,
        carRoutesProvider = carRoutesProvider,
    )

    private val backPressCallback = object : OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
            logAndroidAuto("CarRoutePreviewScreen OnBackPressedCallback")
            MapboxNavigationApp.current()!!.setNavigationRoutes(emptyList())
            screenManager.pop()
        }
    }

    private var styleLoadedListener: OnStyleLoadedListener? = null

    private val surfaceListener = object : MapboxCarMapObserver {

        override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
            super.onAttached(mapboxCarMapSurface)
            logAndroidAuto("CarRoutePreviewScreen loaded")
            styleLoadedListener = mapboxCarMapSurface.handleStyleOnAttached { style ->
                placesLayerUtil.initializePlacesListOnMapLayer(
                    style,
                    carContext.resources
                )
                val coordinate = placeRecord.coordinate ?: return@handleStyleOnAttached
                val featureCollection =
                    FeatureCollection.fromFeature(Feature.fromGeometry(coordinate))
                placesLayerUtil.updatePlacesListOnMapLayer(
                    style,
                    featureCollection
                )
            }
        }

        override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
            super.onDetached(mapboxCarMapSurface)
            logAndroidAuto("CarRoutePreviewScreen detached")
            mapboxCarMapSurface.handleStyleOnDetached(styleLoadedListener)?.let {
                placesLayerUtil.removePlacesListOnMapLayer(it)
            }
        }
    }

    init {
        logAndroidAuto("CarRoutePreviewScreen constructor")
        lifecycle.muteAudioGuidance()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("CarRoutePreviewScreen onResume")
                routePreviewCarContext.carContext.onBackPressedDispatcher.addCallback(
                    backPressCallback
                )
                routePreviewCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                routePreviewCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                routePreviewCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                routePreviewCarContext.mapboxCarMap.registerObserver(carRouteLine)
                routePreviewCarContext.mapboxCarMap.registerObserver(surfaceListener)
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("CarRoutePreviewScreen onPause")
                backPressCallback.remove()
                routePreviewCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                routePreviewCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                routePreviewCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                routePreviewCarContext.mapboxCarMap.unregisterObserver(carRouteLine)
                routePreviewCarContext.mapboxCarMap.unregisterObserver(surfaceListener)
            }
        })
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        navigationRoutes.forEach { navigationRoute ->
            val route = navigationRoute.directionsRoute
            val title = route.legs()?.first()?.summary() ?: placeRecord.name
            val duration = routePreviewCarContext.distanceFormatter.formatDistance(route.duration())
            val routeSpannableString = SpannableString("$duration $title")
            routeSpannableString.setSpan(
                DurationSpan.create(route.duration().toLong()),
                0,
                duration.length,
                0
            )

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(routeSpannableString)
                    .addText(duration)
                    .build()
            )
        }
        if (navigationRoutes.isNotEmpty()) {
            listBuilder.setSelectedIndex(selectedIndex)
            listBuilder.setOnSelectedListener { index ->
                val newRouteOrder = navigationRoutes.toMutableList()
                selectedIndex = index
                if (index > 0) {
                    val swap = newRouteOrder[0]
                    newRouteOrder[0] = newRouteOrder[index]
                    newRouteOrder[index] = swap
                    carRoutesProvider.updateRoutes(newRouteOrder)
                } else {
                    carRoutesProvider.updateRoutes(navigationRoutes)
                }
            }
        }

        return RoutePreviewNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(carContext.getString(R.string.car_action_preview_title))
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        CarFeedbackAction(
                            routePreviewCarContext.mapboxCarMap,
                            CarFeedbackSender(),
                            routePreviewCarContext.feedbackPollProvider
                                .getRoutePreviewFeedbackPoll(routePreviewCarContext.carContext),
                        ).getAction(this@CarRoutePreviewScreen)
                    )
                    .build()
            )
            .setHeaderAction(Action.BACK)
            .setNavigateAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.car_action_preview_navigate_button))
                    .setOnClickListener {
                        val mapboxNavigation = MapboxNavigationApp.current()!!
                        mapboxNavigation.setNavigationRoutes(
                            carRoutesProvider.navigationRoutes.value
                        )
                        MapboxCarApp.updateCarAppState(ActiveGuidanceState)
                    }
                    .build(),
            )
            .build()
    }
}
