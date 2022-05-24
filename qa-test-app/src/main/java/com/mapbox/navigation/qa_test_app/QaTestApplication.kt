package com.mapbox.navigation.qa_test_app

import android.app.Application

class QaTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Uncomment when testing android-auto
        // MapboxCarApp.setup(this)
        // MapboxCarSearchApp.setup(this)
    }
}
