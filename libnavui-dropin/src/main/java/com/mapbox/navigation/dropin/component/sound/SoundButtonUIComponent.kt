package com.mapbox.navigation.dropin.component.sound

import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mapbox.navigation.dropin.component.UIComponent
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.dropin.util.MapboxDropInUtils.toVisibility
import com.mapbox.navigation.ui.voice.view.MapboxSoundButton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

internal sealed interface SoundButtonUIComponent : UIComponent {
    val container: FrameLayout
}

internal class CustomSoundButtonUIComponent(
    override val container: FrameLayout
) : SoundButtonUIComponent {
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

internal class MapboxSoundButtonUIComponent(
    override val container: FrameLayout,
    private val view: MapboxSoundButton,
    private val viewModel: SoundButtonViewModel,
    private val lifeCycleOwner: LifecycleOwner
) : SoundButtonUIComponent {

    init {
        observeSoundState()
    }

    private fun performAction(vararg action: Flow<SoundButtonAction>) {
        viewModel.consumeAction(
            flowOf(*action).flattenConcat()
        )
    }

    override fun onNavigationStateChanged(state: NavigationState) {
        val navStateAction = flowOf(SoundButtonAction.UpdateNavigationState(state))
        performAction(navStateAction)
    }

    fun onVolume(volume: Float) {
        val volumeAction = flowOf(SoundButtonAction.UpdateVolume(volume))
        performAction(volumeAction)
    }

    private fun observeSoundState() {
        lifeCycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                container.visibility = state.isVisible.toVisibility()
                if (state.isMute) {
                    view.mute()
                } else {
                    view.unmute()
                }
            }
        }
    }
}
