package com.mapbox.services.android.navigation.v5.navigation;

public interface RouteTileDownloadListener {
  void onError(Throwable throwable);

  void onProgressUpdate(int percent);

  void onCompletion(boolean successful);
}