package com.mapbox.services.android.navigation.ui.v5;

import android.view.View;

class RouteOverviewBtnClickListener implements View.OnClickListener {

  private NavigationPresenter presenter;

  RouteOverviewBtnClickListener(NavigationPresenter presenter) {
    this.presenter = presenter;
  }

  @Override
  public void onClick(View view) {
    presenter.onRouteOverviewClick();
  }
}
