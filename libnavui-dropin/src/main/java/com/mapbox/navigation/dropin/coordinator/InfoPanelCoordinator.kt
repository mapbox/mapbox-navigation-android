package com.mapbox.navigation.dropin.coordinator

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Guideline
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelHeaderBinder
import com.mapbox.navigation.dropin.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

    private val routesState = context.routesState
    private val behavior = BottomSheetBehavior.from(infoPanel)

    init {
        behavior.hide()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        behavior.addBottomSheetCallback(updateGuideline)
        coroutineScope.launch {
            routesState.map { it.destination }.collect { destination ->
                if (destination != null) behavior.collapse()
                else behavior.hide()
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        behavior.removeBottomSheetCallback(updateGuideline)
    }

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return context.uiBinders.map { uiBinders ->
            InfoPanelBinder(
                uiBinders.infoPanelHeaderBinder ?: InfoPanelHeaderBinder(context),
                uiBinders.infoPanelContentBinder
            )
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
            guidelineBottom.setGuidelineEnd(offsetBottom(bottomSheet))
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            guidelineBottom.setGuidelineEnd(offsetBottom(bottomSheet))
        }

        private fun offsetBottom(v: View) = (v.parent as ViewGroup).height - v.top
    }
}
