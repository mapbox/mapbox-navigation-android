package com.mapbox.services.android.navigation.testapp.activity.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;

import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL;

public class CustomNavigationNotification implements NavigationNotification {

  private static final int CUSTOM_NOTIFICATION_ID = 91234821;
  private static final String STOP_NAVIGATION_ACTION = "stop_navigation_action";

  private final Notification customNotification;
  private final NotificationCompat.Builder customNotificationBuilder;
  private final NotificationManager notificationManager;
  private final BroadcastReceiver stopNavigationReceiver;
  private int numberOfUpdates;

  public CustomNavigationNotification(Context applicationContext, BroadcastReceiver stopNavigationReceiver) {
    // Receiver for listening to clicks
    this.stopNavigationReceiver = stopNavigationReceiver;
    applicationContext.registerReceiver(stopNavigationReceiver, new IntentFilter(STOP_NAVIGATION_ACTION));

    notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);

    customNotificationBuilder = new NotificationCompat.Builder(applicationContext, NAVIGATION_NOTIFICATION_CHANNEL)
      .setSmallIcon(R.drawable.ic_navigation)
      .setContentTitle("Custom Navigation Notification")
      .setContentText("Display your own content here!")
      .setContentIntent(createPendingStopIntent(applicationContext));

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

    notificationManager.notify(CUSTOM_NOTIFICATION_ID, customNotificationBuilder.build());
  }

  @Override
  public void onNavigationStopped(Context context) {
    context.unregisterReceiver(stopNavigationReceiver);
  }

  private PendingIntent createPendingStopIntent(Context context) {
    Intent stopNavigationIntent = new Intent(STOP_NAVIGATION_ACTION);
    return PendingIntent.getBroadcast(context, 0, stopNavigationIntent, 0);
  }
}
