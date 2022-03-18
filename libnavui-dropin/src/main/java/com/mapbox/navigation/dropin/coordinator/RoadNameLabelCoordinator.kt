package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.flowUiBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow

internal class RoadNameLabelCoordinator(
    val navigationViewContext: DropInNavigationViewContext,
    roadNameLabelLayout: ViewGroup
) : UICoordinator<ViewGroup>(roadNameLabelLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return navigationViewContext.flowUiBinder({ it.roadName })
    }
}
