package com.mapbox.navigation.examples.ui.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.mapbox.navigation.examples.R;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public class CustomNavigationNotification implements NavigationNotification {

  private static final int CUSTOM_NOTIFICATION_ID = 91234821;
  private static final String STOP_NAVIGATION_ACTION = "stop_navigation_action";
  private static final String CUSTOM_CHANNEL_ID = "custom_channel_id";
  private static final String CUSTOM_CHANNEL_NAME = "custom_channel_name";

  private final Notification customNotification;
  private final NotificationCompat.Builder customNotificationBuilder;
  private final NotificationManager notificationManager;
  private BroadcastReceiver stopNavigationReceiver;
  private int numberOfUpdates;

  public CustomNavigationNotification(Context applicationContext) {
    notificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel notificationChannel = new NotificationChannel(
        CUSTOM_CHANNEL_ID, CUSTOM_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
      );
      notificationManager.createNotificationChannel(notificationChannel);
    }

    customNotificationBuilder = new NotificationCompat.Builder(applicationContext, CUSTOM_CHANNEL_ID)
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
    notificationManager.cancel(CUSTOM_NOTIFICATION_ID);
  }

  public void register(BroadcastReceiver stopNavigationReceiver, Context applicationContext) {
    this.stopNavigationReceiver = stopNavigationReceiver;
    applicationContext.registerReceiver(stopNavigationReceiver, new IntentFilter(STOP_NAVIGATION_ACTION));
  }

  private PendingIntent createPendingStopIntent(Context context) {
    Intent stopNavigationIntent = new Intent(STOP_NAVIGATION_ACTION);
    return PendingIntent.getBroadcast(context, 0, stopNavigationIntent, 0);
  }
}
