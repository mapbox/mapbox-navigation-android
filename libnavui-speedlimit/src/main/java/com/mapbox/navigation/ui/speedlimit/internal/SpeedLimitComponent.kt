package com.mapbox.navigation.ui.speedlimit.internal

import androidx.annotation.StyleRes
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.SpeedLimitFormatter
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SpeedLimitComponent(
    @StyleRes val style: Int,
    @StyleRes val textAppearance: Int,
    val speedLimitView: MapboxSpeedLimitView,
    val speedLimitApi: MapboxSpeedLimitApi = MapboxSpeedLimitApi(
        SpeedLimitFormatter(speedLimitView.context)
    )
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        speedLimitView.updateStyle(style)
        // setTextAppearance is not deprecated in AppCompatTextView
        speedLimitView.setTextAppearance(speedLimitView.context, textAppearance)

        coroutineScope.launch {
            mapboxNavigation.flowLocationMatcherResult().collect {
                val value = speedLimitApi.updateSpeedLimit(it.speedLimit)
                speedLimitView.render(value)
            }
        }
    }
}
