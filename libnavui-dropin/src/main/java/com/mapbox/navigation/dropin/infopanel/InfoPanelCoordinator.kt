package com.mapbox.navigation.dropin.infopanel

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Coordinator for navigation information.
 * This is also known as the bottom sheet.
 */
internal class InfoPanelCoordinator(
    private val context: NavigationViewContext,
    private val binding: MapboxNavigationViewLayoutBinding,
) : UICoordinator<ViewGroup>(binding.infoPanelLayout) {
    private val store = context.store
    private val behavior = BottomSheetBehavior.from(binding.infoPanelLayout)

    private val updateGuideline = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            context.behavior.infoPanelBehavior.updateBottomSheetState(newState)
            updateGuidelinePosition()
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            context.behavior.infoPanelBehavior.updateSlideOffset(slideOffset)
            updateGuidelinePosition()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val infoPanelTop = callbackFlow {
        val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            trySend(binding.infoPanelLayout.top)
        }
        val viewTreeObserver = binding.infoPanelLayout.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
        awaitClose { viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener) }
    }.distinctUntilChanged()

    init {
        binding.infoPanelLayout.addOnLayoutChangeListener(FixBottomSheetLayoutWhenHidden())
        behavior.peekHeight = context.styles.infoPanelPeekHeight.value
        behavior.hide()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        context.behavior.infoPanelBehavior.updateBottomSheetState(behavior.state)
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
            context.systemBarsInsets.collect { insets ->
                binding.container.setPadding(insets.left, insets.top, insets.right, insets.bottom)
                updateGuidelinePosition(systemBarsInsets = insets)
            }
        }
        coroutineScope.launch {
            context.styles.infoPanelGuidelineMaxPosPercent.collect {
                updateGuidelinePosition(maxPosPercent = it)
            }
        }
        coroutineScope.launch {
            infoPanelTop.collect { updateGuidelinePosition(infoPanelTop = it) }
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
            bottomSheetPeekHeight().collect { behavior.peekHeight = it }
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

    private fun updateGuidelinePosition(
        systemBarsInsets: Insets = context.systemBarsInsets.value,
        infoPanelTop: Int = binding.infoPanelLayout.top,
        maxPosPercent: Float = context.styles.infoPanelGuidelineMaxPosPercent.value
    ) {
        val parentHeight = binding.coordinatorLayout.height
        val maxPos = (parentHeight * maxPosPercent).toInt() - systemBarsInsets.bottom
        if (0 < maxPos) {
            val pos = parentHeight - infoPanelTop - systemBarsInsets.bottom
            binding.guidelineBottom.setGuidelineEnd(pos.coerceIn(0, maxPos))
        }
    }

    private fun resetSlideOffset(prevBottomSheetState: Int, bottomSheetState: Int) {
        if (prevBottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
            // BottomSheet slideOffset value is always in [-1,1] range.
            // From -1.0 when hidden, 0.0 when collapsed to 1.0 when expanded.
            when (bottomSheetState) {
                BottomSheetBehavior.STATE_EXPANDED -> 1.0f
                BottomSheetBehavior.STATE_HALF_EXPANDED -> 0.5f
                BottomSheetBehavior.STATE_COLLAPSED -> 0.0f
                BottomSheetBehavior.STATE_HIDDEN -> -1.0f
                else -> null
            }?.also { slideOffset ->
                context.behavior.infoPanelBehavior.updateSlideOffset(slideOffset)
            }
        }
    }

    /**
     * An OnLayoutChangeListener that ensures the bottom sheet is always laid out at the bottom of
     * the parent view when in STATE_HIDDEN.
     */
    private inner class FixBottomSheetLayoutWhenHidden : View.OnLayoutChangeListener {

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
                ViewCompat.offsetTopAndBottom(
                    binding.infoPanelLayout,
                    binding.coordinatorLayout.height,
                )
            }
        }
    }
}
