package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.navigator.Navigator;

class ConfigureRouterTask extends AsyncTask<Void, Void, OfflineData> {
  private static final String EMPTY_TRANSLATIONS_DIR_PATH = "";
  private final Navigator navigator;
  private final String tilePath;
  private final OnOfflineDataInitialized callback;

  ConfigureRouterTask(Navigator navigator, String tilePath, OnOfflineDataInitialized callback) {
    this.navigator = navigator;
    this.tilePath = tilePath;
    this.callback = callback;
  }

  @Override
  protected OfflineData doInBackground(Void... paramsUnused) {
    synchronized (this) {
      long result = navigator.configureRouter(tilePath, EMPTY_TRANSLATIONS_DIR_PATH);

      return new OfflineData(getStatus(result), String.valueOf(result));
    }
  }

  private OfflineData.Status getStatus(long configureRouterResult) {
    return configureRouterResult > 0 ? OfflineData.Status.CONFIGURE_ROUTER_RESULT_SUCCESS :
      OfflineData.Status.CONFIGURE_ROUTER_RESULT_FAILURE;
  }

  @Override
  protected void onPostExecute(OfflineData offlineData) {
    callback.onOfflineDataInitialized(offlineData);
  }
}
