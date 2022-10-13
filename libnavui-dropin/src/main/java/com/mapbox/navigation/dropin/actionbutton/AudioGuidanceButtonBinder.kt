package com.mapbox.navigation.dropin.actionbutton

import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.audio.AudioComponentContractImpl
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.voice.internal.ui.AudioGuidanceButtonComponent
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton

@ExperimentalPreviewMapboxNavigationAPI
internal class AudioGuidanceButtonBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        return reloadOnChange(context.styles.audioGuidanceButtonParams) { params ->
            val button = MapboxAudioGuidanceButton(viewGroup.context, null, 0, params.style)
            viewGroup.removeAllViews()
            viewGroup.addView(button, params.layoutParams)

            AudioGuidanceButtonComponent(button) {
                AudioComponentContractImpl(context.viewModel.viewModelScope, context.store)
            }
        }
    }
}
