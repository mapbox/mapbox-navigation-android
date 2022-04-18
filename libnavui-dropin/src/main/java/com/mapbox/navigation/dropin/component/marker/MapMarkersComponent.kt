package com.mapbox.navigation.dropin.component.marker

import androidx.annotation.DrawableRes
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Component for rendering all drop-in UI map markers.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal open class MapMarkersComponent(
    protected val mapView: MapView,
    @DrawableRes val iconImage: Int,
    protected val context: NavigationViewContext
) : UIComponent() {
    private val mapAnnotationFactory = context.mapAnnotationFactory()
    private val destinationState = context.destinationState
    private var annotationManager = mapView.annotations.createPointAnnotationManager()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        destinationState
            .map { it.destination?.point }
            .distinctUntilChanged()
            .observe { point ->
                annotationManager.deleteAll()
                if (point != null) {
                    val annotation = mapAnnotationFactory.createPin(point, iconImage)
                    annotationManager.create(annotation)
                }
            }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        annotationManager.deleteAll()
    }
}
