package com.mapbox.navigation.dropin.infopanel

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.extendablebutton.StartNavigationButtonComponent
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelStartNavigationButtonBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        return StartNavigationButtonComponent(
            context.store,
            viewGroup,
            context.styles.startNavigationButtonParams,
            context.routeOptionsProvider,
        )
    }
}
