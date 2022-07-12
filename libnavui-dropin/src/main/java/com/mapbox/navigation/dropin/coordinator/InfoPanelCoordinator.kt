package com.mapbox.navigation.dropin.coordinator

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelBinder
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelHeaderBinder
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
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
        infoPanel.addOnLayoutChangeListener(FixBottomSheetLayoutWhenHidden(infoPanel, behavior))
        behavior.peekHeight = context.styles.infoPanelPeekHeight.value
        behavior.hide()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        behavior.addBottomSheetCallback(updateGuideline)
        coroutineScope.launch {
            bottomSheetVisibility().collect { isVisible ->
                updateBottomSheetVisibility(isVisible)

                // When BottomSheet is already in requested state, BottomSheetCallback won't be
                // called leaving guideline in a wrong position.
                setGuidelinePosition(infoPanel)
            }
        }
        coroutineScope.launch {
            context.styles.infoPanelPeekHeight.collect { peekHeight ->
                behavior.peekHeight = peekHeight
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        behavior.removeBottomSheetCallback(updateGuideline)
    }

    override fun MapboxNavigation.flowViewBinders(): Flow<UIBinder> {
        return combine(
            context.uiBinders.infoPanelBinder,
            context.uiBinders.infoPanelHeaderBinder,
            context.uiBinders.infoPanelContentBinder
        ) { infoPanelBinder, headerBinder, contentBinder ->
            (infoPanelBinder ?: InfoPanelBinder.defaultBinder()).also {
                it.setNavigationViewContext(context)
                it.setBinders(
                    headerBinder ?: InfoPanelHeaderBinder(context),
                    contentBinder
                )
            }
        }
    }

    private fun bottomSheetVisibility() = combine(
        store.select { it.navigation },
        context.options.showInfoPanelInFreeDrive
    ) { navigationState, showInfoPanelInFreeDrive ->
        showInfoPanelInFreeDrive || navigationState != NavigationState.FreeDrive
    }

    private fun updateBottomSheetVisibility(isVisible: Boolean) {
        if (isVisible) behavior.collapse()
        else behavior.hide()
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

    /**
     * An OnLayoutChangeListener that ensures the bottom sheet is always laid out at the bottom of
     * the parent view when in STATE_HIDDEN.
     */
    private class FixBottomSheetLayoutWhenHidden(
        private val layout: ViewGroup,
        private val behavior: BottomSheetBehavior<ViewGroup>
    ) : View.OnLayoutChangeListener {

        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            if (behavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                ViewCompat.offsetTopAndBottom(layout, (layout.parent as? View)?.height ?: 0)
            }
        }
    }
}
