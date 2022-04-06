package com.mapbox.navigation.qa_test_app.lifecycle.topbanner

import android.widget.Toast
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.dropin.internal.extensions.flowRouteProgress
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInManeuver(
    private val maneuverView: MapboxManeuverView
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(
                mapboxNavigation.navigationOptions.distanceFormatterOptions
            )
        )
        coroutineScope.launch {
            mapboxNavigation.flowRouteProgress().collect { routeProgress ->
                val maneuvers = maneuverApi.getManeuvers(routeProgress)
                maneuvers.fold(
                    { error ->
                        Toast.makeText(
                            maneuverView.context,
                            error.errorMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    {
                        maneuverView.renderManeuvers(maneuvers)
                    }
                )
            }
        }.invokeOnCompletion {
            maneuverApi.cancel()
        }
    }
}
