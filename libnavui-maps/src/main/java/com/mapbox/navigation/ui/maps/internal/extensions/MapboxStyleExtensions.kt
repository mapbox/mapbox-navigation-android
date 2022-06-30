@file:JvmName("MapboxStyleEx")

package com.mapbox.navigation.ui.maps.internal.extensions

import com.mapbox.maps.Style
import com.mapbox.navigation.ui.maps.NavigationStyles

private val STYLE_URI_REGEX = "mapbox://styles/(.+)/(.+)".toRegex()

/**
 * Given Style with URI that matches `mapbox://styles/USER_ID/STYLE_ID` pattern
 * return USER_ID, otherwise return `null`.
 */
fun Style.getUserId(): String? = when (styleURI) {
    NavigationStyles.NAVIGATION_DAY_STYLE -> NavigationStyles.NAVIGATION_DAY_STYLE_USER_ID
    NavigationStyles.NAVIGATION_NIGHT_STYLE -> NavigationStyles.NAVIGATION_NIGHT_STYLE_USER_ID
    else -> {
        // mapbox://styles/USER_ID/STYLE_ID
        STYLE_URI_REGEX.find(styleURI)?.groupValues?.getOrNull(index = 1)
    }
}

/**
 * Given Style with URI that matches `mapbox://styles/USER_ID/STYLE_ID` pattern
 * return STYLE_ID, otherwise return `null`.
 */
fun Style.getStyleId(): String? = when (styleURI) {
    NavigationStyles.NAVIGATION_DAY_STYLE -> NavigationStyles.NAVIGATION_DAY_STYLE_ID
    NavigationStyles.NAVIGATION_NIGHT_STYLE -> NavigationStyles.NAVIGATION_NIGHT_STYLE_ID
    else -> {
        // mapbox://styles/USER_ID/STYLE_ID
        STYLE_URI_REGEX.find(styleURI)?.groupValues?.lastOrNull()
    }
}
