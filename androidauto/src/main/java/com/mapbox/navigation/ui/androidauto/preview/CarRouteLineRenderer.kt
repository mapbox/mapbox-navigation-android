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
                try {
                    routeLineView = MapboxRouteLineView(
                        options.routeLineViewOptionsProvider(mapboxCarMapSurface),
                    )
                    routeLineView.initializeLayers(style)
                    routeLineApi = MapboxRouteLineApi(
                        options.routeLineApiOptionsProvider(mapboxCarMapSurface),
                    )
                    routeArrowApi = options.routeArrowApiProvider(mapboxCarMapSurface)
                    routeArrowView = MapboxRouteArrowView(
                        options.routeArrowOptionsProvider(mapboxCarMapSurface),
                    )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    // A customization provider is third-party code (see CarRouteLineRendererOptions);
                    // a bug in it (for example force-unwrapping the nullable Style read off the
                    // surface) must not take down the whole Android Auto session.
                    logAndroidAutoFailure(
                        "CarRouteLine failed to build options from a customized provider",
                        exception,
                    )
                    return@collectLatest
                }
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
