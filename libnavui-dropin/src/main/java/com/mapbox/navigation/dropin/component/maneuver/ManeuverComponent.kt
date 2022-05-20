package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.maps.Style
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
import com.mapbox.navigation.ui.maps.internal.extensions.getStyleId
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class ManeuverComponent(
    val maneuverView: MapboxManeuverView,
    val mapStyle: Style,
    val options: ManeuverViewOptions,
    val maneuverApi: MapboxManeuverApi = MapboxManeuverApi(
        MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(maneuverView.context).build()
        )
    )
) : UIComponent() {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        maneuverView.updateManeuverViewOptions(options)
        maneuverView.upcomingManeuverRenderingEnabled = false
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
                            DirectionsCriteria.PROFILE_DEFAULT_USER,
                            mapStyle.getStyleId(),
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
}
