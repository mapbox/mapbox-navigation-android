package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.TileEndpointConfiguration;

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
      return navigator.configureRouter(tilePath, null, null, new TileEndpointConfiguration("https://api-routing-tiles-staging.tilestream.net", "2019_04_13-00_00_11", "pk.eyJ1IjoieWhhaG4tMTU2Mjc3MDA0MzEzMSIsImEiOiJjanh4Y3lod2IwMGp4M2NrNTVpNTN2bjY5In0.FIRXnGfQ3cb5Cga0wdECCw", ""));
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
