package com.mapbox.services.android.navigation.ui.v5;

import android.view.View;

class RecenterBtnClickListener implements View.OnClickListener {

  private NavigationPresenter presenter;

  RecenterBtnClickListener(NavigationPresenter presenter) {
    this.presenter = presenter;
  }

  @Override
  public void onClick(View view) {
    presenter.onRecenterClick();
  }
}
