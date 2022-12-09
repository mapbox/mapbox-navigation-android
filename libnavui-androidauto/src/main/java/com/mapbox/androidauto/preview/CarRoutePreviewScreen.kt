package com.mapbox.androidauto.preview

import android.text.SpannableString
import androidx.annotation.UiThread
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.DurationSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.R
import com.mapbox.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.androidauto.internal.extensions.handleStyleOnAttached
import com.mapbox.androidauto.internal.extensions.handleStyleOnDetached
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.location.CarLocationRenderer
import com.mapbox.androidauto.navigation.CarCameraMode
import com.mapbox.androidauto.navigation.CarDistanceFormatter
import com.mapbox.androidauto.navigation.CarNavigationCamera
import com.mapbox.androidauto.navigation.audioguidance.muteAudioGuidance
import com.mapbox.androidauto.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.androidauto.placeslistonmap.PlacesListOnMapLayerUtil
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.androidauto.search.PlaceRecord
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

/**
 * After a destination has been selected. This view previews the route and lets
 * you select alternatives. From here, you can start turn-by-turn navigation.
 */
internal class CarRoutePreviewScreen @UiThread constructor(
    private val mapboxCarContext: MapboxCarContext,
    private val placeRecord: PlaceRecord,
    private val navigationRoutes: List<NavigationRoute>,
    private val placesLayerUtil: PlacesListOnMapLayerUtil = PlacesListOnMapLayerUtil(),
) : Screen(mapboxCarContext.carContext) {

    private val carRoutesProvider = PreviewCarRoutesProvider(navigationRoutes)
    private var selectedIndex = 0
    private val carRouteLineRenderer = CarRouteLineRenderer(carRoutesProvider)
    private val carLocationRenderer = CarLocationRenderer()
    private val carSpeedLimitRenderer = CarSpeedLimitRenderer(mapboxCarContext)
    private val carNavigationCamera = CarNavigationCamera(
        initialCarCameraMode = CarCameraMode.OVERVIEW,
        alternativeCarCameraMode = CarCameraMode.FOLLOWING,
        carRoutesProvider = carRoutesProvider,
    )

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
        addBackPressedHandler {
            logAndroidAuto("CarRoutePreviewScreen onBackPressed")
            mapboxCarContext.mapboxScreenManager.goBack()
        }
        lifecycle.muteAudioGuidance()
        lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("CarRoutePreviewScreen onResume")
                mapboxCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                mapboxCarContext.mapboxCarMap.registerObserver(carRouteLineRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(surfaceListener)
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("CarRoutePreviewScreen onPause")
                mapboxCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carRouteLineRenderer)
                mapboxCarContext.mapboxCarMap.unregisterObserver(surfaceListener)
            }
        })
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        navigationRoutes.forEach { navigationRoute ->
            val route = navigationRoute.directionsRoute
            val title = route.legs()?.first()?.summary() ?: placeRecord.name
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
                mapboxCarContext.options.actionStripProvider
                    .getActionStrip(this, MapboxScreen.ROUTE_PREVIEW)
            )
            .setHeaderAction(Action.BACK)
            .setNavigateAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.car_action_preview_navigate_button))
                    .setOnClickListener {
                        MapboxNavigationApp.current()!!.setNavigationRoutes(
                            carRoutesProvider.navigationRoutes.value
                        )
                        MapboxScreenManager.replaceTop(MapboxScreen.ACTIVE_GUIDANCE)
                    }
                    .build(),
            )
            .build()
    }
}
