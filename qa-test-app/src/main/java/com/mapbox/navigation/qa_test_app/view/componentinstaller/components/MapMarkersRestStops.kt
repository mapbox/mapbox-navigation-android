package com.mapbox.navigation.qa_test_app.view.componentinstaller.components

import android.graphics.Color
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.qa_test_app.view.util.IconFactory
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

@ExperimentalPreviewMapboxNavigationAPI
class MapMarkersRestStops(
    private val mapView: MapView
) : UIComponent() {

    private var annotationManager = mapView.annotations.createPointAnnotationManager()

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val icon = IconFactory(mapView.context).pinIconWithText(
            "zzZ",
            Color.parseColor("#00ccff")
        )
        val annotation = PointAnnotationOptions()
            .withIconImage(icon)
            .withIconAnchor(IconAnchor.BOTTOM)

        mapboxNavigation.flowRoutesUpdated()
            .onEach { annotationManager.deleteAll() }
            .mapNotNull {
                it.navigationRoutes.firstOrNull()?.upcomingRoadObjects
            }
            .observe { upcomingRoadObjects ->
                upcomingRoadObjects.mapNotNull {
                    if (it.roadObject.objectType == RoadObjectType.REST_STOP) {
                        (it.roadObject.location.shape as? Point)
                    } else {
                        null
                    }
                }.forEach {
                    annotationManager.create(annotation.withPoint(it))
                }
            }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        annotationManager.deleteAll()
    }
}
