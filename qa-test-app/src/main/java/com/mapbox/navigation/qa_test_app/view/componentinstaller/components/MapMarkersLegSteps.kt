package com.mapbox.navigation.qa_test_app.view.componentinstaller.components

import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.qa_test_app.view.util.IconFactory
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

@ExperimentalPreviewMapboxNavigationAPI
class MapMarkersLegSteps(
    private val mapView: MapView
) : UIComponent() {

    private var annotationManager = mapView.annotations.createPointAnnotationManager()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val annotation = PointAnnotationOptions()
            .withIconAnchor(IconAnchor.BOTTOM)
        val iconFactory = IconFactory(mapView.context)

        mapboxNavigation.flowRoutesUpdated()
            .onEach { annotationManager.deleteAll() }
            .mapNotNull { it.navigationRoutes.firstOrNull()?.directionsRoute?.legs() }
            .observe { legs ->
                legs.forEachIndexed { legIndex, routeLeg ->
                    routeLeg.steps()?.forEachIndexed { stepIndex, legStep ->
                        legStep.intersections()?.firstOrNull()?.also {
                            val icon = iconFactory.pinIconWithText("$legIndex.$stepIndex")
                            annotationManager.create(
                                annotation
                                    .withIconImage(icon)
                                    .withPoint(it.location())
                            )
                        }
                    }
                }
            }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        annotationManager.deleteAll()
    }
}
