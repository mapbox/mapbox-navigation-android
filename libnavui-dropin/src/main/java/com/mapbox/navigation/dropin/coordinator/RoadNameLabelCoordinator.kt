package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class RoadNameLabelCoordinator(
    navigationViewContext: DropInNavigationViewContext,
    roadNameLabelLayout: ViewGroup
) : UICoordinator<ViewGroup>(roadNameLabelLayout) {

    val stateFlow = MutableStateFlow(navigationViewContext.uiBinders.roadName)

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return stateFlow
    }
}
