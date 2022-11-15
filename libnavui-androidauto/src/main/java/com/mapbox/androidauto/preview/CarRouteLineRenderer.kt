package com.mapbox.androidauto.preview

import androidx.car.app.CarContext
import com.mapbox.androidauto.internal.extensions.handleStyleOnAttached
import com.mapbox.androidauto.internal.extensions.handleStyleOnDetached
import com.mapbox.androidauto.internal.extensions.mapboxNavigationForward
import com.mapbox.androidauto.internal.logAndroidAuto
import com.mapbox.androidauto.routes.CarRoutesProvider
import com.mapbox.androidauto.routes.NavigationCarRoutesProvider
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * This class is to simplify the interaction with [MapboxRouteLineApi], [MapboxRouteArrowView]
 * [MapboxRouteArrowApi], and [RouteProgressObserver] use cases that the app needs in the car.
 *
 * Anything for rendering the car's route line, is handled here at this point.
 */
class CarRouteLineRenderer(
    private val carRoutesProvider: CarRoutesProvider = NavigationCarRoutesProvider()
) : MapboxCarMapObserver {

    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder().build()
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    private var carMapSurface: MapboxCarMapSurface? = null
    private val styleFlow = MutableStateFlow<Style?>(null)

    private lateinit var routeLineView: MapboxRouteLineView
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeArrowApi: MapboxRouteArrowApi
    private lateinit var routeArrowView: MapboxRouteArrowView
    private lateinit var coroutineScope: CoroutineScope

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        styleFlow.value?.let {
            val result = routeLineApi.updateTraveledRouteLine(point)
            routeLineView.renderRouteLineUpdate(it, result)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        styleFlow.value?.let { style ->
            routeLineApi.updateWithRouteProgress(routeProgress) { result ->
                routeLineView.renderRouteLineUpdate(style, result)
            }
            routeArrowApi.addUpcomingManeuverArrow(routeProgress).also { arrowUpdate ->
                routeArrowView.renderManeuverUpdate(style, arrowUpdate)
            }
        }
    }

    private val navigationObserver = mapboxNavigationForward(this::onAttached, this::onDetached)

    private var styleLoadedListener: OnStyleLoadedListener? = null

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarRouteLine carMapSurface loaded $mapboxCarMapSurface")
        coroutineScope = MainScope()
        carMapSurface = mapboxCarMapSurface
        styleLoadedListener = mapboxCarMapSurface.handleStyleOnAttached { style ->
            val routeLineOptions = getMapboxRouteLineOptions(mapboxCarMapSurface.carContext, style)
            routeLineView = MapboxRouteLineView(routeLineOptions)
            routeLineApi = MapboxRouteLineApi(routeLineOptions)
            routeArrowApi = MapboxRouteArrowApi()
            routeArrowView = MapboxRouteArrowView(
                RouteArrowOptions.Builder(mapboxCarMapSurface.carContext)
                    .withAboveLayerId(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
                    .build()
            )
            styleFlow.value = style
        }
        val locationPlugin = mapboxCarMapSurface.mapSurface.location
        locationPlugin.addOnIndicatorPositionChangedListener(onPositionChangedListener)
        MapboxNavigationApp.registerObserver(navigationObserver)
        coroutineScope.launch {
            carRoutesProvider.navigationRoutes
                .combine(styleFlow) { routes, _ -> routes }
                .collect { onRoutesChanged(it) }
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarRouteLine carMapSurface detached $mapboxCarMapSurface")
        val mapSurface = mapboxCarMapSurface.mapSurface
        mapboxCarMapSurface.handleStyleOnDetached(styleLoadedListener)
        mapSurface.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        coroutineScope.cancel()
        styleFlow.value = null
        carMapSurface = null
    }

    private fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    private fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
    }

    private fun getMapboxRouteLineOptions(
        carContext: CarContext,
        style: Style
    ): MapboxRouteLineOptions {
        return MapboxRouteLineOptions.Builder(carContext)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId(findRoadLabelsLayerId(style))
            .withVanishingRouteLineEnabled(true)
            .build()
    }

    private fun findRoadLabelsLayerId(style: Style): String {
        return style.styleLayers
            .firstOrNull { layer -> layer.id.contains("road-label") }
            ?.id ?: "road-label-navigation"
    }

    private fun onRoutesChanged(routes: List<NavigationRoute>) {
        logAndroidAuto("CarRouteLine onRoutesChanged ${routes.size}")
        styleFlow.value?.let { style ->
            if (routes.isNotEmpty()) {
                val routesMetadata = MapboxNavigationApp.current()
                    ?.getAlternativeMetadataFor(routes)
                    ?: emptyList()
                routeLineApi.setNavigationRoutes(routes, routesMetadata) { value ->
                    routeLineView.renderRouteDrawData(style, value)
                }
            } else {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(style, value)
                }
                val clearArrowValue = routeArrowApi.clearArrows()
                routeArrowView.render(style, clearArrowValue)
            }
        }
    }
}
