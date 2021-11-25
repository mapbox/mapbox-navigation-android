package com.mapbox.navigation.dropin.component.recenter

import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.util.MapboxDropInUtils.toVisibility
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.camera.view.MapboxRecenterButton
import com.mapbox.navigation.ui.utils.internal.lifecycle.keepExecutingWhenStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.CopyOnWriteArraySet

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

    private val clickListeners = CopyOnWriteArraySet<OnRecenterButtonClickedListener>()

    init {
        observeRecenterButtonState()
        view.setOnClickListener { clickListeners.forEach { it.onRecenterButtonClicked() } }
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

    fun registerOnRecenterButtonClickedListener(listener: OnRecenterButtonClickedListener) {
        clickListeners.add(listener)
    }

    fun unregisterOnRecenterButtonClickedListener(listener: OnRecenterButtonClickedListener) {
        clickListeners.remove(listener)
    }

    private fun observeRecenterButtonState() {
        lifecycleOwner.keepExecutingWhenStarted {
            viewModel.state.collect { state ->
                container.visibility = state.isVisible.toVisibility()
            }
        }
    }
}
