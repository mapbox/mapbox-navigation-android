@file:JvmName("MapboxExtendableButtonEx")

package com.mapbox.navigation.dropin.internal.extensions

import android.content.res.Resources
import androidx.annotation.StyleRes
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.utils.internal.logE

/**
 * Safely change the style of the receiver [MapboxExtendableButton].
 *
 * This method calls [MapboxExtendableButton.updateStyle] and captures [Resources.NotFoundException].
 *
 * @see [MapboxExtendableButton.updateStyle]
 */
@ExperimentalPreviewMapboxNavigationAPI
internal fun MapboxExtendableButton.tryUpdateStyle(@StyleRes style: Int) {
    try {
        updateStyle(style)
    } catch (e: Resources.NotFoundException) {
        logE(
            "Failed to update style: ${e.localizedMessage}",
            "MapboxExtendableButton"
        )
    }
}
