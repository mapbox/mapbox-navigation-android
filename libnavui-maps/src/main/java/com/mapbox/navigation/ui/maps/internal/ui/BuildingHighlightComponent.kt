package com.mapbox.navigation.ui.maps.internal.ui

import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.internal.extensions.getDestination
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowOnFinalDestinationArrival
import com.mapbox.navigation.core.internal.extensions.flowOnNextRouteLegStart
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.building.api.MapboxBuildingsApi
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions
import com.mapbox.navigation.ui.maps.building.view.MapboxBuildingView
import com.mapbox.navigation.ui.maps.internal.extensions.queryBuildingToHighlight
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logW
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class BuildingHighlightComponent(
    private var map: MapboxMap,
    private var options: MapboxBuildingHighlightOptions,
    private var buildingsApi: MapboxBuildingsApi = MapboxBuildingsApi(map),
    private var buildingView: MapboxBuildingView = MapboxBuildingView(),
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowOnFinalDestinationArrival()
            .mapNotNull { it.navigationRoute.getDestination() }
            .mapLatest { it to buildingsApi.queryBuildingToHighlight(it).buildings }
            .catch { logApiError(it) }
            .observe { (destination, buildings) ->
                if (buildings.isNotEmpty()) {
                    map.getStyle()?.also { style ->
                        buildingView.highlightBuilding(style, buildings, options)
                    }
                } else {
                    logNoBuildingsWarning(destination)
                }
            }

        mapboxNavigation.flowOnNextRouteLegStart()
            .observe { removeBuildingHighlight() }

        mapboxNavigation.flowRoutesUpdated()
            .filter { it.navigationRoutes.isEmpty() }
            .observe { removeBuildingHighlight() }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        buildingsApi.cancel()
        removeBuildingHighlight()
        map.getStyle()?.also { buildingView.clear(it) }
    }

    private fun logApiError(e: Throwable) {
        logE("Error querying MapboxBuildingsApi: ${e.localizedMessage}", LOG_TAG)
    }

    private fun logNoBuildingsWarning(destination: Point) {
        logW("No buildings found to highlight at destination: $destination", LOG_TAG)
    }

    private fun removeBuildingHighlight() {
        map.getStyle()?.also { style ->
            buildingView.removeBuildingHighlight(style, options)
        }
    }

    private companion object {
        val LOG_TAG = this::class.simpleName
    }
}
