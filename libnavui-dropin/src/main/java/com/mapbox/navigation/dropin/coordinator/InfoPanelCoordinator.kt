package com.mapbox.navigation.dropin.coordinator

import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewModel
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.infopanel.ActiveGuidanceInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.EmptyInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.FreeDriveInfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.RoutePreviewInfoPanelBinder
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.databinding.DropInNavigationViewBinding
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Provider

/**
 * Coordinator for navigation information.
 * This is also known as the bottom sheet.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelCoordinator @Inject constructor(
    private val viewModel: DropInNavigationViewModel,
    rootViewBinding: DropInNavigationViewBinding,
    private val freeDriveBinderProvider: Provider<FreeDriveInfoPanelBinder>,
    private val routePreviewBinderProvider: Provider<RoutePreviewInfoPanelBinder>,
    private val activeGuidanceBinderProvider: Provider<ActiveGuidanceInfoPanelBinder>,
) : UICoordinator<ViewGroup>(rootViewBinding.infoPanelLayout) {

    private val infoPanel = rootViewBinding.infoPanelLayout
    private val guidelineBottom = rootViewBinding.guidelineBottom
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
                NavigationState.FreeDrive -> freeDriveBinderProvider.get()
                NavigationState.RoutePreview -> routePreviewBinderProvider.get()
                NavigationState.ActiveNavigation,
                NavigationState.Arrival -> activeGuidanceBinderProvider.get()
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
