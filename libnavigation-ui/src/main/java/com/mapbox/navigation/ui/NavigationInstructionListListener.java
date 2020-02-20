package com.mapbox.navigation.ui;

import com.mapbox.navigation.ui.listeners.InstructionListListener;

class NavigationInstructionListListener implements InstructionListListener {

  private NavigationPresenter presenter;
  private NavigationViewEventDispatcher dispatcher;

  NavigationInstructionListListener(NavigationPresenter presenter, NavigationViewEventDispatcher dispatcher) {
    this.presenter = presenter;
    this.dispatcher = dispatcher;
  }

  @Override
  public void onInstructionListVisibilityChanged(boolean visible) {
    dispatcher.onInstructionListVisibilityChanged(visible);
  }
}
