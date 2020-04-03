package com.mapbox.navigation.examples

import android.os.StrictMode
import android.text.TextUtils
import androidx.multidex.MultiDexApplication
import com.mapbox.base.common.logger.model.Message
import com.mapbox.common.logger.MapboxLogger
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.navigation.examples.utils.Utils
import com.mapbox.navigation.examples.utils.extensions.DelegatesExt
import com.squareup.leakcanary.LeakCanary
import timber.log.Timber

private const val DEFAULT_MAPBOX_ACCESS_TOKEN = "YOUR_MAPBOX_ACCESS_TOKEN_GOES_HERE"

class NavigationApplication : MultiDexApplication() {

    companion object {
        var instance: NavigationApplication by DelegatesExt.notNullSingleValue()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        setupTimber()
        setupStrictMode()
        setupCanary()
        setupMapbox()
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }

    private fun setupCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)
    }

    private fun setupMapbox() {
        val mapboxAccessToken = Utils.getMapboxAccessToken(applicationContext)
        if (TextUtils.isEmpty(mapboxAccessToken) || mapboxAccessToken == DEFAULT_MAPBOX_ACCESS_TOKEN) {
            MapboxLogger.w(Message("Mapbox access token isn't set!"))
        }

        Mapbox.getInstance(applicationContext, mapboxAccessToken)
    }
}
