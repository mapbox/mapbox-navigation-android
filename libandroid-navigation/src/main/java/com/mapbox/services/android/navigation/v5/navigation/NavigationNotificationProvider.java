package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

class NavigationNotificationProvider {

  private NavigationNotification navigationNotification;
  private boolean shouldUpdate = true;

  NavigationNotificationProvider(Context context, MapboxNavigation mapboxNavigation) {
    navigationNotification = buildNotificationFrom(context, mapboxNavigation);
  }

  NavigationNotification retrieveNotification() {
    return navigationNotification;
  }

  void updateNavigationNotification(RouteProgress routeProgress) {
    if (shouldUpdate) {
      navigationNotification.updateNotification(routeProgress);
    }
  }

  void shutdown(Context context) {
    navigationNotification.onNavigationStopped(context);
    navigationNotification = null;
    shouldUpdate = false;
  }

  private NavigationNotification buildNotificationFrom(Context context, MapboxNavigation mapboxNavigation) {
    MapboxNavigationOptions options = mapboxNavigation.options();
    if (options.navigationNotification() != null) {
      return options.navigationNotification();
    } else {
      return new MapboxNavigationNotification(context, mapboxNavigation);
    }
  }
}
