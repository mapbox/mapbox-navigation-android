package com.mapbox.navigation.dropin.binder

import android.transition.Scene
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.audioguidance.AudioAction
import com.mapbox.navigation.dropin.component.cameramode.CameraModeButtonComponent
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.recenter.RecenterButtonComponent
import com.mapbox.navigation.dropin.databinding.MapboxActionButtonsLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.model.Store
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.voice.internal.ui.AudioComponentContract
import com.mapbox.navigation.ui.voice.internal.ui.AudioGuidanceButtonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class ActionButtonBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_action_buttons_layout,
            viewGroup.context
        )
        TransitionManager.go(scene, Slide(Gravity.RIGHT))

        val binding = MapboxActionButtonsLayoutBinding.bind(viewGroup)
        val store = context.viewModel.store
        return navigationListOf(
            reloadOnChange(context.styles.audioGuidanceButtonStyle) { style ->
                audioGuidanceButtonComponent(binding, style, store)
            },
            reloadOnChange(context.styles.cameraModeButtonStyle) { style ->
                CameraModeButtonComponent(
                    store = store,
                    cameraModeButton = binding.cameraModeButton,
                    cameraModeStyle = style
                )
            },
            reloadOnChange(context.styles.recenterButtonStyle) { style ->
                RecenterButtonComponent(
                    store = store,
                    recenterStyle = style,
                    recenterButton = binding.recenterButton
                )
            }
        )
    }

    private fun audioGuidanceButtonComponent(
        binding: MapboxActionButtonsLayoutBinding,
        style: Int,
        store: Store
    ): AudioGuidanceButtonComponent {
        return AudioGuidanceButtonComponent(binding.soundButton, style, contractProvider = {
            DropInAudioComponentContract(context.viewModel.viewModelScope, store)
        })
    }
}

@ExperimentalPreviewMapboxNavigationAPI
internal class DropInAudioComponentContract(
    scope: CoroutineScope,
    val store: Store
) : AudioComponentContract {

    override val isMuted: StateFlow<Boolean> =
        store.slice(scope) { it.audio.isMuted }

    override val isVisible: StateFlow<Boolean> =
        store.slice(scope) { it.navigation == NavigationState.ActiveNavigation }

    override fun mute() {
        store.dispatch(AudioAction.Mute)
    }

    override fun unMute() {
        store.dispatch(AudioAction.Unmute)
    }
}
