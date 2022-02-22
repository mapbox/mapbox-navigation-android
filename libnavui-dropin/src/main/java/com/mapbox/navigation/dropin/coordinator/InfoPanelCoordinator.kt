package com.mapbox.navigation.dropin.coordinator

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Guideline
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.infopanel.ActiveGuidanceInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.ArrivalInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.EmptyInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.FreeDriveInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.RoutePreviewInfoPanelBinder
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinator for navigation information.
 * This is also known as the bottom sheet.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelCoordinator(
    private val context: DropInNavigationViewContext,
    infoPanel: ViewGroup,
    private val guideBottom: Guideline
) : UICoordinator<ViewGroup>(infoPanel) {

    private val viewModel = context.viewModel

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val behavior = BottomSheetBehavior.from(viewGroup)
        behavior.addBottomSheetCallback(updateGuideline)
        behavior.isHideable = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        BottomSheetBehavior.from(viewGroup).removeBottomSheetCallback(updateGuideline)
    }

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return viewModel.navigationState.map { navigationState ->
            when (navigationState) {
                NavigationState.FreeDrive -> FreeDriveInfoPanelBinder(context)
                NavigationState.RoutePreview -> RoutePreviewInfoPanelBinder(context)
                NavigationState.ActiveNavigation -> ActiveGuidanceInfoPanelBinder(context)
                NavigationState.Arrival -> ArrivalInfoPanelBinder(context)
                else -> EmptyInfoPanelBinder()
            }
        }
    }

    private val updateGuideline = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            guideBottom.setGuidelineEnd(offsetBottom())
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            guideBottom.setGuidelineEnd(offsetBottom())
        }

        private fun offsetBottom() = (viewGroup.parent as ViewGroup).height - viewGroup.top
    }
}
