package com.mapbox.services.android.navigation.v5.navigation;

/**
 * Listener for receiving updates about a route tile download.
 */
public interface RouteTileDownloadListener {
  /**
   * Called if there is an error with the downloading.
   *
   * @param throwable error
   */
  void onError(Throwable throwable);

  /**
   * Called with percentage progress updates of the download.
   *
   * @param percent completed
   */
  void onProgressUpdate(int percent);

  /**
   * Called when download was completed.
   *
   * @param successful whether it was successful or not
   */
  void onCompletion(boolean successful);
}