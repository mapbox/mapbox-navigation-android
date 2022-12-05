package com.mapbox.navigation.qa_test_app.view.componentinstaller.components

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRouteProgress
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.guidance.restarea.api.MapboxRestAreaApi
import com.mapbox.navigation.ui.maps.guidance.restarea.view.MapboxRestAreaGuideMapView

@ExperimentalPreviewMapboxNavigationAPI
class RestAreaGuideMap(
    private val restAreaApi: MapboxRestAreaApi,
    private val restAreaView: MapboxRestAreaGuideMapView,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowRouteProgress().observe { routeProgress ->
            restAreaApi.generateUpcomingRestAreaGuideMap(routeProgress) {
                restAreaView.render(it)
            }
        }
        mapboxNavigation.flowRoutesUpdated().observe {
            if (it.navigationRoutes.isEmpty()) {
                restAreaApi.cancelAll()
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        restAreaApi.cancelAll()
    }
}
