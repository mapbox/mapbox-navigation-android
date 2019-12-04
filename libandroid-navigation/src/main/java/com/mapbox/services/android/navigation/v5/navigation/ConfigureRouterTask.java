package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.TileEndpointConfiguration;

class ConfigureRouterTask extends AsyncTask<Void, Void, Long> {
  private final Navigator navigator;
  private final String tilePath;
  private final TileEndpointConfiguration tileEndpointConfiguration;
  private final OnOfflineTilesConfiguredCallback callback;

  ConfigureRouterTask(Navigator navigator, String tilePath, TileEndpointConfiguration tileEndpointConfiguration,
                      OnOfflineTilesConfiguredCallback callback) {
    this.navigator = navigator;
    this.tilePath = tilePath;
    this.tileEndpointConfiguration = tileEndpointConfiguration;
    this.callback = callback;
  }

  @Override
  protected Long doInBackground(Void... paramsUnused) {
    synchronized (this) {
      return navigator.configureRouter(tilePath, null, null, 2, tileEndpointConfiguration);
    }
  }

  @Override
  protected void onPostExecute(Long numberOfTiles) {
    if (numberOfTiles >= 0) {
      callback.onConfigured(numberOfTiles.intValue());
    } else {
      OfflineError error = new OfflineError("Offline tile configuration error: 0 tiles found in directory");
      callback.onConfigurationError(error);
    }
  }
}
