package com.mapbox.services.android.navigation.testapp

import android.app.Application
import android.os.StrictMode
import android.text.TextUtils
import com.mapbox.android.search.MapboxSearch
import com.mapbox.android.search.MapboxSearchOptions
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.services.android.navigation.testapp.example.utils.DelegatesExt
import com.squareup.leakcanary.LeakCanary
import timber.log.Timber

private const val DEFAULT_MAPBOX_ACCESS_TOKEN = "YOUR_MAPBOX_ACCESS_TOKEN_GOES_HERE"

class NavigationApplication : Application() {

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
      StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
          .detectAll()
          .penaltyLog()
          .build())
      StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
          .detectAll()
          .penaltyLog()
          .build())
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
      Timber.w("Mapbox access token isn't set!")
    }

    val cachingMode = MapboxSearchOptions().setCachingMode(MapboxSearchOptions.CACHE_EXTERNAL)
    MapboxSearch.getInstance(applicationContext, mapboxAccessToken, cachingMode)
    Mapbox.getInstance(applicationContext, mapboxAccessToken)
  }
}
