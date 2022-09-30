package com.mapbox.navigation.dropin.extendablebutton

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxExtendableButtonParams
import com.mapbox.navigation.dropin.internal.extensions.onClick
import com.mapbox.navigation.dropin.internal.extensions.recreateButton
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.extension.dispatch
import com.mapbox.navigation.ui.app.internal.fetchRouteAndStartActiveNavigation
import com.mapbox.navigation.ui.app.internal.routefetch.RouteOptionsProvider
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class StartNavigationButtonComponent(
    private val store: Store,
    private val buttonContainer: ViewGroup,
    private val buttonParams: StateFlow<MapboxExtendableButtonParams>,
    private val routeOptionsProvider: RouteOptionsProvider,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        buttonParams.observe { params ->
            buttonContainer.recreateButton(params).let {
                it.onClick(coroutineScope) {
                    store.dispatch(
                        fetchRouteAndStartActiveNavigation(routeOptionsProvider, mapboxNavigation),
                    )
                }
            }
        }
    }
}
