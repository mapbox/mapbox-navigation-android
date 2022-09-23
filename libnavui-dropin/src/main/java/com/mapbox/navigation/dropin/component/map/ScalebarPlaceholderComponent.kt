package com.mapbox.navigation.dropin.component.map

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxMapScalebarParams
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class ScalebarPlaceholderComponent(
    private val scalebarPlaceholder: View,
    private val scalebarParams: StateFlow<MapboxMapScalebarParams>,
    private val navigationState: StateFlow<State>,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            combine(scalebarParams, navigationState) { params, state ->
                scalebarPlaceholder.visibility =
                    if (params.enabled && state.navigation !is NavigationState.ActiveNavigation) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }.collect()
        }
    }
}
