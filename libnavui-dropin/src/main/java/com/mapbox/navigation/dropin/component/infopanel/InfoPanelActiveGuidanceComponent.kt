package com.mapbox.navigation.dropin.component.infopanel

import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.usecase.guidance.StopActiveGuidanceUseCase
import kotlinx.coroutines.launch

internal class InfoPanelActiveGuidanceComponent(
    private val stopActiveGuidanceUseCase: StopActiveGuidanceUseCase,
    private val endNavigation: View,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        endNavigation.setOnClickListener {
            coroutineScope.launch {
                stopActiveGuidanceUseCase(Unit)
                mapboxNavigation.setRoutes(emptyList())
            }
        }
    }
}
