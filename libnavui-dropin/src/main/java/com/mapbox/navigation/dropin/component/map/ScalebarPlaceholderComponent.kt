package com.mapbox.navigation.dropin.component.map

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxMapScalebarParams
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class ScalebarPlaceholderComponent(
    private val scalebarPlaceholder: View,
    private val scalebarParams: StateFlow<MapboxMapScalebarParams>,
    private val maneuverHeight: StateFlow<Int>,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            combine(scalebarParams, maneuverHeight) { params, height ->
                scalebarPlaceholder.visibility =
                    if (params.enabled && height == 0) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }.collect()
        }
    }
}
