package com.mapbox.navigation.dropin.actionbutton

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.MapboxExtendableButtonParams
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.camera.CameraModeButtonComponentContractImpl
import com.mapbox.navigation.dropin.camera.RecenterButtonComponentContractImpl
import com.mapbox.navigation.dropin.databinding.MapboxActionButtonsLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.audioguidance.AudioAction
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.ui.maps.internal.ui.CameraModeButtonComponent
import com.mapbox.navigation.ui.maps.internal.ui.RecenterButtonComponent
import com.mapbox.navigation.ui.maps.view.MapboxCameraModeButton
import com.mapbox.navigation.ui.voice.internal.ui.AudioComponentContract
import com.mapbox.navigation.ui.voice.internal.ui.AudioGuidanceButtonComponent
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class ActionButtonBinder(
    private val context: NavigationViewContext,
    private val customButtons: List<ActionButtonDescription>
) : UIBinder {

    private var audioButton: MapboxAudioGuidanceButton? = null
    private var recenterButton: MapboxExtendableButton? = null
    private var cameraButton: MapboxCameraModeButton? = null

    private var startCustomButtonsCount = 0

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val binding = inflateLayout(viewGroup)
        installCustomButtons(binding.buttonContainer)

        val store = context.store
        return navigationListOf(
            reloadOnChange(context.styles.cameraModeButtonParams) { params ->
                cameraModeButtonComponent(binding, params, store)
            },
            reloadOnChange(context.styles.audioGuidanceButtonParams) { params ->
                audioGuidanceButtonComponent(binding, params, store)
            },
            reloadOnChange(context.styles.recenterButtonParams) { params ->
                recenterButtonComponent(binding, params, store)
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
        customButtons
            .filter { it.position == ActionButtonDescription.Position.START }
            .asReversed()
            .also { startCustomButtonsCount = it.size }
            .onEach { buttonContainer.addView(it.view, 0) }
            .forEach { it.view.setMargins(top = spacing, bottom = spacing) }
        customButtons
            .filter { it.position == ActionButtonDescription.Position.END }
            .onEach { buttonContainer.addView(it.view) }
            .forEach { it.view.setMargins(top = spacing, bottom = spacing) }
    }

    private fun audioGuidanceButtonComponent(
        binding: MapboxActionButtonsLayoutBinding,
        customParams: MapboxExtendableButtonParams,
        store: Store,
    ): AudioGuidanceButtonComponent {
        audioButton.recreate(
            ::MapboxAudioGuidanceButton,
            customParams,
            binding.buttonContainer,
            AUDIO_BUTTON_POSITION
        ).let {
            audioButton = it
            return AudioGuidanceButtonComponent(it) {
                AudioComponentContractImpl(context.viewModel.viewModelScope, store)
            }
        }
    }

    private fun cameraModeButtonComponent(
        binding: MapboxActionButtonsLayoutBinding,
        customParams: MapboxExtendableButtonParams,
        store: Store,
    ): CameraModeButtonComponent {
        cameraButton.recreate(
            ::MapboxCameraModeButton,
            customParams,
            binding.buttonContainer,
            CAMERA_BUTTON_POSITION
        ).let {
            cameraButton = it
            return CameraModeButtonComponent(it) {
                CameraModeButtonComponentContractImpl(context.viewModel.viewModelScope, store)
            }
        }
    }

    private fun recenterButtonComponent(
        binding: MapboxActionButtonsLayoutBinding,
        customParams: MapboxExtendableButtonParams,
        store: Store,
    ): RecenterButtonComponent {
        recenterButton.recreate(
            ::MapboxExtendableButton,
            customParams,
            binding.buttonContainer,
            RECENTER_BUTTON_POSITION
        ).let {
            recenterButton = it
            return RecenterButtonComponent(it) {
                RecenterButtonComponentContractImpl(context.viewModel.viewModelScope, store)
            }
        }
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

    private fun <T : View> T?.recreate(
        factory: (context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) -> T,
        customParams: MapboxExtendableButtonParams,
        container: ViewGroup,
        position: Int,
    ): T {
        container.removeView(this)
        val button = factory(context.context, null, 0, customParams.style).apply {
            this.layoutParams = customParams.layoutParams
        }
        container.addView(button, position + startCustomButtonsCount)
        return button
    }

    private companion object {
        private const val AUDIO_BUTTON_POSITION = 1
        private const val CAMERA_BUTTON_POSITION = 0
        private const val RECENTER_BUTTON_POSITION = 2
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
