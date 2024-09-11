package com.mapbox.navigation.core.internal

import com.mapbox.navigation.base.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SdkInfoProviderTest {

    @Test
    fun testCoreFrameworkSdkInformation() {
        sdkVariant = SdkVariant.CORE_FRAMEWORK

        val sdkInformation = SdkInfoProvider.sdkInformation()
        assertEquals("mapbox-navigationCore-android", sdkInformation.name)
        assertEquals("com.mapbox.navigationCore", sdkInformation.packageName)
        assertEquals(BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sdkInformation.version)
    }

    @Test
    fun testUXFrameworkSdkInformation() {
        sdkVariant = SdkVariant.UX_FRAMEWORK

        val sdkInformation = SdkInfoProvider.sdkInformation()
        assertEquals("mapbox-navigationUX-android", sdkInformation.name)
        assertEquals("com.mapbox.navigationUX", sdkInformation.packageName)
        assertEquals(BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sdkInformation.version)
    }
}
