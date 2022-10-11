package com.mapbox.navigation.dropin.map.scalebar

import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class ScalebarPlaceholderComponent(
    private val scalebarPlaceholder: View,
    private val isEnabled: StateFlow<Boolean>,
    private val maneuverViewVisible: StateFlow<Boolean>,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            combine(isEnabled, maneuverViewVisible) { enabled, maneuverViewVisible ->
                scalebarPlaceholder.visibility =
                    if (enabled && !maneuverViewVisible) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }.collect()
        }
    }
}
