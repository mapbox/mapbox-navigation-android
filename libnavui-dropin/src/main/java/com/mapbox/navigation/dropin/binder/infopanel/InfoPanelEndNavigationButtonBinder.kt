package com.mapbox.navigation.dropin.binder.infopanel

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.component.infopanel.EndNavigationButtonComponent
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

@ExperimentalPreviewMapboxNavigationAPI
internal class InfoPanelEndNavigationButtonBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        return EndNavigationButtonComponent(
            context.store,
            viewGroup,
            context.styles.endNavigationButtonParams
        )
    }
}
