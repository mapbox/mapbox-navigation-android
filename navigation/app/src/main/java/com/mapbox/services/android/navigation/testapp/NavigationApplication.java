package com.mapbox.services.android.navigation.testapp;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

import timber.log.Timber;

/**
 * Created by antonio on 4/14/17.
 */

public class NavigationApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    // Leak canary
    if (LeakCanary.isInAnalyzerProcess(this)) {
      return;
    }
    LeakCanary.install(this);

    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }

    // Access token
//    String mapboxAccessToken = Utils.getMapboxAccessToken(getApplicationContext());
//    if (TextUtils.isEmpty(mapboxAccessToken)) {
//      Log.w(LOG_TAG, "Warning: access token isn't set.");
//    }
//    Mapbox.getInstance(getApplicationContext(), mapboxAccessToken);
  }

}
