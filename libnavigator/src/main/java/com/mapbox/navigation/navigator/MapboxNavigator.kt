package com.mapbox.navigation.navigator

import com.mapbox.navigator.Navigator

object MapboxNavigator {

    init {
        System.loadLibrary("navigator-android")
    }

    val instance: Navigator by lazy { Navigator() }
}
