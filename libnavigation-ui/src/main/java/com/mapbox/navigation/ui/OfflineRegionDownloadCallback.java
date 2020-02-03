package com.mapbox.navigation.ui;

interface OfflineRegionDownloadCallback {

  void onComplete();

  void onError(String error);
}