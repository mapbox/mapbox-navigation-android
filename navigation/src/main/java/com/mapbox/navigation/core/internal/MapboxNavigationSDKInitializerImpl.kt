package com.mapbox.navigation.core.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.startup.Initializer
import com.mapbox.common.MapboxSDKCommonInitializerImpl
import com.mapbox.common.SdkInfoRegistryFactory

class MapboxNavigationSDKInitializerImpl : Initializer<MapboxNavigationSDK> {

    override fun create(context: Context): MapboxNavigationSDK {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA,
        )

        val uxfKey = appInfo.metaData?.getBoolean("com.mapbox.navigation.UxFramework")
        sdkVariant = if (uxfKey == true) {
            SdkVariant.UX_FRAMEWORK
        } else {
            SdkVariant.CORE_FRAMEWORK
        }

        SdkInfoRegistryFactory.getInstance().registerSdkInformation(
            SdkInfoProvider.sdkInformation(),
        )

        return MapboxNavigationSDK
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf(MapboxSDKCommonInitializerImpl::class.java)
    }
}
