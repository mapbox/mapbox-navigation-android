package com.mapbox.navigation.ui;

import android.view.View;

import androidx.annotation.StyleRes;

public interface NavigationButton {

  /**
   * Adds an onClickListener to the button
   *
   * @param onClickListener to add
   */
  void addOnClickListener(View.OnClickListener onClickListener);

  /**
   * Removes an onClickListener from the button
   *
   * @param onClickListener to remove
   */
  void removeOnClickListener(View.OnClickListener onClickListener);

  /**
   * Hides the button
   */
  void hide();

  /**
   * Shows the button
   */
  void show();

  /**
   * Use it to update the view style
   *
   * @param styleRes style resource
   */
  void updateStyle(@StyleRes int styleRes);
}
