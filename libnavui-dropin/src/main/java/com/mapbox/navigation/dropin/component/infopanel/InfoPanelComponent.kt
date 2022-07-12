package com.mapbox.navigation.dropin.component.infopanel

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.combine

@ExperimentalPreviewMapboxNavigationAPI
class InfoPanelComponent internal constructor(
    private val layout: ViewGroup,
    private val context: NavigationViewContext
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        stylesFlow().observe { (background, marginLeft, marginRight) ->
            layout.layoutParams = (layout.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                setMargins(marginLeft, topMargin, marginRight, bottomMargin)
            }
            layout.setBackgroundResource(background)
        }
    }

    private fun stylesFlow() = context.styles.let { styles ->
        combine(
            styles.infoPanelBackground,
            styles.infoPanelMarginStart,
            styles.infoPanelMarginEnd
        ) { background, marginStart, marginEnd -> Triple(background, marginStart, marginEnd) }
    }
}
