package com.mapbox.navigation.ui.maps.internal.ui

import android.view.View
import androidx.core.view.isVisible
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.internal.extensions.flowNavigationCameraState
import com.mapbox.navigation.ui.maps.view.MapboxCameraModeButton
import com.mapbox.navigation.ui.utils.internal.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@ExperimentalPreviewMapboxNavigationAPI
interface CameraModeButtonComponentContract {

    val isVisible: StateFlow<Boolean>

    val buttonState: StateFlow<NavigationCameraState>

    fun onClick(view: View)
}

@ExperimentalPreviewMapboxNavigationAPI
class CameraModeButtonComponent(
    private val cameraModeButton: MapboxCameraModeButton,
    private val contractProvider: Provider<CameraModeButtonComponentContract>,
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        val contract = contractProvider.get()
        contract.isVisible.observe { cameraModeButton.isVisible = it }
        contract.buttonState.observe(action = cameraModeButton::setState)
        cameraModeButton.setOnClickListener(contract::onClick)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        super.onDetached(mapboxNavigation)
        cameraModeButton.setOnClickListener(null)
    }
}

@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxCameraModeButtonComponentContract(
    private val navigationCameraProvider: Provider<NavigationCamera?>
) : UIComponent(),
    CameraModeButtonComponentContract {

    override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)

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
