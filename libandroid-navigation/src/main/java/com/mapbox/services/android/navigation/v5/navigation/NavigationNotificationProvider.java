package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

class NavigationNotificationProvider {

  private NavigationNotification navigationNotification;

  NavigationNotificationProvider(Context context, MapboxNavigation mapboxNavigation) {
    navigationNotification = buildNotificationFrom(context, mapboxNavigation);
  }

  NavigationNotification retrieveNotification() {
    return navigationNotification;
  }

  void updateNavigationNotification(RouteProgress routeProgress) {
    navigationNotification.updateNotification(routeProgress);
  }

  void unregisterNotificationReceiver(Context context) {
    if (navigationNotification instanceof MapboxNavigationNotification) {
      ((MapboxNavigationNotification) navigationNotification).unregisterReceiver(context);
    }
    navigationNotification = null;
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
