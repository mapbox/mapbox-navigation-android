package com.mapbox.androidauto.car.preview

import android.text.SpannableString
import androidx.annotation.UiThread
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
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.location.CarLocationRenderer
import com.mapbox.androidauto.car.navigation.CarCameraMode
import com.mapbox.androidauto.car.navigation.CarDistanceFormatter
import com.mapbox.androidauto.car.navigation.CarNavigationCamera
import com.mapbox.androidauto.car.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.androidauto.car.placeslistonmap.PlacesListOnMapLayerUtil
import com.mapbox.androidauto.internal.car.extensions.addBackPressedHandler
import com.mapbox.androidauto.internal.car.extensions.handleStyleOnAttached
import com.mapbox.androidauto.internal.car.extensions.handleStyleOnDetached
import com.mapbox.androidauto.internal.car.extensions.mapboxNavigationForward
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.navigation.audioguidance.muteAudioGuidance
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesPreview
import com.mapbox.navigation.core.directions.session.RoutesPreviewObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * After a destination has been selected. This view previews the route and lets
 * you select alternatives. From here, you can start turn-by-turn navigation.
 */
@OptIn(MapboxExperimental::class)
internal class CarRoutePreviewScreen @UiThread constructor(
    private val routePreviewCarContext: RoutePreviewCarContext,
    private val placesLayerUtil: PlacesListOnMapLayerUtil = PlacesListOnMapLayerUtil(),
) : Screen(routePreviewCarContext.carContext) {
    private val carRouteLine = CarRouteLine()
    private val carLocationRenderer = CarLocationRenderer()
    private val carSpeedLimitRenderer = CarSpeedLimitRenderer(
        routePreviewCarContext.mapboxCarContext
    )
    private val carNavigationCamera = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.OVERVIEW,
        alternativeCarCameraMode = CarCameraMode.FOLLOWING,
    )

    private var styleLoadedListener: OnStyleLoadedListener? = null
    private var style: Style? = null
    private val navigationObserver = mapboxNavigationForward(this::onAttached, this::onDetached)

    private val routesPreviewObserver = RoutesPreviewObserver {
        updateRoutePreview(style, it)
    }

    private val surfaceListener = object : MapboxCarMapObserver {

        override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
            super.onAttached(mapboxCarMapSurface)
            logAndroidAuto("CarRoutePreviewScreen loaded")
            styleLoadedListener = mapboxCarMapSurface.handleStyleOnAttached { style ->
                placesLayerUtil.initializePlacesListOnMapLayer(
                    style,
                    carContext.resources
                )
                this@CarRoutePreviewScreen.style = style
                updateRoutePreview(style, MapboxNavigationApp.current()?.getRoutePreview())
            }
            MapboxNavigationApp.registerObserver(navigationObserver)
        }

        override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
            super.onDetached(mapboxCarMapSurface)
            logAndroidAuto("CarRoutePreviewScreen detached")
            mapboxCarMapSurface.handleStyleOnDetached(styleLoadedListener)?.let {
                placesLayerUtil.removePlacesListOnMapLayer(it)
            }
            this@CarRoutePreviewScreen.style = null
            MapboxNavigationApp.unregisterObserver(navigationObserver)
        }
    }

    private fun updateRoutePreview(style: Style?, routes: RoutesPreview?) {
        val coordinate = routes?.navigationRoutes?.lastOrNull()?.routeOptions
            ?.coordinatesList()?.lastOrNull()
            ?: return
        val style = style
            ?: return
        val featureCollection = FeatureCollection.fromFeature(Feature.fromGeometry(coordinate))
        placesLayerUtil.updatePlacesListOnMapLayer(
            style,
            featureCollection
        )
    }

    init {
        logAndroidAuto("CarRoutePreviewScreen constructor")
        addBackPressedHandler {
            logAndroidAuto("CarRoutePreviewScreen onBackPressed")
            routePreviewCarContext.mapboxScreenManager.goBack()
        }
        lifecycle.muteAudioGuidance()
        lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("CarRoutePreviewScreen onResume")
                routePreviewCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                routePreviewCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                routePreviewCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                routePreviewCarContext.mapboxCarMap.registerObserver(carRouteLine)
                routePreviewCarContext.mapboxCarMap.registerObserver(surfaceListener)
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("CarRoutePreviewScreen onPause")
                routePreviewCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                routePreviewCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                routePreviewCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                routePreviewCarContext.mapboxCarMap.unregisterObserver(carRouteLine)
                routePreviewCarContext.mapboxCarMap.unregisterObserver(surfaceListener)
            }
        })
    }

    private fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRoutePreviewObserver(routesPreviewObserver)
    }

    private fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRoutePreviewObserver(routesPreviewObserver)
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val routesPreview = MapboxNavigationApp.current()!!.getRoutePreview()
        val navigationRoutes = routesPreview.navigationRoutes
        navigationRoutes.forEach { navigationRoute ->
            val route = navigationRoute.directionsRoute
            val title = route.legs()?.first()?.summary() ?: routesPreview.destinationName
            val duration = CarDistanceFormatter.formatDistance(route.duration())
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
            listBuilder.setSelectedIndex(routesPreview.selectedIndex)
            listBuilder.setOnSelectedListener { index ->
                MapboxNavigationApp.current()!!.setRoutePreviewIndex(index)
            }
        }

        return RoutePreviewNavigationTemplate.Builder()
            .setItemList(listBuilder.build())
            .setTitle(carContext.getString(R.string.car_action_preview_title))
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        CarFeedbackAction(
                            MapboxScreen.ROUTE_PREVIEW_FEEDBACK
                        ).getAction(this@CarRoutePreviewScreen)
                    )
                    .build()
            )
            .setHeaderAction(Action.BACK)
            .setNavigateAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.car_action_preview_navigate_button))
                    .setOnClickListener {
                        MapboxNavigationApp.current()!!.startActiveGuidance()
                        MapboxScreenManager.replaceTop(MapboxScreen.ACTIVE_GUIDANCE)
                    }
                    .build(),
            )
            .build()
    }
}
