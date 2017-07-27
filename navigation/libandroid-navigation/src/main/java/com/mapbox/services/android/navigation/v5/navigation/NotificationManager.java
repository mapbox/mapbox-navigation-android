package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.NotificationCompat;

import com.mapbox.services.android.navigation.v5.notification.NavigationStyle;


public class NotificationManager {

  private Notification notification;
  private Context context;

  public NotificationManager(Context context) {
    this.context = context;
  }

  public Notification buildPersistentNotification() {
    // Sets up the top bar notification
    notification = new NotificationCompat.Builder(context)
      .setContentTitle("Mapbox Navigation")
      .setContentText("navigating")
      .setColor(Color.BLUE)
      .setStyle(new NavigationStyle())
      .setSmallIcon(com.mapbox.services.android.navigation.R.drawable.ic_navigation_black_24dp)
      .setContentIntent(PendingIntent.getActivity(context, 0,
        new Intent(context, NavigationService.class), 0)).build();

    return notification;
  }


}
