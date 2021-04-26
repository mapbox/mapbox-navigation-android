@file:JvmName("RouteOptionsEx")
package com.mapbox.navigation.examples.util

import android.content.Context
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.extensions.applyDefaultOptions
import com.mapbox.navigation.base.extensions.applyLocationAndVoiceUnit
import com.mapbox.navigation.base.extensions.applyRecommendedOptions

fun RouteOptions.Builder.applyAllOptions(context: Context) = apply {
    applyDefaultOptions()
    applyLocationAndVoiceUnit(context)
    applyRecommendedOptions()
}
