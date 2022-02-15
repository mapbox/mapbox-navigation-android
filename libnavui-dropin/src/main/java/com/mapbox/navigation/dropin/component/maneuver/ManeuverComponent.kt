package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.dropin.extensions.flowRouteProgress
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ManeuverComponent(val maneuverView: MapboxManeuverView) : UIComponent() {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val distanceFormatter = DistanceFormatterOptions
            .Builder(maneuverView.context)
            .build()
        val maneuverApi = MapboxManeuverApi(MapboxDistanceFormatter(distanceFormatter))

        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect {
                val value = maneuverApi.getManeuvers(it)
                maneuverView.renderManeuvers(value)
            }
        }
    }
}
