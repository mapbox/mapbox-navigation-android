package com.mapbox.navigation.dropin.coordinator

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Guideline
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelHeaderBinder
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Coordinator for navigation information.
 * This is also known as the bottom sheet.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelCoordinator(
    private val context: NavigationViewContext,
    private val infoPanel: ViewGroup,
    private val guidelineBottom: Guideline
) : UICoordinator<ViewGroup>(infoPanel) {
    private val store = context.store
    private val behavior = BottomSheetBehavior.from(infoPanel)

    init {
        behavior.hide()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        behavior.addBottomSheetCallback(updateGuideline)
        coroutineScope.launch {
            store.select { it.destination }.collect { destination ->
                if (destination != null) behavior.collapse()
                else behavior.hide()

                // When BottomSheet is already in requested state, BottomSheetCallback won't be
                // called leaving guideline in a wrong position.
                setGuidelinePosition(infoPanel)
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        behavior.removeBottomSheetCallback(updateGuideline)
    }

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return combine(
            context.uiBinders.infoPanelHeaderBinder,
            context.uiBinders.infoPanelContentBinder
        ) { headerBinder, contentBinder ->
            InfoPanelBinder(
                headerBinder ?: InfoPanelHeaderBinder(context),
                contentBinder
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

    private fun setGuidelinePosition(bottomSheet: View) {
        val offsetBottom = (bottomSheet.parent as ViewGroup).height - bottomSheet.top
        guidelineBottom.setGuidelineEnd(offsetBottom)
    }

    private val updateGuideline = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            setGuidelinePosition(bottomSheet)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            setGuidelinePosition(bottomSheet)
        }
    }
}
