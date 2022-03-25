package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.component.roadlabel.RoadNameViewBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoadNameLabelCoordinator(
    val context: DropInNavigationViewContext,
    roadNameLabelLayout: ViewGroup
) : UICoordinator<ViewGroup>(roadNameLabelLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return context.uiBinders.roadName.map {
            it ?: RoadNameViewBinder(context.mapStyleLoader.loadedMapStyle)
        }
    }
}
