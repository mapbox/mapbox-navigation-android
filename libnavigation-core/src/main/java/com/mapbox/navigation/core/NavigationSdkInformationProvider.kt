package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * A class that provides Navigation SDK information, like version name
 */
@ExperimentalPreviewMapboxNavigationAPI
object NavigationSdkInformationProvider {

    /**
     * Returns Navigation SDK version name
     */
    val versionName: String
        get() = BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME
}
