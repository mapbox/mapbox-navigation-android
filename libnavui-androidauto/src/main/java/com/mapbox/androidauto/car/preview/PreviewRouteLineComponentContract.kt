package com.mapbox.androidauto.car.preview

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.maps.internal.RouteLineComponentContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PreviewRouteLineComponentContract(
    navigationRoutes: List<NavigationRoute>
) : RouteLineComponentContract {

    private val navigationRoutesStateFlow = MutableStateFlow(navigationRoutes)

    override val navigationRoutes: StateFlow<List<NavigationRoute>>
        get() {
            return navigationRoutesStateFlow
        }

    override fun setRoutes(routes: List<NavigationRoute>) {
        navigationRoutesStateFlow.value = routes
    }
}
