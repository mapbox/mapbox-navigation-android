package com.mapbox.navigation.dropin.infopanel

import android.view.ViewGroup
import androidx.core.view.updatePadding
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.dropin.internal.extensions.updateMargins
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.combine

internal class InfoPanelComponent(
    private val layout: ViewGroup,
    private val context: NavigationViewContext
) : UIComponent() {

    private companion object {
        const val APPLY_TOP_PADDING_THRESHOLD = 0.8f
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val navigationView = findNavigationView()
        combine(
            context.behavior.infoPanelBehavior.slideOffset,
            context.systemBarsInsets
        ) { slideOffset, insets ->
            slideOffset to insets
        }.observe { (slideOffset, insets) ->
            val isFullHeightLayout = layout.height == navigationView?.height
            if (isFullHeightLayout && APPLY_TOP_PADDING_THRESHOLD < slideOffset) {
                val f = 1.0f - (1.0f - slideOffset) / (1.0f - APPLY_TOP_PADDING_THRESHOLD)
                val top = insets.top * f
                layout.updatePadding(top = top.toInt(), bottom = insets.bottom)
            } else {
                layout.updatePadding(top = 0, bottom = insets.bottom)
            }
        }

        context.styles.infoPanelBackground.observe { layout.setBackgroundResource(it) }
        context.styles.infoPanelMarginStart.observe { layout.updateMargins(left = it) }
        context.styles.infoPanelMarginEnd.observe { layout.updateMargins(right = it) }

        val parent = layout.parent as ViewGroup
        context.systemBarsInsets.observe { parent.updateMargins(left = it.left, right = it.right) }
    }

    private fun findNavigationView(): NavigationView? {
        var v = layout.parent
        while (v != null && v !is NavigationView) {
            v = v.parent
        }
        return v as? NavigationView
    }
}
