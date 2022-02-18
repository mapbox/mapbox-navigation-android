package com.mapbox.navigation.core.lifecycle

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions

/**
 * Represents a function that returns [NavigationOptions]
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface NavigationOptionsProvider {

    /**
     * Generates [NavigationOptions].
     */
    fun createNavigationOptions(): NavigationOptions
}
