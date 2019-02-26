package com.mapbox.services.android.navigation.ui.v5;

public interface SoundButton extends NavigationButton {
  /**
   * Toggles the muted state.
   * @return whether it is muted or not
   */
  boolean toggleMute();

  /**
   * Meant for initializing.
   * @param muted whether to set muted or not
   */
  void setMuted(boolean muted);

  boolean isMuted();
}
