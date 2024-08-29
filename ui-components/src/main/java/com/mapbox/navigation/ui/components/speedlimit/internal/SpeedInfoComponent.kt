package com.mapbox.navigation.ui.components.speedlimit.internal

import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.tripdata.speedlimit.api.MapboxSpeedInfoApi
import com.mapbox.navigation.tripdata.speedlimit.model.SpeedInfoValue
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.components.speedlimit.model.MapboxSpeedInfoOptions
import com.mapbox.navigation.ui.components.speedlimit.view.MapboxSpeedInfoView
import com.mapbox.navigation.ui.utils.internal.Provider
import kotlinx.coroutines.launch

interface SpeedInfoComponentContract {
    fun onSpeedInfoClicked(speedInfo: SpeedInfoValue?)
}

internal class MapboxSpeedInfoComponentContract : SpeedInfoComponentContract {
    override fun onSpeedInfoClicked(speedInfo: SpeedInfoValue?) {
        // do nothing
    }
}

class SpeedInfoComponent(
    private val speedInfoView: MapboxSpeedInfoView,
    private val speedInfoOptions: MapboxSpeedInfoOptions,
    private val distanceFormatterOptions: DistanceFormatterOptions,
    private val speedInfoApi: MapboxSpeedInfoApi = MapboxSpeedInfoApi(),
    private val contractProvider: Provider<SpeedInfoComponentContract> = Provider {
        MapboxSpeedInfoComponentContract()
    },
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        speedInfoView.applyOptions(speedInfoOptions)

        speedInfoView.setOnClickListener {
            contractProvider.get().onSpeedInfoClicked(speedInfoView.speedInfo)
        }

        coroutineScope.launch {
            mapboxNavigation.flowLocationMatcherResult().collect { locationMatcher ->
                val value = speedInfoApi.updatePostedAndCurrentSpeed(
                    locationMatcher,
                    distanceFormatterOptions,
                )
                value?.let { speedInfoView.render(it) }
            }
        }
    }
}
