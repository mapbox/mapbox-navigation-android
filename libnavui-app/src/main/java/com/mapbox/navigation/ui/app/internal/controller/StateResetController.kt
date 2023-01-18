package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

internal class StateResetController(
    private val store: Store
) : UIComponent() {
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)

        // we reset Store state every time MapboxNavigation gets destroyed
        store.reset()
    }
}
