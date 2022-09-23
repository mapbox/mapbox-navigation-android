@file:JvmName("NavigationOptionsExtensions")

package com.mapbox.navigation.base.internal.extensions

import com.mapbox.navigation.base.internal.CopilotOptions
import com.mapbox.navigation.base.options.NavigationOptions

fun NavigationOptions.Builder.copilotOptions(copilotOptions: CopilotOptions):
    NavigationOptions.Builder = this.copilotOptions(copilotOptions)

fun NavigationOptions.copilotOptions(): CopilotOptions = this.copilotOptions
