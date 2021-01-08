package com.mapbox.navigation.instrumentation_tests

import android.app.Application
import timber.log.Timber

class InstrumentationTestsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
