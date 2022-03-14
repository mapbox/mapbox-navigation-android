package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.dropin.extensions.flowRouteProgress
import com.mapbox.navigation.dropin.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.extensions.flowTripSessionState
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.NavigationStyles
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ManeuverComponent(
    val maneuverView: MapboxManeuverView,
    val maneuverApi: MapboxManeuverApi = MapboxManeuverApi(
        MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(maneuverView.context).build()
        )
    )
) : UIComponent() {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

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
                            NavigationStyles.NAVIGATION_DAY_STYLE_ID,
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
