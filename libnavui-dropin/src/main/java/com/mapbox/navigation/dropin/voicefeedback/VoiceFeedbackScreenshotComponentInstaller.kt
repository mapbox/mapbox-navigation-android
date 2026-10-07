package com.mapbox.navigation.dropin.voicefeedback

import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.maps.util.ViewUtils.capture
import com.mapbox.navigation.ui.voicefeedback.internal.ScreenshotCapturer
import com.mapbox.navigation.ui.voicefeedback.internal.VoiceFeedbackComponent
import com.mapbox.navigation.ui.voicefeedback.view.MapboxVoiceFeedbackButton

/**
 * Installs the voice feedback button component into the current Mapbox Navigation flow.
 *
 * @param mapView The [MapView] that will be used to capture screenshots during feedback. Leave null
 * if you don't want to capture screenshots.
 * @param button The visual representation of the voice feedback button.
 * @return An [Installation] handle that can be used to remove the component.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.voiceFeedbackButton(
    mapView: MapView? = null,
    button: MapboxVoiceFeedbackButton,
): Installation {
    val screenshotCapturer = mapView?.let {
        ScreenshotCapturer { callback ->
            it.capture { screenshot -> callback(screenshot) }
        }
    }
    return component(VoiceFeedbackComponent(button, screenshotCapturer))
}
