package com.mapbox.navigation.ui.voice

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.ComponentConfig
import com.mapbox.navigation.ui.voice.internal.ui.AudioGuidanceButtonComponent
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
fun ComponentConfig.audioGuidanceButtonComponent(button: MapboxAudioGuidanceButton) {
    component(AudioGuidanceButtonComponent(button))
}
