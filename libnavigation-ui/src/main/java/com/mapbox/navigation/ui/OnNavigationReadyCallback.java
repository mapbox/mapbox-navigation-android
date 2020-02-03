package com.mapbox.navigation.ui;

public interface OnNavigationReadyCallback {

  /**
   * Fired after the navigation is ready.
   *
   * @param isRunning true after {@link NavigationView} has been initialized once and began navigation,
   *                  else false.
   *                  Indicates that {@link NavigationViewModel} is currently running and hasn't been destroyed from
   *                  a configuration change.
   */
  void onNavigationReady(boolean isRunning);
}
