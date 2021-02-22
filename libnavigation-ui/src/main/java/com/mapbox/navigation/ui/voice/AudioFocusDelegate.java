package com.mapbox.navigation.ui.voice;

/**
 * Delegate to retrieve and release audio focus. Used by the SDK to play navigation commands.
 */
interface AudioFocusDelegate {

  /**
   * Request audio focus.
   */
  void requestFocus();

  /**
   * Abandon audio focus.
   */
  void abandonFocus();
}
