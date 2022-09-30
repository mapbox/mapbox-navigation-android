package com.mapbox.navigation.dropin.camera

import android.view.View
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.camera.CameraAction
import com.mapbox.navigation.ui.app.internal.camera.TargetCameraMode
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.maps.internal.ui.RecenterButtonComponentContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@ExperimentalPreviewMapboxNavigationAPI
internal class RecenterButtonComponentContractImpl(
    scope: CoroutineScope,
    private val store: Store
) : RecenterButtonComponentContract {

    override val isVisible: StateFlow<Boolean> = combine(
        store.select { it.camera.cameraMode },
        store.select { it.navigation },
        transform = ::isVisible
    ).stateIn(
        scope,
        SharingStarted.WhileSubscribed(),
        isVisible(store.state.value.camera.cameraMode, store.state.value.navigation)
    )

    override fun onClick(view: View) {
        store.dispatch(CameraAction.SetCameraMode(TargetCameraMode.Following))
    }

    private fun isVisible(cameraMode: TargetCameraMode, navigationState: NavigationState) =
        cameraMode == TargetCameraMode.Idle && navigationState != NavigationState.RoutePreview
}
