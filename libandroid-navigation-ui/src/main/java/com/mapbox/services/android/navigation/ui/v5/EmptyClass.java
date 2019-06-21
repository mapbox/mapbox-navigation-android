package com.mapbox.services.android.navigation.ui.v5;

import android.view.View;

public class EmptyClass implements SoundButton {
  /**
   * Toggles the muted state.
   *
   * @return whether it is muted or not
   */
  @Override
  public boolean toggleMute() {
    return false;
  }

  /**
   * Meant for initializing. This is to avoid showing any toggle animations, etc.
   *
   * @param muted whether to set muted or not
   */
  @Override
  public void setMuted(boolean muted) {

  }

  /**
   * Returns whether the sound button is currently displaying that it is muted.
   *
   * @return whether the sound button is muted
   */
  @Override
  public boolean isMuted() {
    return false;
  }

  /**
   * Adds an onClickListener to the button
   *
   * @param onClickListener to add
   */
  @Override
  public void addOnClickListener(View.OnClickListener onClickListener) {

  }

  /**
   * Removes an onClickListener from the button
   *
   * @param onClickListener to remove
   */
  @Override
  public void removeOnClickListener(View.OnClickListener onClickListener) {

  }

  /**
   * Hides the button
   */
  @Override
  public void hide() {

  }

  /**
   * Shows the button
   */
  @Override
  public void show() {

  }
}
