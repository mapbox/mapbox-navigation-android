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
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.ManeuverUtils;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;
import com.mapbox.services.android.navigation.v5.utils.time.TimeUtils;

import java.text.DecimalFormat;
import java.util.Locale;

import timber.log.Timber;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;

/**
 * This is in charge of creating the persistent navigation session notification and updating it.
 */
class MapboxNavigationNotification implements NavigationNotification {

  private static final String NAVIGATION_NOTIFICATION_CHANNEL = "NAVIGATION_NOTIFICATION_CHANNEL";
  private static final String END_NAVIGATION_ACTION = "com.mapbox.intent.action.END_NAVIGATION";

  private NotificationCompat.Builder notificationBuilder;
  private NotificationManager notificationManager;
  private Notification notification;
  private RemoteViews notificationRemoteViews;
  private MapboxNavigation mapboxNavigation;

  private SpannableStringBuilder currentDistanceText;
  private DecimalFormat decimalFormat;
  private String currentArrivalTime;
  private String currentStepName;
  private int currentManeuverId;
  private int distanceUnitType;

  MapboxNavigationNotification(Context context, MapboxNavigation mapboxNavigation) {
    this.mapboxNavigation = mapboxNavigation;
    this.distanceUnitType = mapboxNavigation.options().unitType();
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

  private void buildNotification(Context context) {
    notificationRemoteViews = new RemoteViews(context.getPackageName(), R.layout.navigation_notification_layout);

    // Will trigger endNavigationBtnReceiver when clicked
    PendingIntent pendingCloseIntent = createPendingCloseIntent(context);
    notificationRemoteViews.setOnClickPendingIntent(R.id.endNavigationBtn, pendingCloseIntent);

    // Sets up the top bar notification
    notificationBuilder = new NotificationCompat.Builder(context, NAVIGATION_NOTIFICATION_CHANNEL)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setSmallIcon(R.drawable.ic_navigation)
      .setContent(notificationRemoteViews)
      .setOngoing(true);

    notification =  notificationBuilder.build();
  }

  /**
   * With each location update and new routeProgress, the notification is checked and updated if any
   * information has changed.
   *
   * @param routeProgress the latest RouteProgress object
   */
  private void updateNotificationViews(RouteProgress routeProgress) {
    // Street name
    if (newStepName(routeProgress) || currentStepName == null) {
      Timber.d("Updating step name");
      addStepName(routeProgress);
    }

    // Distance
    if (newDistanceText(routeProgress) || currentDistanceText == null) {
      Timber.d("Updating distance");
      addDistanceText(routeProgress);
    }

    // Arrival Time
    if (newArrivalTime(routeProgress) || currentArrivalTime == null) {
      Timber.d("Updating arrival time");
      addArrivalTime(routeProgress);
    }

    // Maneuver Image
    LegStep step = routeProgress.currentLegProgress().upComingStep() == null
      ? routeProgress.currentLegProgress().currentStep()
      : routeProgress.currentLegProgress().upComingStep();
    addManeuverImage(step);

    notificationManager.notify(NAVIGATION_NOTIFICATION_ID, notificationBuilder.build());
  }

  private boolean newStepName(RouteProgress routeProgress) {
    return currentStepName != null
      && !currentStepName.contentEquals(routeProgress.currentLegProgress().currentStep().name());
  }

  private void addStepName(RouteProgress routeProgress) {
    currentStepName = routeProgress.currentLegProgress().currentStep().name();
    String formattedStepName = StringAbbreviator.deliminator(
      StringAbbreviator.abbreviate(currentStepName));
    notificationRemoteViews.setTextViewText(
      R.id.notificationInstructionText,
      formattedStepName
    );
  }

  private boolean newDistanceText(RouteProgress routeProgress) {
    return currentDistanceText != null
      && !currentDistanceText.toString().contentEquals(DistanceUtils.distanceFormatter(
      routeProgress.currentLegProgress().currentStepProgress().distanceRemaining(),
      decimalFormat, true, distanceUnitType).toString());
  }

  private void addDistanceText(RouteProgress routeProgress) {
    currentDistanceText = DistanceUtils.distanceFormatter(
      routeProgress.currentLegProgress().currentStepProgress().distanceRemaining(),
      decimalFormat, true, distanceUnitType);
    if (!TextUtils.isEmpty(currentStepName)) {
      currentDistanceText.append(" - ");
    }
    notificationRemoteViews.setTextViewText(R.id.notificationDistanceText, currentDistanceText);
  }

  private boolean newArrivalTime(RouteProgress routeProgress) {
    return currentArrivalTime != null && currentArrivalTime.contentEquals(TimeUtils
      .formatArrivalTime(routeProgress.durationRemaining()));
  }

  private void addArrivalTime(RouteProgress routeProgress) {
    currentArrivalTime = TimeUtils.formatArrivalTime(routeProgress.durationRemaining());
    notificationRemoteViews.setTextViewText(R.id.notificationArrivalText,
      String.format(Locale.getDefault(), "Arrive at %s", currentArrivalTime));
  }

  private boolean newManeuverId(LegStep step) {
    return currentManeuverId != ManeuverUtils.getManeuverResource(step);
  }

  private void addManeuverImage(LegStep step) {
    if (newManeuverId(step)) {
      Timber.d("Updating maneuver");
      int maneuverResource = ManeuverUtils.getManeuverResource(step);
      currentManeuverId = maneuverResource;
      notificationRemoteViews.setImageViewResource(R.id.maneuverImage, maneuverResource);
    }
  }

  private void initialize(Context context) {
    notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    decimalFormat = new DecimalFormat(NavigationConstants.DECIMAL_FORMAT);
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

  private void registerReceiver(Context context) {
    if (context != null) {
      context.registerReceiver(endNavigationBtnReceiver, new IntentFilter(END_NAVIGATION_ACTION));
    }
  }

  private PendingIntent createPendingCloseIntent(Context context) {
    Intent endNavigationBtn = new Intent(END_NAVIGATION_ACTION);
    return PendingIntent.getBroadcast(context, 0, endNavigationBtn, 0);
  }

  private BroadcastReceiver endNavigationBtnReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(final Context context, final Intent intent) {
      MapboxNavigationNotification.this.onEndNavigationBtnClick();
    }
  };

  private void onEndNavigationBtnClick() {
    if (mapboxNavigation != null) {
      mapboxNavigation.endNavigation();
    }
  }
}