package com.mapbox.navigation.dropin.component.routeoverview

import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.util.MapboxDropInUtils.toVisibility
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver
import com.mapbox.navigation.ui.maps.camera.view.MapboxRouteOverviewButton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

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

    init {
        observeRouteOverviewState()
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

    private fun observeRouteOverviewState() {
        lifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                container.visibility = state.isVisible.toVisibility()
            }
        }
    }
}
