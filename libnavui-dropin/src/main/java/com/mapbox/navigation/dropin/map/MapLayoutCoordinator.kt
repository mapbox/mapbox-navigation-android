package com.mapbox.navigation.dropin.map

import android.view.ViewGroup
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.Binder
import com.mapbox.navigation.ui.base.lifecycle.UICoordinator
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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

    private var loadStyleJob: Job? = null
    private val mapStyleLoader = navigationViewContext.mapStyleLoader

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        mapStyleLoader.mapboxMap = null
    }

    override fun MapboxNavigation.flowViewBinders(): Flow<Binder<ViewGroup>> {
        return navigationViewContext.uiBinders.mapViewBinder.map { customBinder ->
            loadStyleJob?.cancel()
            loadStyleJob = null
            navigationViewContext.mapViewOwner.updateMapView(null)
            (customBinder ?: MapboxMapViewBinder()).also {
                it.context = navigationViewContext
                it.navigationViewBinding = binding
                navigationViewContext.mapViewOwner.doOnAttachMapView { mapView ->
                    mapStyleLoader.mapboxMap = mapView.getMapboxMap()
                    if (it.shouldLoadMapStyle) {
                        mapStyleLoader.loadInitialStyle()
                        loadStyleJob = coroutineScope.launch {
                            mapStyleLoader.observeAndReloadNewStyles()
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalPreviewMapboxNavigationAPI
internal inline fun MapViewOwner.doOnAttachMapView(crossinline action: (MapView) -> Unit) {
    registerObserver(object : MapViewObserver() {
        override fun onAttached(mapView: MapView) {
            action(mapView)
            unregisterObserver(this)
        }
    })
}
