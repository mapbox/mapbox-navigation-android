package com.mapbox.navigation.dropin.component.roadlabel

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.extensions.flowLocationMatcherResult
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoadNameComponent(private val roadNameView: MapboxRoadNameView) : UIComponent() {
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
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        roadNameView.visibility = View.INVISIBLE
    }
}
