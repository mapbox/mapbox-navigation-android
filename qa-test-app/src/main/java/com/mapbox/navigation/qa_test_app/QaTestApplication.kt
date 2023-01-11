package com.mapbox.navigation.qa_test_app

import android.app.Application
import com.mapbox.common.LogConfiguration
import com.mapbox.common.LoggingLevel

class QaTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LogConfiguration.setLoggingLevel(LoggingLevel.DEBUG)
    }
}
