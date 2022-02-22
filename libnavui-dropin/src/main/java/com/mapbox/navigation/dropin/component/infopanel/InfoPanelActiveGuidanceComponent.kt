package com.mapbox.navigation.dropin.component.infopanel

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.usecase.guidance.StopActiveGuidanceUseCase
import kotlinx.coroutines.launch

internal class InfoPanelActiveGuidanceComponent(
    private val stopActiveGuidanceUseCase: StopActiveGuidanceUseCase,
    private val infoPanel: View,
    private val endNavigation: View,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val behavior = BottomSheetBehavior.from(infoPanel)
        behavior.peekHeight = peekHeight()
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        behavior.isHideable = false

        endNavigation.setOnClickListener {
            coroutineScope.launch {
                stopActiveGuidanceUseCase(Unit)
                mapboxNavigation.setRoutes(emptyList())
            }
        }
    }

    private fun peekHeight() =
        infoPanel.resources.getDimensionPixelSize(R.dimen.mapbox_infoPanel_peekHeight)
}
