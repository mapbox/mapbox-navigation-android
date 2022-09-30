package com.mapbox.navigation.dropin.coordinator

import android.view.ViewGroup
import androidx.transition.Scene
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.map.MapBinder
import com.mapbox.navigation.dropin.databinding.MapboxMapviewLayoutBinding
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.ui.base.lifecycle.Binder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Coordinator for inflating [MapView].
 */
@OptIn(FlowPreview::class)
@ExperimentalPreviewMapboxNavigationAPI
internal class MapLayoutCoordinator(
    private val navigationViewContext: NavigationViewContext,
    private val binding: MapboxNavigationViewLayoutBinding
) : UICoordinator<ViewGroup>(binding.mapViewLayout) {

    private val viewGroup = binding.mapViewLayout
    private val mapViewOwner = navigationViewContext.mapViewOwner
    private val mapStyleLoader = navigationViewContext.mapStyleLoader
    private var reloadStyleJob: Job? = null

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapStyleLoader.mapboxMap = null
    }

    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<ViewGroup>> {
        return navigationViewContext.mapView
            .map { mapViewOverride ->
                if (mapViewOverride == null) {
                    Scene.getSceneForLayout(
                        viewGroup,
                        R.layout.mapbox_mapview_layout,
                        viewGroup.context,
                    ).enter()

                    val mapView = BoundMapViewProvider.bindLayoutAndGet(viewGroup)
                    initDefaultMap(mapView.getMapboxMap())
                    mapViewOwner.updateMapView(mapView)
                    mapView
                } else {
                    initCustomMap(mapViewOverride.getMapboxMap())
                    viewGroup.removeAllViews()
                    viewGroup.addView(mapViewOverride)
                    mapViewOwner.updateMapView(mapViewOverride)
                    mapViewOverride
                }
            }
            .map { mapView ->
                MapBinder(
                    navigationViewContext,
                    binding,
                    mapView
                )
            }
    }

    private fun initDefaultMap(mapboxMap: MapboxMap) {
        mapStyleLoader.mapboxMap = mapboxMap
        mapStyleLoader.loadInitialStyle() // immediately load map style to avoid map flashing
        reloadStyleJob = coroutineScope.launch {
            mapStyleLoader.observeAndReloadNewStyles()
        }
    }

    private fun initCustomMap(mapboxMap: MapboxMap) {
        reloadStyleJob?.cancel()
        mapStyleLoader.mapboxMap = mapboxMap
    }
}
