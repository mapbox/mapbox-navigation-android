package com.mapbox.services.android.navigation.v5.navigation.notification;

import android.app.Notification;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public interface NavigationNotification {

  Notification getNotification();

  int getNotificationId();

  void updateNotification(RouteProgress routeProgress);
}
