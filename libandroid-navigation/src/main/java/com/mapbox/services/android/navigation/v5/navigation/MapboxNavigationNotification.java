package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableString;
import android.widget.RemoteViews;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;
import com.mapbox.services.android.navigation.v5.utils.ManeuverUtils;
import com.mapbox.services.android.navigation.v5.utils.time.TimeUtils;

import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_CHANNEL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;

/**
 * This is in charge of creating the persistent navigation session notification and updating it.
 */
class MapboxNavigationNotification implements NavigationNotification {
  private static final String END_NAVIGATION_ACTION = "com.mapbox.intent.action.END_NAVIGATION";
  private final DistanceUtils distanceUtils;
  private NotificationCompat.Builder notificationBuilder;
  private NotificationManager notificationManager;
  private Notification notification;
  private RemoteViews notificationRemoteViews;
  private MapboxNavigation mapboxNavigation;
  private SpannableString currentDistanceText;
  private String currentArrivalTime;
  private String instructionText;
  private int currentManeuverId;

  private BroadcastReceiver endNavigationBtnReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(final Context context, final Intent intent) {
      MapboxNavigationNotification.this.onEndNavigationBtnClick();
    }
  };

  MapboxNavigationNotification(Context context, MapboxNavigation mapboxNavigation) {
    this.mapboxNavigation = mapboxNavigation;
    Locale locale = mapboxNavigation.options().locale();
    if (locale == null) {
      locale = LocaleUtils.getDeviceLocale(context);
    }
    this.distanceUtils = new DistanceUtils(
      context, locale, mapboxNavigation.options().unitType());
    initialize(context);
  }

  @Override
  public Notification getNotification() {
    return notification;
  }

  @Override
  public int getNotificationId() {
    return NAVIGATION_NOTIFICATION_ID;
  }

  @Override
  public void updateNotification(RouteProgress routeProgress) {
    updateNotificationViews(routeProgress);
  }

  void unregisterReceiver(Context context) {
    if (context != null) {
      context.unregisterReceiver(endNavigationBtnReceiver);
    }
    if (notificationManager != null) {
      notificationManager.cancel(NAVIGATION_NOTIFICATION_ID);
    }
  }

  private void initialize(Context context) {
    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    createNotificationChannel(context);
    buildNotification(context);
    registerReceiver(context);
  }

  /**
   * Notification channel setup for devices running Android Oreo or later.
   */
  private void createNotificationChannel(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel notificationChannel = new NotificationChannel(
        NAVIGATION_NOTIFICATION_CHANNEL, context.getString(R.string.channel_name),
        NotificationManager.IMPORTANCE_LOW);
      notificationManager.createNotificationChannel(notificationChannel);
    }
  }

  private void buildNotification(Context context) {
    notificationRemoteViews = new RemoteViews(context.getPackageName(), R.layout.navigation_notification_layout);

    // Will trigger endNavigationBtnReceiver when clicked
    PendingIntent pendingCloseIntent = createPendingCloseIntent(context);
    notificationRemoteViews.setOnClickPendingIntent(R.id.endNavigationBtn, pendingCloseIntent);

    // Sets up the top bar notification
    notificationBuilder = new NotificationCompat.Builder(context, NAVIGATION_NOTIFICATION_CHANNEL)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setSmallIcon(R.drawable.ic_navigation)
      .setCustomBigContentView(notificationRemoteViews)
      .setOngoing(true);

    notification = notificationBuilder.build();
  }

  private void registerReceiver(Context context) {
    if (context != null) {
      context.registerReceiver(endNavigationBtnReceiver, new IntentFilter(END_NAVIGATION_ACTION));
    }
  }

  /**
   * With each location update and new routeProgress, the notification is checked and updated if any
   * information has changed.
   *
   * @param routeProgress the latest RouteProgress object
   */
  private void updateNotificationViews(RouteProgress routeProgress) {
    // Instruction
    updateInstructionText(routeProgress.currentLegProgress().currentStep());
    // Distance
    updateDistanceText(routeProgress);
    // Arrival Time
    updateArrivalTime(routeProgress);
    // Get upcoming step for maneuver image - current step if null
    LegStep step = routeProgress.currentLegProgress().upComingStep() != null
      ? routeProgress.currentLegProgress().upComingStep()
      : routeProgress.currentLegProgress().currentStep();
    // Maneuver Image
    updateManeuverImage(step);

    notificationManager.notify(NAVIGATION_NOTIFICATION_ID, notificationBuilder.build());
  }

  private void updateInstructionText(LegStep step) {
    if (hasInstructions(step) && (instructionText == null || newInstructionText(step))) {
      instructionText = step.bannerInstructions().get(0).primary().text();
      notificationRemoteViews.setTextViewText(R.id.notificationInstructionText, instructionText);
    }
  }

  private boolean hasInstructions(LegStep step) {
    return step.bannerInstructions() != null && !step.bannerInstructions().isEmpty();
  }

  private boolean newInstructionText(LegStep step) {
    return !instructionText.equals(step.bannerInstructions().get(0).primary().text());
  }

  private void updateDistanceText(RouteProgress routeProgress) {
    if (currentDistanceText == null || newDistanceText(routeProgress)) {
      currentDistanceText = distanceUtils.formatDistance(
        routeProgress.currentLegProgress().currentStepProgress().distanceRemaining());
      notificationRemoteViews.setTextViewText(R.id.notificationDistanceText, currentDistanceText);
    }
  }

  private boolean newDistanceText(RouteProgress routeProgress) {
    return currentDistanceText != null
      && !currentDistanceText.toString().equals(distanceUtils.formatDistance(
      routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()).toString());
  }

  private void updateArrivalTime(RouteProgress routeProgress) {
    if (currentArrivalTime == null || newArrivalTime(routeProgress)) {
      currentArrivalTime = TimeUtils.formatArrivalTime(routeProgress.durationRemaining());
      notificationRemoteViews.setTextViewText(R.id.notificationArrivalText,
        String.format(Locale.getDefault(), "%s ETA", currentArrivalTime));
    }
  }

  private boolean newArrivalTime(RouteProgress routeProgress) {
    return currentArrivalTime != null && !currentArrivalTime.equals(TimeUtils
      .formatArrivalTime(routeProgress.durationRemaining()));
  }

  private void updateManeuverImage(LegStep step) {
    if (newManeuverId(step)) {
      int maneuverResource = ManeuverUtils.getManeuverResource(step);
      currentManeuverId = maneuverResource;
      notificationRemoteViews.setImageViewResource(R.id.maneuverImage, maneuverResource);
    }
  }

  private boolean newManeuverId(LegStep step) {
    return currentManeuverId != ManeuverUtils.getManeuverResource(step);
  }

  private PendingIntent createPendingCloseIntent(Context context) {
    Intent endNavigationBtn = new Intent(END_NAVIGATION_ACTION);
    return PendingIntent.getBroadcast(context, 0, endNavigationBtn, 0);
  }

  private void onEndNavigationBtnClick() {
    if (mapboxNavigation != null) {
      mapboxNavigation.endNavigation();
    }
  }
}