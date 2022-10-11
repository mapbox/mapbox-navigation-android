package com.mapbox.navigation.dropin.roadname

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.EmptyBinder
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Coordinator for showing current road name.
 */
internal class RoadNameCoordinator(
    private val context: NavigationViewContext,
    private val roadNameLayout: ViewGroup
) : UICoordinator<ViewGroup>(roadNameLayout) {

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
                    roadNameLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        startToStart = startConstraintId
                    }
                }
        }
    }

    private fun isOrientationLandscape() =
        roadNameLayout.resources.configuration.orientation == ORIENTATION_LANDSCAPE

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return combine(
            context.options.showRoadName,
            context.uiBinders.roadName
        ) { show, binder ->
            if (show) {
                binder ?: RoadNameViewBinder(context)
            } else {
                EmptyBinder()
            }
        }
    }
}
