package com.mapbox.navigation.ui.androidauto.preview

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
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.location.CarLocationRenderer
import com.mapbox.navigation.ui.androidauto.navigation.CarActiveGuidanceMarkers
import com.mapbox.navigation.ui.androidauto.navigation.CarCameraMode
import com.mapbox.navigation.ui.androidauto.navigation.CarDistanceFormatter
import com.mapbox.navigation.ui.androidauto.navigation.CarNavigationCamera
import com.mapbox.navigation.ui.androidauto.navigation.audioguidance.muteAudioGuidance
import com.mapbox.navigation.ui.androidauto.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord

/**
 * After a destination has been selected. This view previews the route and lets
 * you select alternatives. From here, you can start turn-by-turn navigation.
 */
internal class CarRoutePreviewScreen @UiThread constructor(
    private val mapboxCarContext: MapboxCarContext,
    private val placeRecord: PlaceRecord,
    private val navigationRoutes: List<NavigationRoute>,
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
    private val carMarkers = CarActiveGuidanceMarkers(carRoutesProvider)

    init {
        logAndroidAuto("CarRoutePreviewScreen constructor")
        addBackPressedHandler {
            logAndroidAuto("CarRoutePreviewScreen onBackPressed")
            mapboxCarContext.mapboxScreenManager.goBack()
        }
        lifecycle.muteAudioGuidance()
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {

                override fun onResume(owner: LifecycleOwner) {
                    logAndroidAuto("CarRoutePreviewScreen onResume")
                    mapboxCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                    mapboxCarContext.mapboxCarMap.registerObserver(carRouteLineRenderer)
                    mapboxCarContext.mapboxCarMap.registerObserver(carMarkers)
                }

                override fun onPause(owner: LifecycleOwner) {
                    logAndroidAuto("CarRoutePreviewScreen onPause")
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carRouteLineRenderer)
                    mapboxCarContext.mapboxCarMap.unregisterObserver(carMarkers)
                }
            },
        )
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        navigationRoutes.forEach { navigationRoute ->
            val route = navigationRoute.directionsRoute
            val title = route.legs()?.first()?.summary() ?: placeRecord.name
            val routeSpannableString = SpannableString("  $title")
            val span = DurationSpan.create(route.duration().toLong())
            routeSpannableString.setSpan(span, 0, 1, 0)

            val distance = CarDistanceFormatter.formatDistance(route.distance())
            val item = Row.Builder().setTitle(routeSpannableString).addText(distance).build()
            listBuilder.addItem(item)
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
                    .getActionStrip(this, MapboxScreen.ROUTE_PREVIEW),
            )
            .setHeaderAction(Action.BACK)
            .setNavigateAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.car_action_preview_navigate_button))
                    .setOnClickListener {
                        MapboxNavigationApp.current()!!.setNavigationRoutes(
                            carRoutesProvider.navigationRoutes.value,
                        )
                        MapboxScreenManager.replaceTop(MapboxScreen.ACTIVE_GUIDANCE)
                    }
                    .build(),
            )
            .build()
    }
}
