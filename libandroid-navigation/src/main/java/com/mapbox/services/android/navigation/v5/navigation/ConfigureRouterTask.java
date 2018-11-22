package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.navigator.Navigator;

import timber.log.Timber;

class ConfigureRouterTask extends AsyncTask<Void, Void, Void> {
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
  protected Void doInBackground(Void... paramsUnused) {
    synchronized (navigator) {
      long i = navigator.configureRouter(tileFilePath, translationsDirPath);
      Timber.e("NUMBER " + i + " " + tileFilePath);
    }
    return null;
  }

  @Override
  protected void onPostExecute(Void paramUnused) {
    super.onPostExecute(paramUnused);
    callback.onOfflineDataInitialized();
  }
}

