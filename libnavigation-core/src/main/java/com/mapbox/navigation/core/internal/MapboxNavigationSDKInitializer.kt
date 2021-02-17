package com.mapbox.navigation.core.internal

import android.content.Context
import androidx.startup.Initializer
import com.mapbox.common.MapboxSDKCommonInitializer

class MapboxNavigationSDKInitializer : Initializer<MapboxNavigationSDK> {
    override fun create(context: Context): MapboxNavigationSDK {
        return MapboxNavigationSDK
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(MapboxSDKCommonInitializer::class.java)
    }
}
