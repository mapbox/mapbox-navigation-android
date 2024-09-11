package com.mapbox.navigation.ui.components.voice

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.components.voice.internal.ui.AudioGuidanceButtonComponent
import com.mapbox.navigation.ui.components.voice.view.MapboxAudioGuidanceButton

/**
 * Install component that connects [MapboxAudioGuidanceButton] to the default [MapboxAudioGuidance] instance.
 *
 * The installed component:
 * - handles button onClick events and mutes/un-mutes voice instructions playback
 * - updates button muted/un-muted state
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.audioGuidanceButton(button: MapboxAudioGuidanceButton): Installation {
    return component(AudioGuidanceButtonComponent(button))
}
