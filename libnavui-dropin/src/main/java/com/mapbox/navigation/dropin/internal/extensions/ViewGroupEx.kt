@file:JvmName("ViewGroupEx")

package com.mapbox.navigation.dropin.internal.extensions

import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.MapboxExtendableButtonParams
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal fun ViewGroup.recreateButton(
    customParams: MapboxExtendableButtonParams
): MapboxExtendableButton {
    removeView(getChildAt(0))
    val button = MapboxExtendableButton(context, null, 0, customParams.style).apply {
        layoutParams = customParams.layoutParams
    }
    addView(button, 0)
    return button
}
