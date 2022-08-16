package com.mapbox.navigation.qa_test_app

import android.app.Application
import com.mapbox.androidauto.MapboxCarApp

class QaTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MapboxCarApp.setup(this)
    }
}
