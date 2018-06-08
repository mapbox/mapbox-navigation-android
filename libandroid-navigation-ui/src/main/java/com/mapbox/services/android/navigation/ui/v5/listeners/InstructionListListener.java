package com.mapbox.services.android.navigation.ui.v5.listeners;

/**
 * A listener that is triggered when the instruction list in InstructionView is shown or hidden.
 */
public interface InstructionListListener {
  /**
   * Triggered when the instruction list is shown or hidden.
   *
   * @param visible whether the list is shown or hidden
   * @since 0.15.0
   */
  void onInstructionListVisibilityChanged(boolean visible);
}
