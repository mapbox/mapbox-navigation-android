package com.mapbox.services.android.navigation.ui.v5;

interface OfflineRegionDownloadCallback {

  void onComplete();

  void onError(String error);
}