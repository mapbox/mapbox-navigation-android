package com.mapbox.navigation.dropin.extendablebutton

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.internal.extensions.onClick
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction.FetchRouteAndShowRoutePreview
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton

internal class RoutePreviewButtonComponent(
    private val store: Store,
    private val button: MapboxExtendableButton
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        button.onClick(coroutineScope) {
            store.dispatch(FetchRouteAndShowRoutePreview)
        }
    }
}
