package com.mapbox.services.android.navigation.ui.v5;

public interface SoundButton extends NavigationButton {
  /**
   * Toggles the muted state.
   * @return whether it is muted or not
   */
  boolean toggleMute();

  /**
   * Meant for initializing. This is to avoid showing any toggle animations, etc.
   * @param muted whether to set muted or not
   */
  void setMuted(boolean muted);

  /**
   * Returns whether the sound button is currently displaying that it is muted.
   * @return whether the sound button is muted
   */
  boolean isMuted();
}
