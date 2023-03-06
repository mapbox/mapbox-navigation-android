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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mapbox.androidauto.MapboxCarContext
import com.mapbox.androidauto.R
import com.mapbox.androidauto.internal.extensions.addBackPressedHandler
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.location.CarLocationRenderer
import com.mapbox.androidauto.navigation.CarActiveGuidanceMarkers
import com.mapbox.androidauto.navigation.CarCameraMode
import com.mapbox.androidauto.navigation.CarDistanceFormatter
import com.mapbox.androidauto.navigation.CarNavigationCamera
import com.mapbox.androidauto.navigation.audioguidance.muteAudioGuidance
import com.mapbox.androidauto.navigation.speedlimit.CarSpeedLimitRenderer
import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.preview.RoutesPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * After a destination has been selected. This view previews the route and lets
 * you select alternatives. From here, you can start turn-by-turn navigation.
 * The difference between [CarRoutePreviewScreen] and this class is that
 * the latter uses the experimental route preview state in Navigation SDK.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class CarRoutePreviewScreen2 @UiThread constructor(
    private val mapboxCarContext: MapboxCarContext,
) : Screen(mapboxCarContext.carContext) {

    private var routesPreview: RoutesPreview? = null
        set(value) {
            field = value
            invalidate()
        }

    private val carRoutesProvider = PreviewCarRoutesProvider2()
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
        logAndroidAuto("CarRoutePreviewScreen2 constructor")
        addBackPressedHandler {
            logAndroidAuto("CarRoutePreviewScreen2 onBackPressed")
            mapboxCarContext.mapboxScreenManager.goBack()
            MapboxNavigationApp.current()!!.setRoutesPreview(emptyList())
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                carRoutesProvider.routesPreview.collect { routesPreview = it }
            }
        }
        lifecycle.muteAudioGuidance()
        lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {
                logAndroidAuto("CarRoutePreviewScreen2 onResume")
                mapboxCarContext.mapboxCarMap.registerObserver(carLocationRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(carSpeedLimitRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(carNavigationCamera)
                mapboxCarContext.mapboxCarMap.registerObserver(carRouteLineRenderer)
                mapboxCarContext.mapboxCarMap.registerObserver(carMarkers)
            }

            override fun onPause(owner: LifecycleOwner) {
                logAndroidAuto("CarRoutePreviewScreen2 onPause")
                mapboxCarContext.mapboxCarMap.unregisterObserver(carLocationRenderer)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carSpeedLimitRenderer)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carNavigationCamera)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carRouteLineRenderer)
                mapboxCarContext.mapboxCarMap.unregisterObserver(carMarkers)
            }
        })
    }

    override fun onGetTemplate(): Template {
        val templateBuilder = RoutePreviewNavigationTemplate.Builder()
        val routesPreview = routesPreview
        val originalRoutesList = routesPreview?.originalRoutesList
        if (originalRoutesList.isNullOrEmpty()) {
            templateBuilder.setLoading(true)
        } else {
            val listBuilder = ItemList.Builder()
            for (navigationRoute in originalRoutesList) {
                val route = navigationRoute.directionsRoute
                val duration = CarDistanceFormatter.formatDistance(route.duration())
                val title = route.legs()?.first()?.summary()?.let { "$duration $it" } ?: duration
                val routeSpannableString = SpannableString(title)
                val span = DurationSpan.create(route.duration().toLong())
                routeSpannableString.setSpan(span, 0, duration.length, 0)

                val item = Row.Builder().setTitle(routeSpannableString).addText(duration).build()
                listBuilder.addItem(item)
            }
            listBuilder.setSelectedIndex(routesPreview.primaryRouteIndex)
            listBuilder.setOnSelectedListener { index ->
                carRoutesProvider.updateSelectedRoute(index)
            }
            templateBuilder.setItemList(listBuilder.build())
            templateBuilder.setNavigateAction(
                Action.Builder()
                    .setTitle(carContext.getString(R.string.car_action_preview_navigate_button))
                    .setOnClickListener {
                        MapboxNavigationApp.current()!!.setNavigationRoutes(
                            routesPreview.routesList,
                        )
                        MapboxScreenManager.replaceTop(MapboxScreen.ACTIVE_GUIDANCE)
                    }
                    .build(),
            )
        }

        return templateBuilder
            .setTitle(carContext.getString(R.string.car_action_preview_title))
            .setActionStrip(
                mapboxCarContext.options.actionStripProvider
                    .getActionStrip(screen = this, MapboxScreen.ROUTE_PREVIEW),
            )
            .setHeaderAction(Action.BACK)
            .build()
    }
}
