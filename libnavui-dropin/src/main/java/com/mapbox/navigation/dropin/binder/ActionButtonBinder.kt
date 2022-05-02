package com.mapbox.navigation.dropin.binder

import android.transition.Scene
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.audioguidance.AudioGuidanceButtonComponent
import com.mapbox.navigation.dropin.component.cameramode.CameraModeButtonComponent
import com.mapbox.navigation.dropin.component.recenter.RecenterButtonComponent
import com.mapbox.navigation.dropin.databinding.MapboxActionButtonsLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.navigationListOf
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange

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
                AudioGuidanceButtonComponent(
                    store = store,
                    audioGuidanceButton = binding.soundButton,
                    audioGuidanceButtonStyle = style,
                )
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
}
