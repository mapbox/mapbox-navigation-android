package com.mapbox.navigation.ui.components.maps.internal.ui

import androidx.core.view.isVisible
import com.mapbox.bindgen.Expected
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.coroutine.mapLoadedEvents
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.tripdata.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.tripdata.shield.api.ShieldFontConfig
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.components.maps.roadname.view.MapboxRoadNameView
import com.mapbox.navigation.ui.maps.internal.extensions.getStyleId
import com.mapbox.navigation.ui.maps.internal.extensions.getUserId
import com.mapbox.navigation.ui.utils.internal.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface RoadNameComponentContract {
    val roadInfo: StateFlow<Road?>
    val mapStyle: StateFlow<Style?>
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoadNameComponent(
    private val roadNameView: MapboxRoadNameView,
    private val contractProvider: Provider<RoadNameComponentContract>,
    private val routeShieldApi: MapboxRouteShieldApi = MapboxRouteShieldApi(),
    private val shieldFontConfig: ShieldFontConfig? = null,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val contract = contractProvider.get()

        combine(
            contract.mapStyle,
            contract.roadInfo,
        ) { mapStyle, roadInfo ->
            mapStyle to roadInfo
        }.observe { (mapStyle, road) ->
            if (mapStyle != null && road != null && road.isRoadNameAvailable()) {
                roadNameView.isVisible = true

                roadNameView.renderRoadName(road)

                val result = routeShieldApi.getRouteShields(
                    road,
                    mapStyle.getUserId(),
                    mapStyle.getStyleId(),
                    shieldFontConfig,
                )
                roadNameView.renderRoadNameWith(result)
            } else {
                roadNameView.isVisible = false
            }
        }
    }

    private fun Road.isRoadNameAvailable() = components.isNotEmpty()

    private suspend fun MapboxRouteShieldApi.getRouteShields(
        road: Road,
        userId: String?,
        styleId: String?,
        fontConfig: ShieldFontConfig?,
    ): List<Expected<RouteShieldError, RouteShieldResult>> =
        suspendCancellableCoroutine { cont ->
            getRouteShields(road, userId, styleId, fontConfig) { result ->
                cont.resume(result)
            }
            cont.invokeOnCancellation {
                // this cancel call will cancel any job invoked through other APIs
                cancel()
            }
        }
}

internal class MapboxRoadNameComponentContract(
    private val map: MapboxMap,
) : UIComponent(),
    RoadNameComponentContract {

    private var _roadInfo = MutableStateFlow<Road?>(null)
    override val roadInfo: StateFlow<Road?> = _roadInfo.asStateFlow()

    private var _mapStyle = MutableStateFlow<Style?>(null)
    override val mapStyle: StateFlow<Style?> = _mapStyle.asStateFlow()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowLocationMatcherResult().observe {
            _roadInfo.value = it.road
        }

        map.mapLoadedEvents.observe {
            map.getStyle { _mapStyle.value = it }
        }
    }
}
