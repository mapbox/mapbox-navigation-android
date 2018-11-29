package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

/**
 * Listener for receiving updates about a route tile download.
 */
public interface RouteTileDownloadListener {

  /**
   * Called if there is an error with the downloading.
   *
   * @param error with message description
   */
  void onError(@NonNull OfflineError error);

  /**
   * Called with percentage progress updates of the download.
   *
   * @param percent completed
   */
  void onProgressUpdate(int percent);

  /**
   * Called when download was completed.
   */
  void onCompletion();
}