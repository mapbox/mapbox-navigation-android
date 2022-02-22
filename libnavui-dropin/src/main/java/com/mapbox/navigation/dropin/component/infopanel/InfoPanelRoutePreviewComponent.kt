package com.mapbox.navigation.dropin.component.infopanel

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.usecase.guidance.StartActiveGuidanceUseCase
import kotlinx.coroutines.launch

internal class InfoPanelRoutePreviewComponent(
    private val startActiveGuidanceUseCase: StartActiveGuidanceUseCase,
    private val infoPanel: View,
    private val startButton: View
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val behavior = BottomSheetBehavior.from(infoPanel)
        behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        behavior.isHideable = false

        startButton.setOnClickListener {
            coroutineScope.launch {
                startActiveGuidanceUseCase(Unit)
            }
        }
    }
}
