package com.mapbox.navigation.ui.speedlimit.internal

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedInfoApi
import com.mapbox.navigation.ui.speedlimit.model.MapboxSpeedInfoOptions
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedInfoView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SpeedInfoComponent(
    private val speedInfoView: MapboxSpeedInfoView,
    private val speedInfoOptions: MapboxSpeedInfoOptions,
    private val distanceFormatterOptions: DistanceFormatterOptions,
    private val speedInfoApi: MapboxSpeedInfoApi = MapboxSpeedInfoApi()
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        speedInfoView.applyOptions(speedInfoOptions)

        coroutineScope.launch {
            mapboxNavigation.flowLocationMatcherResult().collect { locationMatcher ->
                val value = speedInfoApi.updatePostedAndCurrentSpeed(
                    locationMatcher,
                    distanceFormatterOptions,
                )
                speedInfoView.render(value)
            }
        }
    }
}
