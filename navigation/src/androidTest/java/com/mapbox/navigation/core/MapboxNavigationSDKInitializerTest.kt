package com.mapbox.navigation.core

import com.mapbox.common.SdkInfoRegistryFactory
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.core.internal.SdkVariant
import com.mapbox.navigation.core.internal.sdkVariant
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxNavigationSDKInitializerTest {

    @Test
    fun testSdkInformationProvided() {
        val sdkPackageName = "com.mapbox.navigationCore"

        val sdkInformation = SdkInfoRegistryFactory.getInstance().sdkInformation.find {
            it.packageName == sdkPackageName
        }

        assertEquals(SdkInfoProvider.sdkInformation(), sdkInformation)
    }

    @Test
    fun testDefaultSdkVariant() {
        // After initialization, default sdk variant should be CORE_FRAMEWORK
        assertEquals(SdkVariant.CORE_FRAMEWORK, sdkVariant)
    }
}
