package com.mapbox.navigation.instrumentation_tests

import android.app.Application
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LoggingLevel

class InstrumentationTestsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
    }
}
