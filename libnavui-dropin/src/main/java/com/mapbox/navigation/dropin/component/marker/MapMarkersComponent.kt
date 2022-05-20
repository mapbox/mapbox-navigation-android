package com.mapbox.navigation.dropin.component.marker

import androidx.annotation.DrawableRes
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.model.Store
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

/**
 * Component for rendering all drop-in UI map markers.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal open class MapMarkersComponent(
    private val store: Store,
    protected val mapView: MapView,
    @DrawableRes val iconImage: Int,
) : UIComponent() {

    private val mapMarkerFactory by lazy {
        MapMarkerFactory.create(mapView.context)
    }
    private var annotationManager = mapView.annotations.createPointAnnotationManager()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        store.select { it.destination?.point }
            .observe { point ->
                annotationManager.deleteAll()
                if (point != null) {
                    val annotation = mapMarkerFactory.createPin(point, iconImage)
                    annotationManager.create(annotation)
                }
            }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        annotationManager.deleteAll()
    }
}
