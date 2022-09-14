package com.mapbox.navigation.dropin.component.infopanel

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.internal.extensions.onClick
import com.mapbox.navigation.dropin.internal.extensions.tryUpdateStyle
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.fetchRouteAndStartActiveNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class StartNavigationButtonComponent(
    private val store: Store,
    private val button: MapboxExtendableButton,
    private val buttonStyle: StateFlow<Int>,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        buttonStyle.observe {
            button.tryUpdateStyle(it)
        }

        button.onClick(coroutineScope) {
            store.dispatch(fetchRouteAndStartActiveNavigation())
        }
    }
}
