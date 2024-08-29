package com.mapbox.navigation.ui.androidauto.preview

import androidx.car.app.CarContext
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.androidauto.internal.extensions.mapboxNavigationForward
import com.mapbox.navigation.ui.androidauto.internal.extensions.styleFlow
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import com.mapbox.navigation.ui.androidauto.routes.NavigationCarRoutesProvider
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * This class is to simplify the interaction with [MapboxRouteLineApi], [MapboxRouteArrowView]
 * [MapboxRouteArrowApi], and [RouteProgressObserver] use cases that the app needs in the car.
 *
 * Anything for rendering the car's route line, is handled here at this point.
 */
class CarRouteLineRenderer(
    private val carRoutesProvider: CarRoutesProvider = NavigationCarRoutesProvider(),
) : MapboxCarMapObserver {

    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder().build()
    }

    private var style: Style? = null

    private lateinit var routeLineView: MapboxRouteLineView
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeArrowApi: MapboxRouteArrowApi
    private lateinit var routeArrowView: MapboxRouteArrowView
    private lateinit var coroutineScope: CoroutineScope

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val style = style ?: return@OnIndicatorPositionChangedListener
        val result = routeLineApi.updateTraveledRouteLine(point)
        routeLineView.renderRouteLineUpdate(style, result)
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        val style = style ?: return@RouteProgressObserver
        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            routeLineView.renderRouteLineUpdate(style, result)
        }
        routeArrowApi.addUpcomingManeuverArrow(routeProgress).also { arrowUpdate ->
            routeArrowView.renderManeuverUpdate(style, arrowUpdate)
        }
    }

    private val navigationObserver = mapboxNavigationForward(this::onAttached, this::onDetached)

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarRouteLine carMapSurface loaded $mapboxCarMapSurface")
        coroutineScope = MainScope()
        coroutineScope.launch {
            mapboxCarMapSurface.styleFlow().collectLatest { style ->
                val carContext = mapboxCarMapSurface.carContext
                routeLineView = MapboxRouteLineView(
                    getMapboxRouteLineViewOptions(carContext, style),
                )
                routeLineView.initializeLayers(style)
                routeLineApi = MapboxRouteLineApi(getMapboxRouteLineApiOptions(style))
                routeArrowApi = MapboxRouteArrowApi()
                routeArrowView = MapboxRouteArrowView(
                    RouteArrowOptions.Builder(carContext)
                        .withAboveLayerId(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
                        .build(),
                )
                this@CarRouteLineRenderer.style = style
                carRoutesProvider.navigationRoutes.collect { onRoutesChanged(style, it) }
            }
        }
        val locationPlugin = mapboxCarMapSurface.mapSurface.location
        locationPlugin.addOnIndicatorPositionChangedListener(onPositionChangedListener)
        MapboxNavigationApp.registerObserver(navigationObserver)
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarRouteLine carMapSurface detached $mapboxCarMapSurface")
        val mapSurface = mapboxCarMapSurface.mapSurface
        mapSurface.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        MapboxNavigationApp.unregisterObserver(navigationObserver)
        coroutineScope.cancel()
        style = null
    }

    private fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    private fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
    }

    private fun getMapboxRouteLineApiOptions(
        style: Style,
    ): MapboxRouteLineApiOptions {
        return MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .build()
    }

    private fun getMapboxRouteLineViewOptions(
        carContext: CarContext,
        style: Style,
    ): MapboxRouteLineViewOptions {
        return MapboxRouteLineViewOptions.Builder(carContext)
            .routeLineColorResources(routeLineColorResources)
            .routeLineBelowLayerId(findRoadLabelsLayerId(style))
            .build()
    }

    private fun findRoadLabelsLayerId(style: Style): String {
        return style.styleLayers
            .firstOrNull { layer -> layer.id.contains("road-label") }
            ?.id ?: "road-label-navigation"
    }

    private fun onRoutesChanged(style: Style, routes: List<NavigationRoute>) {
        logAndroidAuto("CarRouteLine onRoutesChanged ${routes.size}")
        if (routes.isNotEmpty()) {
            val routesMetadata = MapboxNavigationApp.current()
                ?.getAlternativeMetadataFor(routes).orEmpty()
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
