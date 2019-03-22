package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.navigator.Navigator;

class ConfigureRouterTask extends AsyncTask<Void, Void, Long> {
  private final Navigator navigator;
  private final String tilePath;
  private final OnOfflineTilesConfiguredCallback callback;

  ConfigureRouterTask(Navigator navigator, String tilePath, OnOfflineTilesConfiguredCallback callback) {
    this.navigator = navigator;
    this.tilePath = tilePath;
    this.callback = callback;
  }

  @Override
  protected Long doInBackground(Void... paramsUnused) {
    synchronized (this) {
      return navigator.configureRouter(tilePath);
    }
  }

  @Override
  protected void onPostExecute(Long numberOfTiles) {
    if (numberOfTiles > 0) {
      callback.onConfigured(numberOfTiles.intValue());
    } else {
      OfflineError error = new OfflineError("Offline tile configuration error: 0 tiles found in directory");
      callback.onConfigurationError(error);
    }
  }
}
