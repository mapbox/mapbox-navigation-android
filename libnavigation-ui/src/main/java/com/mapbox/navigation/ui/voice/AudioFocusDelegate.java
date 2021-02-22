package com.mapbox.navigation.ui.voice;

/**
 * Delegate to grad and release audio focus. Used by the SDK to play navigation commands.
 */
public interface AudioFocusDelegate {

  /**
   * Request audio focus.
   */
  void requestFocus();

  /**
   * Abandon audio focus.
   */
  void abandonFocus();
}
