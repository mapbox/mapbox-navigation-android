package com.mapbox.navigation.qa

import android.text.TextUtils
import androidx.multidex.MultiDexApplication
import com.mapbox.base.common.logger.model.Message
import com.mapbox.common.logger.MapboxLogger
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.qa.utils.Utils
import timber.log.Timber

private const val DEFAULT_MAPBOX_ACCESS_TOKEN = "YOUR_MAPBOX_ACCESS_TOKEN_GOES_HERE"

class QATestApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        setupTimber()
        //setupMapbox()
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun setupMapbox() {
        val mapboxAccessToken = Utils.getMapboxAccessToken(applicationContext)
        if (TextUtils.isEmpty(mapboxAccessToken) ||
            mapboxAccessToken == DEFAULT_MAPBOX_ACCESS_TOKEN
        ) {
            MapboxLogger.w(Message("Mapbox access token isn't set!"))
        }

        Mapbox.getInstance(applicationContext, mapboxAccessToken)
        MapboxNavigationProvider
            .create(
                MapboxNavigation
                    .defaultNavigationOptionsBuilder(this, mapboxAccessToken)
                    .build()
            )
    }
}
