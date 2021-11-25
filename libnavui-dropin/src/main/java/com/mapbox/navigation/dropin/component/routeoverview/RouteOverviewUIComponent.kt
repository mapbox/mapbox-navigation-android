package com.mapbox.navigation.dropin.component.routeoverview

import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.util.MapboxDropInUtils.toVisibility
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.camera.view.MapboxRouteOverviewButton
import com.mapbox.navigation.ui.utils.internal.lifecycle.keepExecutingWhenStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.CopyOnWriteArraySet

internal sealed interface RouteOverviewUIComponent : UIComponent {
    val container: FrameLayout
}

internal class CustomRouteOverviewUIComponent(
    override val container: FrameLayout
) : RouteOverviewUIComponent {
    override fun onNavigationStateChanged(state: NavigationState) {
        container.visibility = when (state) {
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

internal class MapboxRouteOverviewUIComponent(
    override val container: FrameLayout,
    private val view: MapboxRouteOverviewButton,
    private val viewModel: RouteOverviewViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RouteOverviewUIComponent, NavigationCameraStateChangedObserver {

    private val clickListeners = CopyOnWriteArraySet<OnOverviewButtonClickedListener>()

    init {
        observeRouteOverviewState()
        view.setOnClickListener { clickListeners.forEach { it.onOverviewButtonClicked() } }
    }

    private fun performAction(vararg action: Flow<RouteOverviewButtonAction>) {
        viewModel.consumeAction(
            flowOf(*action).flattenConcat()
        )
    }

    override fun onNavigationStateChanged(state: NavigationState) {
        val navStateAction = flowOf(RouteOverviewButtonAction.UpdateNavigationState(state))
        performAction(navStateAction)
    }

    override fun onNavigationCameraStateChanged(navigationCameraState: NavigationCameraState) {
        val cameraAction = flowOf(
            RouteOverviewButtonAction.UpdateCameraState(navigationCameraState)
        )
        performAction(cameraAction)
    }

    fun registerOnOverviewButtonClickedListener(listener: OnOverviewButtonClickedListener) {
        clickListeners.add(listener)
    }

    fun unregisterOnOverviewButtonClickedListener(listener: OnOverviewButtonClickedListener) {
        clickListeners.remove(listener)
    }

    private fun observeRouteOverviewState() {
        lifecycleOwner.keepExecutingWhenStarted {
            viewModel.state.collect { state ->
                container.visibility = state.isVisible.toVisibility()
            }
        }
    }
}
