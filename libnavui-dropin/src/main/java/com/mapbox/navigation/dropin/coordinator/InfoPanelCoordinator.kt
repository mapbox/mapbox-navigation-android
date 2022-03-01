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
    private val guidelineBottom: Guideline
) : UICoordinator<ViewGroup>(infoPanel) {

    private val viewModel = context.viewModel
    private val behavior = BottomSheetBehavior.from(infoPanel)

    init {
        behavior.hide()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        behavior.addBottomSheetCallback(updateGuideline)
        viewModel.destination.observe { destination ->
            if (destination != null) behavior.collapse()
            else behavior.hide()
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        BottomSheetBehavior.from(viewGroup).removeBottomSheetCallback(updateGuideline)
    }

    // Content Binders
    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return viewModel.navigationState.map { navigationState ->
            when (navigationState) {
                NavigationState.FreeDrive -> FreeDriveInfoPanelBinder(context)
                NavigationState.RoutePreview -> RoutePreviewInfoPanelBinder(context)
                NavigationState.ActiveNavigation,
                NavigationState.Arrival -> ActiveGuidanceInfoPanelBinder(context)
                else -> EmptyInfoPanelBinder()
            }
        }
    }

    private fun <V : View> BottomSheetBehavior<V>.collapse() {
        state = BottomSheetBehavior.STATE_COLLAPSED
        isHideable = false
    }

    private fun <V : View> BottomSheetBehavior<V>.hide() {
        isHideable = true
        state = BottomSheetBehavior.STATE_HIDDEN
    }

    private val updateGuideline = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            guidelineBottom.setGuidelineEnd(offsetBottom())
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            guidelineBottom.setGuidelineEnd(offsetBottom())
        }

        private fun offsetBottom() = (viewGroup.parent as ViewGroup).height - viewGroup.top
    }
}
