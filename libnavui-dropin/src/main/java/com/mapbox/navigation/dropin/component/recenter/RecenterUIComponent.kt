package com.mapbox.navigation.dropin.component.recenter

import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.util.MapboxDropInUtils.toVisibility
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

internal sealed interface RecenterUIComponent : UIComponent {
    val container: FrameLayout
}

internal class CustomRecenterUIComponent(
    override val container: FrameLayout
) : RecenterUIComponent {
    override fun onNavigationStateChanged(state: NavigationState) {
        container.visibility = when (state) {
            is NavigationState.FreeDrive,
            NavigationState.RoutePreview,
            NavigationState.ActiveNavigation,
            NavigationState.Arrival -> {
                View.VISIBLE
            }
            else -> {
                View.GONE
            }
        }
    }
}

internal class MapboxRecenterUIComponent(
    override val container: FrameLayout,
    private val view: MapboxRecenterButton,
    private val viewModel: RecenterViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecenterUIComponent, NavigationCameraStateChangedObserver {

    init {
        observeRecenterButtonState()
    }

    private fun performAction(vararg action: Flow<RecenterButtonAction>) {
        viewModel.consumeAction(
            flowOf(*action).flattenConcat()
        )
    }

    override fun onNavigationCameraStateChanged(navigationCameraState: NavigationCameraState) {
        val cameraAction = flowOf(RecenterButtonAction.UpdateCameraState(navigationCameraState))
        performAction(cameraAction)
    }

    override fun onNavigationStateChanged(state: NavigationState) {
        val navStateAction = flowOf(RecenterButtonAction.UpdateNavigationState(state))
        performAction(navStateAction)
    }

    private fun observeRecenterButtonState() {
        lifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                container.visibility = state.isVisible.toVisibility()
            }
        }
    }
}
