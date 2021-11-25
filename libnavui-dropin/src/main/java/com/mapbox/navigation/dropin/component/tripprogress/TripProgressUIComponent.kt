package com.mapbox.navigation.dropin.component.tripprogress

import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.util.MapboxDropInUtils.toVisibility
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

internal sealed interface TripProgressUIComponent : UIComponent {
    val container: FrameLayout
}

internal class CustomTripProgressUIComponent(
    override val container: FrameLayout
) : TripProgressUIComponent {
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

internal class MapboxTripProgressUIComponent(
    override val container: FrameLayout,
    private val view: MapboxTripProgressView,
    private val viewModel: TripProgressViewModel,
    private val lifeCycleOwner: LifecycleOwner
) : TripProgressUIComponent, RouteProgressObserver {

    init {
        observeTripProgressState()
    }

    private fun performAction(vararg action: Flow<TripProgressAction>) {
        viewModel.consumeAction(
            flowOf(*action).flattenConcat()
        )
    }

    override fun onNavigationStateChanged(state: NavigationState) {
        val navStateAction = flowOf(TripProgressAction.UpdateNavigationState(state))
        performAction(navStateAction)
    }

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
    }

    private fun observeTripProgressState() {
        lifeCycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                container.visibility = state.isVisible.toVisibility()
            }
        }
    }
}
