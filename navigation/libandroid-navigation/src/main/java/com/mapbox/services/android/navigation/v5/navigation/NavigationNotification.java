package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.StringUtils;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;

/**
 * This is in charge of creating the persistent navigation session notification and updating it.
 */
class NavigationNotification {

  private NotificationCompat.Builder notificationBuilder;
  private NotificationManager notificationManager;
  private RemoteViews remoteViews;
  private Context context;

  NavigationNotification(Context context) {
    this.context = context;
    initialize();
  }

  private void initialize() {
    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  /**
   *
   */
  Notification buildPersistentNotification(@LayoutRes int layout) {
    remoteViews = new RemoteViews(context.getPackageName(), layout);

    // Sets up the top bar notification
    notificationBuilder = new NotificationCompat.Builder(context)
      .setContent(remoteViews)
      .setSmallIcon(R.drawable.ic_navigation)
      .setContentIntent(PendingIntent.getActivity(context, 0,
        new Intent(context, NavigationService.class), 0));

    return notificationBuilder.build();
  }

  public void updateDefaultNotification(RouteProgress routeProgress) {
    remoteViews.setTextViewText(
      R.id.notificationStreetNameTextView,
      routeProgress.currentLegProgress().currentStep().getName()
    );
    remoteViews.setTextViewText(
      R.id.notificationStepDistanceTextView,
      StringUtils.distanceFormatter(routeProgress.currentLegProgress().currentStepProgress().distanceRemaining())
    );
//    remoteViews.setImageViewResource();
    notificationManager.notify( NAVIGATION_NOTIFICATION_ID, notificationBuilder.build());
  }
}