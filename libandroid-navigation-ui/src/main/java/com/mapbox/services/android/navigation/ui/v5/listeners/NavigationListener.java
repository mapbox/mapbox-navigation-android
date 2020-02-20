package com.mapbox.services.android.navigation.ui.v5.listeners;

/**
 * A listener that can be implemented and
 * added to {@link com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions} to
 * hook into navigation related events occurring in {@link com.mapbox.services.android.navigation.ui.v5.NavigationView}.
 */
public interface NavigationListener {

  /**
   * Will be triggered when the user clicks
   * on the cancel "X" icon while navigating.
   *
   * @since 0.8.0
   */
  void onCancelNavigation();

  /**
   * Will be triggered when {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}
   * has finished and the service is completely shut down.
   *
   * @since 0.8.0
   */
  void onNavigationFinished();

  /**
   * Will be triggered when {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}
   * has been initialized and the user is navigating the given route.
   *
   * @since 0.8.0
   */
  void onNavigationRunning();
}
