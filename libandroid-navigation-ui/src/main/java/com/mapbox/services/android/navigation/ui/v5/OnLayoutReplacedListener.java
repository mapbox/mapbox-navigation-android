package com.mapbox.services.android.navigation.ui.v5;

import android.widget.FrameLayout;

/**
 * Listener used with {@link CustomLayoutUpdater#update(FrameLayout, int, OnLayoutReplacedListener)}.
 */
public interface OnLayoutReplacedListener {

  /**
   * Invoked when the layout resource ID has been inflated and added to the parent.
   */
  void onLayoutReplaced();
}
