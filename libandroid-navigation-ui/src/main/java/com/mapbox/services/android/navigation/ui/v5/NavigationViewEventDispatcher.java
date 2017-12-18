package com.mapbox.services.android.navigation.ui.v5;


import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

class NavigationViewEventDispatcher {

  private List<NavigationListener> navigationListeners;
  private List<RouteListener> routeListeners;

  NavigationViewEventDispatcher() {
    navigationListeners = new ArrayList<>();
    routeListeners = new ArrayList<>();
  }

  void addNavigationListener(@NonNull NavigationListener navigationListener) {
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
