package com.mapbox.services.android.navigation.ui.v5;

interface OfflineDatabaseLoadedCallback {

  void onComplete();

  void onError(String error);
}