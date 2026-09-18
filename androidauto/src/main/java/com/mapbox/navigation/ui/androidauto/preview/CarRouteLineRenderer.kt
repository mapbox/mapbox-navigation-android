package com.mapbox.navigation.ui.androidauto.preview

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
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.androidauto.routes.CarRoutesProvider
import com.mapbox.navigation.ui.androidauto.routes.NavigationCarRoutesProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import kotlinx.coroutines.CancellationException
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
 *
 * @param options customizes the route line and maneuver arrow rendering components, see
 * [CarRouteLineRendererOptions].
 */
class CarRouteLineRenderer(
    private val options: CarRouteLineRendererOptions = CarRouteLineRendererOptions.Builder()
        .build(),
    private val carRoutesProvider: CarRoutesProvider = NavigationCarRoutesProvider(),
) : MapboxCarMapObserver {

    private var routeLineResources: RouteLineResources? = null
    private var coroutineScope: CoroutineScope? = null

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val resources = routeLineResources ?: return@OnIndicatorPositionChangedListener
        val result = resources.routeLineApi.updateTraveledRouteLine(point)
        resources.routeLineView.renderRouteLineUpdate(resources.style, result)
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        val resources = routeLineResources ?: return@RouteProgressObserver
        resources.routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            resources.routeLineView.renderRouteLineUpdate(resources.style, result)
        }
        resources.routeArrowApi.addUpcomingManeuverArrow(routeProgress).also { arrowUpdate ->
            resources.routeArrowView.renderManeuverUpdate(resources.style, arrowUpdate)
        }
    }

    private val navigationObserver = mapboxNavigationForward(this::onAttached, this::onDetached)

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarRouteLine carMapSurface loaded $mapboxCarMapSurface")
        val coroutineScope = MainScope()
        this.coroutineScope = coroutineScope
        coroutineScope.launch {
            mapboxCarMapSurface.styleFlow().collectLatest { style ->
                clearRouteLineResources()
                val resources = try {
                    val routeLineView = MapboxRouteLineView(
                        options.routeLineViewOptionsProvider(mapboxCarMapSurface),
                    )
                    routeLineView.initializeLayers(style)
                    RouteLineResources(
                        style = style,
                        routeLineApi = MapboxRouteLineApi(
                            options.routeLineApiOptionsProvider(mapboxCarMapSurface),
                        ),
                        routeLineView = routeLineView,
                        routeArrowApi = options.routeArrowApiProvider(mapboxCarMapSurface),
                        routeArrowView = MapboxRouteArrowView(
                            options.routeArrowOptionsProvider(mapboxCarMapSurface),
                        ),
                    )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    logAndroidAutoFailure(
                        "CarRouteLine failed to build options from a customized provider",
                        exception,
                    )
                    return@collectLatest
                }
                this@CarRouteLineRenderer.routeLineResources = resources
                carRoutesProvider.navigationRoutes.collect { onRoutesChanged(resources, it) }
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
        coroutineScope?.cancel()
        coroutineScope = null
        clearRouteLineResources()
    }

    private fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
    }

    private fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
    }

    private fun clearRouteLineResources() {
        val resources = routeLineResources ?: return
        routeLineResources = null
        resources.routeLineApi.cancel()
        resources.routeLineView.cancel()
    }

    private fun onRoutesChanged(
        resources: RouteLineResources,
        routes: List<NavigationRoute>,
    ) {
        logAndroidAuto("CarRouteLine onRoutesChanged ${routes.size}")
        if (routes.isNotEmpty()) {
            val routesMetadata = MapboxNavigationApp.current()
                ?.getAlternativeMetadataFor(routes).orEmpty()
            resources.routeLineApi.setNavigationRoutes(routes, routesMetadata) { value ->
                resources.routeLineView.renderRouteDrawData(resources.style, value)
            }
        } else {
            resources.routeLineApi.clearRouteLine { value ->
                resources.routeLineView.renderClearRouteLineValue(resources.style, value)
            }
            val clearArrowValue = resources.routeArrowApi.clearArrows()
            resources.routeArrowView.render(resources.style, clearArrowValue)
        }
    }

    private data class RouteLineResources(
        val style: Style,
        val routeLineApi: MapboxRouteLineApi,
        val routeLineView: MapboxRouteLineView,
        val routeArrowApi: MapboxRouteArrowApi,
        val routeArrowView: MapboxRouteArrowView,
    )
}
