package com.mapbox.navigation.ui;


import androidx.annotation.NonNull;

import com.mapbox.navigation.ui.map.OnWayNameChangedListener;

class NavigationViewWayNameListener implements OnWayNameChangedListener {

  private final NavigationPresenter presenter;

  NavigationViewWayNameListener(NavigationPresenter presenter) {
    this.presenter = presenter;
  }

  @Override
  public void onWayNameChanged(@NonNull String wayName) {
    presenter.onWayNameChanged(wayName);
  }
}
