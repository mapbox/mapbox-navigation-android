package com.mapbox.navigation.dropin.coordinator

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.roadlabel.RoadNameViewBinder
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
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

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        coroutineScope.launch {
            context.infoPanelBehavior.bottomSheetState
                .filterNotNull()
                .map { bottomSheetState ->
                    val isBottomSheetVisible = bottomSheetState != BottomSheetBehavior.STATE_HIDDEN
                    if (isOrientationLandscape() && isBottomSheetVisible) {
                        R.id.guidelineBegin
                    } else {
                        ConstraintLayout.LayoutParams.PARENT_ID
                    }
                }.collect { startConstraintId ->
                    roadNameLabelLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        startToStart = startConstraintId
                    }
                }
        }
    }

    private fun isOrientationLandscape() =
        roadNameLabelLayout.resources.configuration.orientation == ORIENTATION_LANDSCAPE

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return context.uiBinders.roadName.map {
            it ?: RoadNameViewBinder(context)
        }
    }
}
