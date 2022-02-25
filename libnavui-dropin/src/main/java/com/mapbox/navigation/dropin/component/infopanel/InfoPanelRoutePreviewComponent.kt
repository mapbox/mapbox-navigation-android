package com.mapbox.navigation.dropin.component.infopanel

import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.usecase.guidance.StartActiveGuidanceUseCase
import kotlinx.coroutines.launch

internal class InfoPanelRoutePreviewComponent(
    private val startActiveGuidanceUseCase: StartActiveGuidanceUseCase,
    private val startButton: View
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        startButton.setOnClickListener {
            coroutineScope.launch {
                startActiveGuidanceUseCase(Unit)
            }
        }
    }
}
