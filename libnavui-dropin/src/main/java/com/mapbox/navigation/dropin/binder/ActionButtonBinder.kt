package com.mapbox.navigation.dropin.binder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.ActionButtonDescription
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.cameramode.CameraModeButtonComponent
import com.mapbox.navigation.dropin.component.recenter.RecenterButtonComponentContractImpl
import com.mapbox.navigation.dropin.databinding.MapboxActionButtonsLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.audioguidance.AudioAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.maps.internal.ui.RecenterButtonComponent
import com.mapbox.navigation.ui.voice.internal.ui.AudioComponentContract
import com.mapbox.navigation.ui.voice.internal.ui.AudioGuidanceButtonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class ActionButtonBinder(
    private val context: NavigationViewContext,
    private val customButtons: List<ActionButtonDescription>
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val binding = inflateLayout(viewGroup)
        installCustomButtons(binding.buttonContainer)

        val store = context.store
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
                recenterButtonComponent(binding, style, store)
            }
        )
    }

    private fun inflateLayout(viewGroup: ViewGroup): MapboxActionButtonsLayoutBinding {
        val layoutInflater =
            viewGroup.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        viewGroup.removeAllViews()
        layoutInflater.inflate(R.layout.mapbox_action_buttons_layout, viewGroup)
        return MapboxActionButtonsLayoutBinding.bind(viewGroup)
    }

    private fun installCustomButtons(buttonContainer: LinearLayout) {
        val spacing = buttonContainer.resources
            .getDimensionPixelSize(R.dimen.mapbox_actionList_spacing)
        customButtons.filter { it.position == ActionButtonDescription.Position.START }
            .reversed()
            .forEach {
                buttonContainer.children.firstOrNull()?.setMargins(top = spacing)
                it.view.setMargins(bottom = spacing)
                buttonContainer.addView(it.view, 0)
            }
        customButtons.filter { it.position == ActionButtonDescription.Position.END }
            .forEach {
                buttonContainer.children.lastOrNull()?.setMargins(bottom = spacing)
                it.view.setMargins(top = spacing)
                buttonContainer.addView(it.view)
            }
    }

    private fun audioGuidanceButtonComponent(
        binding: MapboxActionButtonsLayoutBinding,
        style: Int,
        store: Store
    ): AudioGuidanceButtonComponent {
        return AudioGuidanceButtonComponent(binding.soundButton, style, contractProvider = {
            AudioComponentContractImpl(context.viewModel.viewModelScope, store)
        })
    }

    private fun recenterButtonComponent(
        binding: MapboxActionButtonsLayoutBinding,
        style: Int,
        store: Store
    ): RecenterButtonComponent {
        return RecenterButtonComponent(binding.recenterButton, contractProvider = {
            RecenterButtonComponentContractImpl(context.viewModel.viewModelScope, store)
        }, style)
    }

    private fun View.setMargins(top: Int? = null, bottom: Int? = null) {
        (layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            setMargins(
                leftMargin,
                top ?: topMargin,
                rightMargin,
                bottom ?: bottomMargin
            )
            layoutParams = this
        }
    }
}

@ExperimentalPreviewMapboxNavigationAPI
internal class AudioComponentContractImpl(
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
