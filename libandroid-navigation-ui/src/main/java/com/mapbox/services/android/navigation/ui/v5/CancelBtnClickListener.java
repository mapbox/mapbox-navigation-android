package com.mapbox.services.android.navigation.ui.v5;

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
