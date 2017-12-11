package com.mapbox.services.android.navigation.testapp.activity.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL;

public class CustomNavigationNotification implements NavigationNotification {

  private static final int CUSTOM_NOTIFICATION_ID = 91234821;

  private Notification customNotification;
  private NotificationCompat.Builder customNotificationBuilder;
  private NotificationManager notificationManager;
  private int numberOfUpdates;

  public CustomNavigationNotification(Context context) {
    // Get the notification manager to update your notification
    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    // Store the builder to update later
    customNotificationBuilder = new NotificationCompat.Builder(context, NAVIGATION_NOTIFICATION_CHANNEL)
      .setSmallIcon(R.drawable.ic_navigation)
      .setContentTitle("Custom Navigation Notification")
      .setContentText("Display your own content here!");

    // Build the notification
    customNotification = customNotificationBuilder.build();
  }

  @Override
  public Notification getNotification() {
    return customNotification;
  }

  @Override
  public int getNotificationId() {
    return CUSTOM_NOTIFICATION_ID;
  }

  @Override
  public void updateNotification(RouteProgress routeProgress) {
    // Update the builder with a new number of updates
    customNotificationBuilder.setContentText("Number of updates: " + numberOfUpdates++);

    // Notify the notification manager
    notificationManager.notify(CUSTOM_NOTIFICATION_ID, customNotificationBuilder.build());
  }
}
