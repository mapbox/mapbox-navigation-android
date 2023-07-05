package com.mapbox.navigation.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mapbox.common.SdkInfoRegistryFactory
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapboxNavigationSDKInitializerTest {

    @Test
    fun testSdkInformationProvided() {
        val sdkPackageName = "com.mapbox.navigation"

        val sdkInformation = SdkInfoRegistryFactory.getInstance().sdkInformation.find {
            it.packageName == sdkPackageName
        }

        Assert.assertNotNull(sdkInformation)
        Assert.assertEquals("mapbox-navigation-android", sdkInformation!!.name)
        Assert.assertEquals(BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME, sdkInformation.version)
        Assert.assertEquals(sdkPackageName, sdkInformation.packageName)
    }
}
