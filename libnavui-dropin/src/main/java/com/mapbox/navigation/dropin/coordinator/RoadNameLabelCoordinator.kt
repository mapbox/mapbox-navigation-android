package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.roadlabel.RoadNameViewBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinator for showing current road name.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class RoadNameLabelCoordinator(
    val context: NavigationViewContext,
    roadNameLabelLayout: ViewGroup
) : UICoordinator<ViewGroup>(roadNameLabelLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return context.uiBinders.roadName.map {
            it ?: RoadNameViewBinder(context)
        }
    }
}
