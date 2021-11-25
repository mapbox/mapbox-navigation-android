package com.mapbox.navigation.dropin.component.maneuver

import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.util.MapboxDropInUtils.toVisibility
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

internal sealed interface ManeuverUIComponent : UIComponent {
    val container: FrameLayout
}

internal class CustomManeuverUIComponent(
    override val container: FrameLayout
) : ManeuverUIComponent {

    override fun onNavigationStateChanged(state: NavigationState) {
        container.visibility = when (state) {
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

internal class MapboxManeuverUIComponent(
    override val container: FrameLayout,
    private val view: MapboxManeuverView,
    private val viewModel: ManeuverViewModel,
    private val lifecycleOwner: LifecycleOwner
) : ManeuverUIComponent, RouteProgressObserver {

    init {
        observeManeuverState()
    }

    private fun performAction(vararg action: Flow<ManeuverAction>) {
        viewModel.consumeAction(
            flowOf(*action).flattenConcat()
        )
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val routeProgressAction = flowOf(ManeuverAction.UpdateRouteProgress(routeProgress))
        performAction(routeProgressAction)
    }

    override fun onNavigationStateChanged(state: NavigationState) {
        val navStateAction = flowOf(ManeuverAction.UpdateNavigationState(state))
        performAction(navStateAction)
    }

    private fun observeManeuverState() {
        lifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                container.visibility = state.isVisible.toVisibility()
                // view.renderManeuvers(state.maneuver)
            }
        }
    }
}
