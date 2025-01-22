package com.mapbox.navigation.core.lifecycle

import com.mapbox.navigation.base.options.NavigationOptions

/**
 * Represents a function that returns [NavigationOptions]
 */
fun interface NavigationOptionsProvider {

    /**
     * Generates [NavigationOptions].
     */
    fun createNavigationOptions(): NavigationOptions
}
