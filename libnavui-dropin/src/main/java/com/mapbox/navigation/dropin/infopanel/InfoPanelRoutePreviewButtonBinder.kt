package com.mapbox.navigation.dropin.infopanel

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.extendablebutton.RoutePreviewButtonComponent
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.internal.extensions.updateMargins
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelRoutePreviewButtonBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        return reloadOnChange(context.styles.routePreviewButtonStyle) { style ->
            val button = MapboxExtendableButton(viewGroup.context, null, 0, style)
            viewGroup.removeAllViews()
            viewGroup.addView(button)
            button.updateMargins(
                right = button.resources.getDimensionPixelSize(R.dimen.mapbox_infoPanel_paddingEnd)
            )

            RoutePreviewButtonComponent(context.store, context.routeOptionsProvider, button)
        }
    }
}
