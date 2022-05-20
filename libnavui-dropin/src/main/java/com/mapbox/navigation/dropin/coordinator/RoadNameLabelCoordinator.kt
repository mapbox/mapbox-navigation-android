package com.mapbox.navigation.dropin.coordinator

import android.content.res.Configuration
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.roadlabel.RoadNameViewBinder
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Coordinator for showing current road name.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class RoadNameLabelCoordinator(
    val context: NavigationViewContext,
    val roadNameLabelLayout: ViewGroup
) : UICoordinator<ViewGroup>(roadNameLabelLayout) {

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        coroutineScope.launch {
            context.viewModel.store.select { it.navigation }.collect { navigationState ->
                when (roadNameLabelLayout.resources.configuration.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        adjustLandscapeConstraints(navigationState)
                    }
                    else -> {
                        roadNameLabelLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                            startToStart = R.id.container
                        }
                    }
                }
            }
        }

        return context.uiBinders.roadName.map {
            it ?: RoadNameViewBinder(context)
        }
    }

    private fun adjustLandscapeConstraints(navigationState: NavigationState) {
        when (navigationState) {
            NavigationState.FreeDrive -> {
                roadNameLabelLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    startToStart = R.id.container
                }
            }
            NavigationState.DestinationPreview,
            NavigationState.RoutePreview,
            NavigationState.ActiveNavigation,
            NavigationState.Arrival -> {
                roadNameLabelLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    startToStart = R.id.guidelineBegin
                }
            }
        }
    }
}
