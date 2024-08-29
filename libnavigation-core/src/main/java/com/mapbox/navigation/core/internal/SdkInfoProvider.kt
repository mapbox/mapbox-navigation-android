package com.mapbox.navigation.core.internal

import androidx.annotation.RestrictTo
import com.mapbox.common.SdkInformation
import com.mapbox.navigation.base.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object SdkInfoProvider {

    // These values are aligned with iOS. Shouldn't be changed
    private const val CORE_FRAMEWORK_SDK_NAME = "mapbox-navigationCore-android"
    private const val CORE_FRAMEWORK_PACKAGE_NAME = "com.mapbox.navigationCore"

    private const val UX_FRAMEWORK_SDK_NAME = "mapbox-navigationUX-android"
    private const val UX_FRAMEWORK_PACKAGE_NAME = "com.mapbox.navigationUX"

    fun sdkInformation(): SdkInformation {
        val (sdkName, packageName) = when (sdkVariant) {
            SdkVariant.CORE_FRAMEWORK -> CORE_FRAMEWORK_SDK_NAME to CORE_FRAMEWORK_PACKAGE_NAME
            SdkVariant.UX_FRAMEWORK -> UX_FRAMEWORK_SDK_NAME to UX_FRAMEWORK_PACKAGE_NAME
        }

        return SdkInformation(
            sdkName,
            BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME,
            packageName,
        )
    }
}
