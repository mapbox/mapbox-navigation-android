package com.mapbox.navigation.testing

import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration

class NavSDKRobolectricTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    /**
     * Refs https://github.com/robolectric/robolectric/issues/4340 and tries to remove duplicates from robolectric's sandbox.
     */
    override fun createClassLoaderConfig(method: FrameworkMethod?): InstrumentationConfiguration {
        val builder = InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
        builder.doNotAcquirePackage("com.mapbox.navigator")
        builder.doNotAcquirePackage("com.mapbox.bindgen")
        return builder.build()
    }
}
