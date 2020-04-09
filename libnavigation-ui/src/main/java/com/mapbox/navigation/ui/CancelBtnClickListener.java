package com.mapbox.navigation.ui;

import android.view.View;

class CancelBtnClickListener implements View.OnClickListener {

  private NavigationViewEventDispatcher dispatcher;

  CancelBtnClickListener(NavigationViewEventDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  public void onClick(View view) {
    dispatcher.onCancelNavigation();
  }
}
