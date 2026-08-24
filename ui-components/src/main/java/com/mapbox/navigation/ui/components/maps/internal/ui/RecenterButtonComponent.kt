package com.mapbox.navigation.ui.components.maps.internal.ui

import android.view.View
import androidx.core.view.isVisible
import com.mapbox.common.location.Location
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.flowLocationMatcherResult
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.components.MapboxExtendableButton
import com.mapbox.navigation.ui.components.maps.RecenterButtonConfig
import com.mapbox.navigation.ui.utils.internal.Provider
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

interface RecenterButtonComponentContract {
    val isVisible: StateFlow<Boolean>

    fun onClick(view: View)
}

class RecenterButtonComponent(
    private val recenterButton: MapboxExtendableButton,
    private val contractProvider: Provider<RecenterButtonComponentContract>,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        val contract = contractProvider.get()
        recenterButton.setOnClickListener(contract::onClick)

        coroutineScope.launch {
            contract.isVisible.collect { visible ->
                recenterButton.isVisible = visible
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        recenterButton.setOnClickListener(null)
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class MapboxRecenterButtonComponentContract(
    private val mapView: MapView,
    private val config: RecenterButtonConfig,
) : UIComponent(), RecenterButtonComponentContract {

    private val location = AtomicReference<Location?>(null)

    override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)

    override fun onClick(view: View) {
        location.get()?.also {
            val cameraOptions = config.cameraOptions.toBuilder().center(it.toPoint()).build()
            mapView.camera.easeTo(cameraOptions, config.animationOptions)
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mapboxNavigation.flowLocationMatcherResult()
            .map { it.enhancedLocation }
            .observe { location.set(it) }
    }
}
