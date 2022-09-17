package com.mapbox.navigation.dropin.coordinator

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Guideline
import androidx.core.graphics.Insets
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

    private val updateGuideline = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            context.infoPanelBehavior.updateBottomSheetState(newState)
            updateGuidelinePosition()
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            context.infoPanelBehavior.updateSlideOffset(slideOffset)
            updateGuidelinePosition()
        }
    }

    init {
        infoPanel.addOnLayoutChangeListener(FixBottomSheetLayoutWhenHidden(infoPanel, behavior))
        behavior.peekHeight = context.styles.infoPanelPeekHeight.value
        behavior.hide()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        behavior.addBottomSheetCallback(updateGuideline)
        coroutineScope.launch {
            bottomSheetState().collect { state ->
                val prevState = behavior.state
                when (state) {
                    BottomSheetBehavior.STATE_HIDDEN -> behavior.hide()
                    BottomSheetBehavior.STATE_COLLAPSED,
                    BottomSheetBehavior.STATE_HALF_EXPANDED,
                    BottomSheetBehavior.STATE_EXPANDED -> behavior.show(state)
                }
                resetSlideOffset(prevState, state)
                updateGuidelinePosition()
            }
        }
        coroutineScope.launch {
            context.systemBarsInsets.collect(this@InfoPanelCoordinator::updateGuidelinePosition)
        }
        coroutineScope.launch {
            context.options.isInfoPanelHideable.collect { hideable ->
                if (behavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                    // To avoid incorrect layout positioning, we only allow `behavior.isHideable`
                    // changes when BottomSheet is NOT in STATE_HIDDEN.
                    // NOTE: Setting `behavior.isHideable = false` when BottomSheet is in STATE_HIDDEN
                    // will force BottomSheet into STATE_COLLAPSED.
                    behavior.isHideable = hideable
                }
            }
        }
        coroutineScope.launch {
            bottomSheetPeekHeight().collect(behavior::setPeekHeight)
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
            context.uiBinders.infoPanelContentBinder,
            context.systemBarsInsets
        ) { infoPanelBinder, headerBinder, contentBinder, _ ->
            (infoPanelBinder ?: InfoPanelBinder.defaultBinder()).also {
                it.setNavigationViewContext(context)
                it.setBinders(
                    headerBinder ?: InfoPanelHeaderBinder(context),
                    contentBinder
                )
            }
        }
    }

    private fun bottomSheetState() = combine(
        context.uiBinders.infoPanelContentBinder,
        store.select { it.destination?.point },
        store.select { it.navigation },
        context.options.showInfoPanelInFreeDrive,
        context.options.infoPanelForcedState,
    ) { _, _, navigationState, showInfoPanelInFreeDrive, infoPanelForcedState ->
        if (infoPanelForcedState != 0) {
            infoPanelForcedState
        } else if (showInfoPanelInFreeDrive || navigationState != NavigationState.FreeDrive) {
            BottomSheetBehavior.STATE_COLLAPSED
        } else {
            BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun bottomSheetPeekHeight() = combine(
        context.styles.infoPanelPeekHeight,
        context.systemBarsInsets
    ) { peekHeight, insets ->
        peekHeight + insets.bottom
    }

    private fun <V : View> BottomSheetBehavior<V>.show(@BottomSheetBehavior.State state: Int) {
        this.state = state
        isHideable = context.options.isInfoPanelHideable.value
    }

    private fun <V : View> BottomSheetBehavior<V>.hide() {
        isHideable = true
        state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun updateGuidelinePosition(systemBarsInsets: Insets = context.systemBarsInsets.value) {
        val parentHeight = (infoPanel.parent as ViewGroup).height
        val maxPos = (parentHeight * GUIDELINE_MAX_POSITION_PERCENT).toInt()
        val pos = parentHeight - infoPanel.top - systemBarsInsets.bottom
        guidelineBottom.setGuidelineEnd(pos.coerceIn(0, maxPos))
    }

    private fun resetSlideOffset(prevBottomSheetState: Int, bottomSheetState: Int) {
        if (prevBottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
            when (bottomSheetState) {
                BottomSheetBehavior.STATE_EXPANDED -> 1.0f
                BottomSheetBehavior.STATE_HALF_EXPANDED -> 0.5f
                BottomSheetBehavior.STATE_COLLAPSED -> 0.0f
                BottomSheetBehavior.STATE_HIDDEN -> -1.0f
                else -> null
            }?.also { slideOffset ->
                context.infoPanelBehavior.updateSlideOffset(slideOffset)
            }
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

    private companion object {
        /**
         * Guideline bottom maximum position within its parent view.
         * This value must be within 0.0f..1.0f range.
         */
        const val GUIDELINE_MAX_POSITION_PERCENT = 0.3f
    }
}
