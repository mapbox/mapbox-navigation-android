package com.mapbox.navigation.dropin.component.marker

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewModel
import com.mapbox.navigation.dropin.lifecycle.UIComponent

/**
 * Component for rendering all drop-in UI map markers.
 */
internal open class MapMarkersComponent(
    protected val mapView: MapView,
    protected val mapAnnotationFactory: MapMarkerFactory,
    protected val viewModel: DropInNavigationViewModel
) : UIComponent() {
    private var annotationManager = mapView.annotations.createPointAnnotationManager()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        viewModel.destination.observe { destination ->
            annotationManager.deleteAll()
            if (destination != null) {
                val annotation = mapAnnotationFactory.createPin(destination.point)
                annotationManager.create(annotation)
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        annotationManager.deleteAll()
    }
}
