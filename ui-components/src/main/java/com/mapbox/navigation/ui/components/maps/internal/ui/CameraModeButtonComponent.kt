package com.mapbox.navigation.ui.components.maps.internal.ui

import android.view.View
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.components.maps.view.MapboxCameraModeButton
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.internal.extensions.flowNavigationCameraState
import com.mapbox.navigation.ui.utils.internal.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CameraModeButtonComponentContract {

    val buttonState: StateFlow<NavigationCameraState>

    fun onClick(view: View)
}

class CameraModeButtonComponent(
    private val cameraModeButton: MapboxCameraModeButton,
    private val contractProvider: Provider<CameraModeButtonComponentContract>,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val contract = contractProvider.get()
        contract.buttonState.observe { cameraModeButton.setState(it) }
        cameraModeButton.setOnClickListener(contract::onClick)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        cameraModeButton.setOnClickListener(null)
    }
}

internal class MapboxCameraModeButtonComponentContract(
    private val navigationCameraProvider: Provider<NavigationCamera?>,
) : UIComponent(),
    CameraModeButtonComponentContract {

    private val _buttonState = MutableStateFlow(NavigationCameraState.OVERVIEW)
    override val buttonState: StateFlow<NavigationCameraState> = _buttonState.asStateFlow()

    private var navigationCamera: NavigationCamera? = null

    override fun onClick(view: View) {
        when (buttonState.value) {
            NavigationCameraState.OVERVIEW ->
                navigationCamera?.requestNavigationCameraToFollowing()
            NavigationCameraState.FOLLOWING ->
                navigationCamera?.requestNavigationCameraToOverview()
            else -> {
                /* do nothing */
            }
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        navigationCamera = navigationCameraProvider.get()

        navigationCamera?.flowNavigationCameraState()?.observe {
            if (it != NavigationCameraState.IDLE) {
                _buttonState.value = it
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        navigationCamera = null
    }
}
