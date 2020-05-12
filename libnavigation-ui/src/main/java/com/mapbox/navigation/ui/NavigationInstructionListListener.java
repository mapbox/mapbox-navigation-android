package com.mapbox.navigation.ui;

import com.mapbox.navigation.ui.listeners.InstructionListListener;

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
