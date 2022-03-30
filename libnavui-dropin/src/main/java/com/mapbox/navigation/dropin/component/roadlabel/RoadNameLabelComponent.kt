package com.mapbox.navigation.dropin.component.roadlabel

import android.view.View
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.extensions.flowLocationMatcherResult
import com.mapbox.navigation.dropin.extensions.getStyleId
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoadNameLabelComponent(
    private val roadNameView: MapboxRoadNameView,
    private val mapStyle: Style,
    private val routeShieldApi: MapboxRouteShieldApi = MapboxRouteShieldApi()
) : UIComponent() {
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        coroutineScope.launch {
            mapboxNavigation.flowLocationMatcherResult().collect { locationMatcherResult ->
                // todo there is probably some other state data that needs to be monitored to
                // determine the visibility of this view
                if (roadNameView.visibility != View.VISIBLE) {
                    roadNameView.visibility = View.VISIBLE
                }
                roadNameView.renderRoadName(locationMatcherResult.road)

                routeShieldApi.getRouteShields(
                    locationMatcherResult.road,
                    DirectionsCriteria.PROFILE_DEFAULT_USER,
                    mapStyle.getStyleId(),
                    mapboxNavigation.navigationOptions.accessToken,
                ) { result ->
                    roadNameView.renderRoadNameWith(result)
                }
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        roadNameView.visibility = View.INVISIBLE
    }
}
