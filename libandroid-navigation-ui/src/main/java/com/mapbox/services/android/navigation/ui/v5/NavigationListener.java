package com.mapbox.services.android.navigation.ui.v5;

public interface NavigationListener {
  void onCancelNavigation();

  void onNavigationFinished();

  void onNavigationRunning();
}
