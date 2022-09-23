package com.mapbox.navigation.ui.maneuver.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.core.internal.extensions.flowTripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState
import com.mapbox.navigation.ui.utils.internal.Provider
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

interface ManeuverComponentContract {
    fun onManeuverViewStateChanged(state: MapboxManeuverViewState)
    fun onManeuverViewVisibilityChanged(isVisible: Boolean)
}

@ExperimentalPreviewMapboxNavigationAPI
class ManeuverComponent(
    val maneuverView: MapboxManeuverView,
    val userId: String?,
    val styleId: String?,
    val options: ManeuverViewOptions,
    private val formatterOptions: DistanceFormatterOptions,
    val contract: Provider<ManeuverComponentContract>? = null,
    val maneuverApi: MapboxManeuverApi = MapboxManeuverApi(
        MapboxDistanceFormatter(formatterOptions)
    )
) : UIComponent() {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        maneuverView.updateManeuverViewOptions(options)
        coroutineScope.launch {
            maneuverView.maneuverViewState.collect {
                contract?.get()?.onManeuverViewStateChanged(it)
            }
        }
        contract?.get()?.onManeuverViewVisibilityChanged(true)
        coroutineScope.launch {
            combine(
                mapboxNavigation.flowRoutesUpdated(),
                mapboxNavigation.flowRouteProgress(),
                mapboxNavigation.flowTripSessionState(),
            ) { routes, routeProgress, tripSessionState ->
                if (
                    routes.navigationRoutes.isNotEmpty() &&
                    tripSessionState == TripSessionState.STARTED
                ) {
                    val value = maneuverApi.getManeuvers(routeProgress)
                    maneuverView.renderManeuvers(value)

                    value.onValue { maneuvers ->
                        maneuverApi.getRoadShields(
                            userId,
                            styleId,
                            mapboxNavigation.navigationOptions.accessToken,
                            maneuvers
                        ) { result ->
                            maneuverView.renderManeuverWith(result)
                        }
                    }
                }
            }.collect()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        contract?.get()?.onManeuverViewVisibilityChanged(false)
    }
}
