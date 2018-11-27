package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.navigator.Navigator;

class ConfigureRouterTask extends AsyncTask<Void, Void, OfflineData> {
  private final Navigator navigator;
  private final String tileFilePath;
  private final String translationsDirPath;
  private final OnOfflineDataInitialized callback;

  ConfigureRouterTask(Navigator navigator, String tileFilePath, String translationsDirPath,
                      OnOfflineDataInitialized callback) {
    this.navigator = navigator;
    this.tileFilePath = tileFilePath;
    this.translationsDirPath = translationsDirPath;
    this.callback = callback;
  }

  @Override
  protected OfflineData doInBackground(Void... paramsUnused) {
    synchronized (this) {
      long result = navigator.configureRouter(tileFilePath, translationsDirPath);

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
