package com.mapbox.navigation.dropin.component.marker

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.flow.map

/**
 * Component for rendering all drop-in UI map markers.
 */
internal open class MapMarkersComponent(
    protected val mapView: MapView,
    protected val context: DropInNavigationViewContext
) : UIComponent() {
    private val mapAnnotationFactory = context.mapAnnotationFactory()
    private val routesState = context.routesState
    private var annotationManager = mapView.annotations.createPointAnnotationManager()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        routesState.map { it.destination }.observe { destination ->
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
