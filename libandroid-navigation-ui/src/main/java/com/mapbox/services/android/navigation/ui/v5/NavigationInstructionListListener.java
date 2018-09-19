package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;

class NavigationInstructionListListener implements InstructionListListener {

  private NavigationViewEventDispatcher dispatcher;

  NavigationInstructionListListener(NavigationViewEventDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  public void onInstructionListVisibilityChanged(boolean visible) {
    dispatcher.onInstructionListVisibilityChanged(visible);
  }
}
