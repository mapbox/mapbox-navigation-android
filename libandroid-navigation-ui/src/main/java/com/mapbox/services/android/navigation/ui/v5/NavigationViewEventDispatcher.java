package com.mapbox.services.android.navigation.ui.v5;


import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class NavigationViewEventDispatcher {
  private List<NavigationViewListener> navigationViewListeners;
  private List<NavigationListener> navigationListeners;
  private List<RouteListener> routeListeners;

  NavigationViewEventDispatcher() {
    navigationViewListeners = new ArrayList<>();
    navigationListeners = new ArrayList<>();
    routeListeners = new ArrayList<>();
  }

  void addHNavigationViewListener(@NonNull NavigationViewListener navigationViewListener) {
    if (navigationViewListeners.contains(navigationViewListener)) {
      Timber.w("The specified NavigationViewListener has already been added to the stack.");
      return;
    }
    navigationViewListeners.add(navigationViewListener);
  }

  void addHNavigationVListener(@NonNull NavigationListener navigationListener) {
    if (navigationListeners.contains(navigationListener)) {
      Timber.w("The specified NavigationListener has already been added to the stack.");
      return;
    }
    navigationListeners.add(navigationListener);
  }

  void addRouteListener(@NonNull RouteListener routeListener) {
    if (routeListeners.contains(routeListener)) {
      Timber.w("The specified RouteListener has already been added to the stack.");
      return;
    }
    routeListeners.add(routeListener);
  }
}
