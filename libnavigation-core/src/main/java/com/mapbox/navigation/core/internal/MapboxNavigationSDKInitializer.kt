package com.mapbox.navigation.core.internal

import android.content.Context
import androidx.startup.Initializer
import com.mapbox.common.MapboxSDKCommonInitializer
import com.mapbox.common.SdkInfoRegistryFactory
import com.mapbox.common.SdkInformation
import com.mapbox.navigation.core.BuildConfig

class MapboxNavigationSDKInitializer : Initializer<MapboxNavigationSDK> {
    override fun create(context: Context): MapboxNavigationSDK {
        SdkInfoRegistryFactory.getInstance().registerSdkInformation(
            SdkInformation(
                "mapbox-navigation-android",
                BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
                "com.mapbox.navigation"
            )
        )

        return MapboxNavigationSDK
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(MapboxSDKCommonInitializer::class.java)
    }
}
