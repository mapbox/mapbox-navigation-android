package com.mapbox.navigation.qa_test_app.view.componentinstaller.components

import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.map

@ExperimentalPreviewMapboxNavigationAPI
class SimpleFollowingCamera(
    private val mapView: MapView
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowLocationMatcherResult()
            .map { it.enhancedLocation }
            .observe { location ->
                val mapAnimationOptionsBuilder = MapAnimationOptions.Builder()
                mapAnimationOptionsBuilder.duration(1500L)
                mapView.camera.easeTo(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(location.longitude, location.latitude))
                        .bearing(location.bearing.toDouble())
                        .pitch(45.0)
                        .zoom(17.0)
                        .padding(EdgeInsets(1000.0, 0.0, 0.0, 0.0))
                        .build(),
                    mapAnimationOptionsBuilder.build()
                )
            }
    }
}
