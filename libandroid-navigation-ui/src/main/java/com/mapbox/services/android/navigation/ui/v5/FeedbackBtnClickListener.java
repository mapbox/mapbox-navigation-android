package com.mapbox.services.android.navigation.ui.v5;

import android.view.View;

public class FeedbackBtnClickListener implements View.OnClickListener {
  private NavigationPresenter presenter;

  FeedbackBtnClickListener(NavigationPresenter presenter) {
    this.presenter = presenter;
  }

  @Override
  public void onClick(View view) {
    presenter.onFeedbackClick();
  }
}
