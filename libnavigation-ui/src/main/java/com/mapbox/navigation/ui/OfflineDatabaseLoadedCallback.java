package com.mapbox.navigation.ui;

interface OfflineDatabaseLoadedCallback {

  void onComplete();

  void onError(String error);
}