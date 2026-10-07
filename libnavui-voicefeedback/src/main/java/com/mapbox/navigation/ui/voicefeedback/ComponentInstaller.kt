package com.mapbox.navigation.ui.voicefeedback

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.voicefeedback.internal.VoiceFeedbackComponent
import com.mapbox.navigation.ui.voicefeedback.view.MapboxVoiceFeedbackButton

/**
 * Installs the voice feedback button component into the current Mapbox Navigation flow.
 *
 * @param button The visual representation of the voice feedback button.
 * @return An [Installation] handle that can be used to remove the component.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.voiceFeedbackButton(button: MapboxVoiceFeedbackButton): Installation {
    return component(VoiceFeedbackComponent(button))
}
