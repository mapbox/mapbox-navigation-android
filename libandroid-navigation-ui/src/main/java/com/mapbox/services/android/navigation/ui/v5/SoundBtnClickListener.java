package com.mapbox.services.android.navigation.ui.v5;

import android.view.View;

class SoundBtnClickListener implements View.OnClickListener {
  private final NavigationPresenter presenter;

  SoundBtnClickListener(NavigationPresenter presenter) {
    this.presenter = presenter;
  }

  @Override
  public void onClick(View view) {
    presenter.onSoundClick();
  }
}
