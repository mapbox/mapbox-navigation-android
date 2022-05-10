package com.mapbox.androidauto.car.preview

import com.mapbox.androidauto.car.MainCarContext
import com.mapbox.androidauto.car.routes.NavigationRoutesProvider
import com.mapbox.androidauto.car.routes.RoutesListener
import com.mapbox.androidauto.car.routes.RoutesProvider
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapObserver
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources

/**
 * This class is to simplify the interaction with [MapboxRouteLineApi], [MapboxRouteArrowView]
 * [MapboxRouteArrowApi], and [RouteProgressObserver] use cases that the app needs in the car.
 *
 * Anything for rendering the car's route line, is handled here at this point.
 */
@OptIn(MapboxExperimental::class)
class CarRouteLine internal constructor(
    val mainCarContext: MainCarContext,
    private val routesProvider: RoutesProvider,
) : MapboxCarMapObserver {

    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder().build()
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .build()
    }

    private lateinit var routeLineView: MapboxRouteLineView
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeArrowApi: MapboxRouteArrowApi
    private lateinit var routeArrowView: MapboxRouteArrowView

    private val onPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val result = routeLineApi.updateTraveledRouteLine(point)
        val mapboxMap = mainCarContext.mapboxCarMap.carMapSurface?.mapSurface?.getMapboxMap()
        mapboxMap?.getStyle()?.let { style ->
            routeLineView.renderRouteLineUpdate(style, result)
        }
    }

    private val routesListener = RoutesListener { routes ->
        logAndroidAuto("CarRouteLine onRoutesChanged ${routes.size}")
        mainCarContext.mapboxCarMap.carMapSurface?.mapSurface?.getMapboxMap()?.getStyle { style ->
            if (routes.isNotEmpty()) {
                val routeLines = routes.map { NavigationRouteLine(it, identifier = null) }
                routeLineApi.setNavigationRouteLines(routeLines) { value ->
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

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        mainCarContext.mapboxCarMap.carMapSurface?.mapSurface?.getMapboxMap()?.getStyle { style ->
            routeLineApi.updateWithRouteProgress(routeProgress) { result ->
                routeLineView.renderRouteLineUpdate(style, result)
            }
            routeArrowApi.addUpcomingManeuverArrow(routeProgress).also { arrowUpdate ->
                routeArrowView.renderManeuverUpdate(style, arrowUpdate)
            }
        }
    }

    constructor(
        mainCarContext: MainCarContext,
    ) : this(mainCarContext, NavigationRoutesProvider(mainCarContext.mapboxNavigation))

    override fun onAttached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarRouteLine carMapSurface loaded $mapboxCarMapSurface")
        val locationPlugin = mapboxCarMapSurface.mapSurface.location
        mainCarContext.mapboxCarMap.carMapSurface?.mapSurface?.getMapboxMap()?.getStyle { style ->
            val routeLineOptions = getMapboxRouteLineOptions(mapboxCarMapSurface)
            routeLineView = MapboxRouteLineView(routeLineOptions)
            routeLineApi = MapboxRouteLineApi(routeLineOptions).also {
                routeLineView.initializeLayers(style)
            }
            routeArrowApi = MapboxRouteArrowApi()
            routeArrowView = MapboxRouteArrowView(
                RouteArrowOptions.Builder(mainCarContext.carContext)
                    .withAboveLayerId(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
                    .build()
            )

            locationPlugin.addOnIndicatorPositionChangedListener(onPositionChangedListener)
            mainCarContext.mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            routesProvider.registerRoutesListener(routesListener)
        }
    }

    override fun onDetached(mapboxCarMapSurface: MapboxCarMapSurface) {
        logAndroidAuto("CarRouteLine carMapSurface detached $mapboxCarMapSurface")
        val mapSurface = mapboxCarMapSurface.mapSurface
        mapSurface.location.removeOnIndicatorPositionChangedListener(onPositionChangedListener)
        mainCarContext.mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        routesProvider.unregisterRoutesListener(routesListener)
    }

    private fun getMapboxRouteLineOptions(
        mapboxCarMapSurface: MapboxCarMapSurface
    ): MapboxRouteLineOptions {
        return MapboxRouteLineOptions.Builder(mainCarContext.carContext)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId(findRoadLabelsLayerId(mapboxCarMapSurface))
            .withVanishingRouteLineEnabled(true)
            .build()
    }

    private fun findRoadLabelsLayerId(mapboxCarMapSurface: MapboxCarMapSurface): String {
        return mapboxCarMapSurface.mapSurface.getMapboxMap().getStyle()?.styleLayers
            ?.firstOrNull { layer -> layer.id.contains("road-label") }
            ?.id ?: "road-label-navigation"
    }
}
