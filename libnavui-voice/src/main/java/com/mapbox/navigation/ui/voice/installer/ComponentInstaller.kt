package com.mapbox.navigation.ui.voice.installer

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.voice.internal.MapboxAudioGuidance
import com.mapbox.navigation.ui.voice.internal.impl.MapboxAudioGuidanceImpl
import com.mapbox.navigation.ui.voice.internal.ui.AudioGuidanceButtonComponent
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton

/**
 * Install component that connects [MapboxAudioGuidanceButton] to the default [MapboxAudioGuidance] instance.
 *
 * The installed component:
 * - handles button onClick events and mutes/un-umutes voice instructions playback
 * - updates button muted/un-muted state
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.audioGuidanceButton(button: MapboxAudioGuidanceButton): Installation {
    ensureAudioGuidanceRegistered(button.context)
    return component(AudioGuidanceButtonComponent(button))
}

@ExperimentalPreviewMapboxNavigationAPI
private fun ensureAudioGuidanceRegistered(context: Context) {
    if (MapboxNavigationApp.getObservers(MapboxAudioGuidance::class).isEmpty()) {
        val audioGuidance = MapboxAudioGuidanceImpl.create(context)
        MapboxNavigationApp.registerObserver(audioGuidance)
    }
}
