package com.mapbox.navigation.dropin.component.speedlimit

import android.location.Location
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.util.MapboxDropInUtils.toVisibility
import com.mapbox.navigation.ui.speedlimit.view.MapboxSpeedLimitView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

internal sealed interface SpeedLimitUIComponent : UIComponent {
    val container: FrameLayout
}

internal class CustomSpeedLimitUIComponent(
    override val container: FrameLayout
) : SpeedLimitUIComponent {
    override fun onNavigationStateChanged(state: NavigationState) {
        container.visibility = when (state) {
            NavigationState.FreeDrive,
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

internal class MapboxSpeedLimitUIComponent(
    override val container: FrameLayout,
    private val view: MapboxSpeedLimitView,
    private val viewModel: SpeedLimitViewModel,
    private val lifecycleOwner: LifecycleOwner
) : SpeedLimitUIComponent, LocationObserver {

    init {
        observeSpeedLimitState()
    }

    private fun performAction(vararg action: Flow<SpeedLimitAction>) {
        viewModel.consumeAction(
            flowOf(*action).flattenConcat()
        )
    }

    override fun onNavigationStateChanged(state: NavigationState) {
        val navStateAction = flowOf(SpeedLimitAction.UpdateNavigationState(state))
        performAction(navStateAction)
    }

    override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
        val locationMatcherAction = flowOf(
            SpeedLimitAction.UpdateLocationMatcher(locationMatcherResult)
        )
        performAction(locationMatcherAction)
    }

    override fun onNewRawLocation(rawLocation: Location) {
        // no impl
    }

    private fun observeSpeedLimitState() {
        lifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                container.visibility = state.isVisible.toVisibility()
                view.render(state.speedLimit)
            }
        }
    }
}
