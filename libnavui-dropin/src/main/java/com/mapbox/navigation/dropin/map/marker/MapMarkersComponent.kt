package com.mapbox.navigation.dropin.map.marker

import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

/**
 * Component for rendering all drop-in UI map markers.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal open class MapMarkersComponent(
    private val store: Store,
    protected val mapView: MapView,
    private val markerAnnotationOptions: PointAnnotationOptions,
) : UIComponent() {

    private var annotationManager = mapView.annotations.createPointAnnotationManager()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        store.select { it.destination?.point }
            .observe { point ->
                annotationManager.deleteAll()
                if (point != null) {
                    annotationManager.create(
                        markerAnnotationOptions.apply {
                            withPoint(point)
                        }
                    )
                }
            }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        annotationManager.deleteAll()
    }
}
