package com.mapbox.navigation.dropin.component.speedlimit

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.DropInComponent
import com.mapbox.navigation.dropin.lifecycle.flowLocationMatcherResult
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(InternalCoroutinesApi::class)
class DropInSpeedLimitComponent(val speedLimitView: MapboxSpeedLimitView) : DropInComponent() {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val speedLimitFormatter = SpeedLimitFormatter(speedLimitView.context)
        val speedLimitApi = MapboxSpeedLimitApi(speedLimitFormatter)

        coroutineScope.launch {
            mapboxNavigation.flowLocationMatcherResult().collect {
                val value = speedLimitApi.updateSpeedLimit(it.speedLimit)
                speedLimitView.render(value)
            }
        }
    }
}
